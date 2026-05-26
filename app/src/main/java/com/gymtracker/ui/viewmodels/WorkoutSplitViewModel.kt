package com.gymtracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.network.ApiClient
import com.gymtracker.network.WorkoutSummaryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ActualSet(
    val weight: Double = 0.0,
    val reps: Int = 0,
    val isCompleted: Boolean = false
)

data class PlannedExercise(
    val name: String,
    val sets: Int,
    val reps: Int,
    val pr: Double = 0.0,
    val actualSets: List<ActualSet> = emptyList()
)

data class WorkoutDay(
    val dayName: String,
    val splitTitle: String,
    val exercises: List<PlannedExercise> = emptyList(),
    val lastLoggedDate: String? = null
)

data class WorkoutSplitUiState(
    val days: List<WorkoutDay> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class WorkoutSplitViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutSplitUiState())
    val uiState: StateFlow<WorkoutSplitUiState> = _uiState.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _history = MutableStateFlow<List<WorkoutSummaryResponse>>(emptyList())
    val history: StateFlow<List<WorkoutSummaryResponse>> = _history.asStateFlow()

    init {
        loadSplit()
        loadHistory()
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
        if (!_isEditMode.value) {
            savePlan()
        }
    }

    fun updateDayTitle(dayIndex: Int, newTitle: String) {
        val currentDays = _uiState.value.days.toMutableList()
        if (dayIndex in currentDays.indices) {
            currentDays[dayIndex] = currentDays[dayIndex].copy(splitTitle = newTitle)
            _uiState.value = _uiState.value.copy(days = currentDays)
        }
    }

    fun updateExercise(dayIndex: Int, exerciseIndex: Int, updatedExercise: PlannedExercise) {
        val currentDays = _uiState.value.days.toMutableList()
        if (dayIndex in currentDays.indices) {
            val currentExercises = currentDays[dayIndex].exercises.toMutableList()
            if (exerciseIndex in currentExercises.indices) {
                currentExercises[exerciseIndex] = updatedExercise
                currentDays[dayIndex] = currentDays[dayIndex].copy(exercises = currentExercises)
                _uiState.value = _uiState.value.copy(days = currentDays)
            }
        }
    }

    fun removeExercise(dayIndex: Int, exerciseIndex: Int) {
        val currentDays = _uiState.value.days.toMutableList()
        if (dayIndex in currentDays.indices) {
            val currentExercises = currentDays[dayIndex].exercises.toMutableList()
            if (exerciseIndex in currentExercises.indices) {
                currentExercises.removeAt(exerciseIndex)
                currentDays[dayIndex] = currentDays[dayIndex].copy(exercises = currentExercises)
                _uiState.value = _uiState.value.copy(days = currentDays)
            }
        }
    }
    fun moveExercise(dayIndex: Int, fromIndex: Int, toIndex: Int) {
        val currentDays = _uiState.value.days.toMutableList()
        if (dayIndex in currentDays.indices) {
            val currentExercises = currentDays[dayIndex].exercises.toMutableList()
            if (fromIndex in currentExercises.indices && toIndex in currentExercises.indices) {
                val exercise = currentExercises.removeAt(fromIndex)
                currentExercises.add(toIndex, exercise)
                currentDays[dayIndex] = currentDays[dayIndex].copy(exercises = currentExercises)
                _uiState.value = _uiState.value.copy(days = currentDays)
            }
        }
    }

    fun addExercise(dayIndex: Int) {
        val currentDays = _uiState.value.days.toMutableList()
        if (dayIndex in currentDays.indices) {
            val currentExercises = currentDays[dayIndex].exercises.toMutableList()
            val defaultSets = mutableListOf<ActualSet>()
            for (i in 0 until 3) defaultSets.add(ActualSet())
            
            currentExercises.add(PlannedExercise("New Exercise", 3, 10, 0.0, defaultSets))
            currentDays[dayIndex] = currentDays[dayIndex].copy(exercises = currentExercises)
            _uiState.value = _uiState.value.copy(days = currentDays)
        }
    }
    
    fun toggleSetCompleted(dayIndex: Int, exerciseIndex: Int, setIndex: Int) {
        val currentDays = _uiState.value.days.toMutableList()
        val day = currentDays[dayIndex]
        val exercises = day.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        
        val actualSets = ex.actualSets.toMutableList()
        val currentSet = actualSets[setIndex]
        actualSets[setIndex] = currentSet.copy(isCompleted = !currentSet.isCompleted)
        
        exercises[exerciseIndex] = ex.copy(actualSets = actualSets)
        
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        currentDays[dayIndex] = day.copy(exercises = exercises, lastLoggedDate = todayStr)
        _uiState.value = _uiState.value.copy(days = currentDays)

        saveInlineLog(day.splitTitle, exercises[exerciseIndex])
        savePlan()
    }

    fun updateActualSet(dayIndex: Int, exerciseIndex: Int, setIndex: Int, weight: Double, reps: Int) {
        val currentDays = _uiState.value.days.toMutableList()
        val day = currentDays[dayIndex]
        val exercises = day.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        
        val actualSets = ex.actualSets.toMutableList()
        val currentSet = actualSets[setIndex]
        actualSets[setIndex] = currentSet.copy(weight = weight, reps = reps)
        
        exercises[exerciseIndex] = ex.copy(actualSets = actualSets)
        
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        currentDays[dayIndex] = day.copy(exercises = exercises, lastLoggedDate = todayStr)
        _uiState.value = _uiState.value.copy(days = currentDays)
        
        if (currentSet.isCompleted) {
            saveInlineLog(day.splitTitle, exercises[exerciseIndex])
        }
        savePlan()
    }

    private fun saveInlineLog(dayTitle: String, exercise: PlannedExercise) {
        viewModelScope.launch {
            try {
                val completed = exercise.actualSets.filter { it.isCompleted }
                val setLogs = completed.mapIndexed { i, s ->
                    com.gymtracker.network.SetLog(
                        exercise_name = exercise.name,
                        set_number = i + 1,
                        weight_kg = s.weight.toFloat(),
                        reps = s.reps,
                        rpe = 8.0f,
                        rest_seconds = 90
                    )
                }
                
                ApiClient.apiService.logInlineWorkout(
                    com.gymtracker.network.InlineLogRequest(
                        dayTitle = dayTitle,
                        exerciseName = exercise.name,
                        sets = setLogs
                    )
                )
                
                loadHistory()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun savePlan() {
        viewModelScope.launch {
            try {
                val planObj = org.json.JSONObject()
                planObj.put("split", "Custom Split")
                planObj.put("week_number", 1)
                
                val daysArr = org.json.JSONArray()
                _uiState.value.days.forEach { day ->
                    val dayObj = org.json.JSONObject()
                    dayObj.put("day", day.dayName)
                    dayObj.put("title", day.splitTitle)
                    dayObj.put("lastLoggedDate", day.lastLoggedDate ?: "")
                    
                    val exArr = org.json.JSONArray()
                    day.exercises.forEach { ex ->
                        val exObj = org.json.JSONObject()
                        exObj.put("name", ex.name)
                        exObj.put("sets", ex.sets)
                        exObj.put("reps", ex.reps)
                        exObj.put("pr", ex.pr)
                        
                        val actArr = org.json.JSONArray()
                        ex.actualSets.forEach { a ->
                            val aObj = org.json.JSONObject()
                            aObj.put("weight", a.weight)
                            aObj.put("reps", a.reps)
                            aObj.put("isCompleted", a.isCompleted)
                            actArr.put(aObj)
                        }
                        exObj.put("actualSets", actArr)
                        exArr.put(exObj)
                    }
                    dayObj.put("exercises", exArr)
                    daysArr.put(dayObj)
                }
                planObj.put("days", daysArr)
                
                ApiClient.apiService.updateActivePlan(com.gymtracker.network.UpdatePlanRequest(planObj.toString()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getWorkoutHistory()
                if (response.isSuccessful) {
                    _history.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadSplit() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = ApiClient.apiService.getActivePlan()
                if (response.isSuccessful) {
                    val plan = response.body()
                    if (plan != null) {
                        val days = parsePlanJson(plan.plan_json)
                        _uiState.value = _uiState.value.copy(days = days, isLoading = false)
                    } else {
                        _uiState.value = _uiState.value.copy(days = getDefaultSplit(), isLoading = false)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(days = getDefaultSplit(), isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    days = getDefaultSplit(),
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun parsePlanJson(jsonStr: String): List<WorkoutDay> {
        val list = mutableListOf<WorkoutDay>()
        try {
            val planObj = org.json.JSONObject(jsonStr)
            val daysArr = planObj.optJSONArray("days")
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            
            if (daysArr != null) {
                for (i in 0 until daysArr.length()) {
                    val dObj = daysArr.getJSONObject(i)
                    val dayName = dObj.optString("day")
                    val splitTitle = dObj.optString("title")
                    val lastLoggedDate = dObj.optString("lastLoggedDate", "")
                    
                    val isToday = lastLoggedDate == todayStr
                    
                    val exercisesArr = dObj.optJSONArray("exercises")
                    val exercisesList = mutableListOf<PlannedExercise>()
                    if (exercisesArr != null) {
                        for (j in 0 until exercisesArr.length()) {
                            val exObj = exercisesArr.getJSONObject(j)
                            
                            val sets = exObj.optInt("sets")
                            val actualSetsList = mutableListOf<ActualSet>()
                            val actArr = exObj.optJSONArray("actualSets")
                            
                            if (isToday && actArr != null && actArr.length() > 0) {
                                for (k in 0 until actArr.length()) {
                                    val aObj = actArr.getJSONObject(k)
                                    actualSetsList.add(ActualSet(
                                        weight = aObj.optDouble("weight", 0.0),
                                        reps = aObj.optInt("reps", 0),
                                        isCompleted = aObj.optBoolean("isCompleted", false)
                                    ))
                                }
                            } else {
                                // Default initialize based on sets
                                for (k in 0 until sets) {
                                    actualSetsList.add(ActualSet(weight = 0.0, reps = 0, isCompleted = false))
                                }
                            }
                            
                            exercisesList.add(
                                PlannedExercise(
                                    name = exObj.optString("name"),
                                    sets = sets,
                                    reps = exObj.optInt("reps"),
                                    pr = exObj.optDouble("pr", 0.0),
                                    actualSets = actualSetsList
                                )
                            )
                        }
                    }
                    list.add(WorkoutDay(dayName, splitTitle, exercisesList, if (isToday) lastLoggedDate else null))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return getDefaultSplit()
        }
        return if (list.isEmpty()) getDefaultSplit() else list
    }

    private fun getDefaultSplit(): List<WorkoutDay> {
        return listOf(
            WorkoutDay("MON", "PULL A", listOf(
                PlannedExercise("Overhand / Neutral Pull-up", 3, 8),
                PlannedExercise("Seated Cable Row", 3, 10),
                PlannedExercise("Neutral / Overhand Lat Pulldown", 3, 10),
                PlannedExercise("Incline DB Row", 3, 10),
                PlannedExercise("Face Pull", 3, 12),
                PlannedExercise("Machine Reverse Delt Fly", 3, 12),
                PlannedExercise("Dumbbell Shrug", 3, 12),
                PlannedExercise("Side Plank", 3, 1)
            )),
            WorkoutDay("TUE", "PUSH A", listOf(
                PlannedExercise("Push-up", 3, 12),
                PlannedExercise("Flat Barbell / DB Bench Press", 3, 10),
                PlannedExercise("Incline DB / Barbell Press", 3, 10),
                PlannedExercise("Pec Deck / Incline DB Fly / Flat DB Fly", 3, 12),
                PlannedExercise("DB Shoulder Press", 3, 10),
                PlannedExercise("Seated DB Lateral Raise / Single-arm Cable Lateral Raise", 3, 12),
                PlannedExercise("Push-up Plus", 3, 15),
                PlannedExercise("Leg Raise", 3, 15)
            )),
            WorkoutDay("WED", "LEGS", listOf(
                PlannedExercise("Squat Variation", 3, 10),
                PlannedExercise("Leg Press", 3, 12),
                PlannedExercise("Lying Leg Curl", 3, 12),
                PlannedExercise("Romanian Deadlift", 3, 10),
                PlannedExercise("Leg Extension", 3, 12),
                PlannedExercise("Weighted Hyperextension", 3, 12),
                PlannedExercise("Standing / Seated Calf Raise", 3, 15),
                PlannedExercise("Hip Abduction Machine / Cable Hip Abduction", 3, 15),
                PlannedExercise("Tibialis Raise", 3, 15)
            )),
            WorkoutDay("THU", "PULL B", listOf(
                PlannedExercise("Neutral / Underhand Chin-up", 3, 8),
                PlannedExercise("Single-arm DB Row", 3, 10),
                PlannedExercise("DB Pullover / Machine Pullover", 3, 12),
                PlannedExercise("Dumbbell Shrug", 3, 12),
                PlannedExercise("Reverse Curl", 3, 12),
                PlannedExercise("Incline DB Rear Delt Fly", 3, 12),
                PlannedExercise("Hollow Body Hold", 3, 30)
            )),
            WorkoutDay("FRI", "PUSH B", listOf(
                PlannedExercise("DB Floor Press", 3, 12),
                PlannedExercise("Close-Grip / Neutral-Grip Bench Press", 3, 10),
                PlannedExercise("Cable Chest Fly", 3, 12),
                PlannedExercise("Standing Overhead DB / Barbell Press", 3, 10),
                PlannedExercise("Machine Shoulder Press", 3, 12),
                PlannedExercise("Upright Row", 3, 12),
                PlannedExercise("Cable Woodchop / Pallof Press", 3, 12),
                PlannedExercise("Neck Flexion", 3, 15),
                PlannedExercise("Neck Extension", 3, 15)
            )),
            WorkoutDay("SAT", "CARDIO + STAMINA", listOf(
                PlannedExercise("Walk (Warm-up)", 1, 5),
                PlannedExercise("Run (Main)", 1, 10),
                PlannedExercise("Walk/Rest", 1, 3),
                PlannedExercise("Run (Main)", 1, 15),
                PlannedExercise("Incline Walk", 1, 10),
                PlannedExercise("Walk (Cool-down)", 1, 5),
                PlannedExercise("Stretch (Cool-down)", 1, 5)
            )),
            WorkoutDay("SUN", "REST", emptyList())
        )
    }
}
