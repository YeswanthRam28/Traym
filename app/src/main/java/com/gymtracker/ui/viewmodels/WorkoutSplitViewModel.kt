package com.gymtracker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.network.ApiClient
import com.gymtracker.network.WorkoutSummaryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ActualSet(
    val weightInput: String = "",
    val repsInput: String = "",
    val isCompleted: Boolean = false,
    val setType: String = "Normal",
    val id: String = java.util.UUID.randomUUID().toString()
) {
    val weight: Double get() = weightInput.toDoubleOrNull() ?: 0.0
    val reps: Int get() = repsInput.toIntOrNull() ?: 0
}

data class PlannedExercise(
    val name: String,
    val sets: Int,
    val reps: Int,
    val pr: Double = 0.0,
    val actualSets: List<ActualSet> = emptyList(),
    val id: String = java.util.UUID.randomUUID().toString()
)

data class WorkoutDay(
    val dayName: String,
    val splitTitle: String,
    val exercises: List<PlannedExercise> = emptyList(),
    val lastLoggedDate: String? = null,
    val id: String = java.util.UUID.randomUUID().toString()
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

    fun updateDayName(dayIndex: Int, newName: String) {
        val currentDays = _uiState.value.days.toMutableList()
        if (dayIndex in currentDays.indices) {
            currentDays[dayIndex] = currentDays[dayIndex].copy(dayName = newName)
            _uiState.value = _uiState.value.copy(days = currentDays)
        }
    }

    fun addDay() {
        val currentDays = _uiState.value.days.toMutableList()
        currentDays.add(WorkoutDay("NEW DAY", "NEW SPLIT", emptyList()))
        _uiState.value = _uiState.value.copy(days = currentDays)
    }

    fun removeDay(dayIndex: Int) {
        val currentDays = _uiState.value.days.toMutableList()
        if (dayIndex in currentDays.indices) {
            currentDays.removeAt(dayIndex)
            _uiState.value = _uiState.value.copy(days = currentDays)
        }
    }

    fun moveDay(fromIndex: Int, toIndex: Int) {
        val currentDays = _uiState.value.days.toMutableList()
        if (fromIndex in currentDays.indices && toIndex in currentDays.indices) {
            val day = currentDays.removeAt(fromIndex)
            currentDays.add(toIndex, day)
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

    fun cycleSetType(dayIndex: Int, exerciseIndex: Int, setIndex: Int) {
        val currentDays = _uiState.value.days.toMutableList()
        if (dayIndex in currentDays.indices) {
            val currentExercises = currentDays[dayIndex].exercises.toMutableList()
            if (exerciseIndex in currentExercises.indices) {
                val currentSets = currentExercises[exerciseIndex].actualSets.toMutableList()
                if (setIndex in currentSets.indices) {
                    val currentType = currentSets[setIndex].setType
                    val newType = when (currentType) {
                        "Normal" -> "Drop Set"
                        "Drop Set" -> "Super Set"
                        "Super Set" -> "Warm-up"
                        else -> "Normal"
                    }
                    currentSets[setIndex] = currentSets[setIndex].copy(setType = newType)
                    currentExercises[exerciseIndex] = currentExercises[exerciseIndex].copy(actualSets = currentSets)
                    currentDays[dayIndex] = currentDays[dayIndex].copy(exercises = currentExercises)
                    _uiState.value = _uiState.value.copy(days = currentDays)
                }
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

    fun addExerciseFromApi(dayIndex: Int, exerciseName: String) {
        val currentDays = _uiState.value.days.toMutableList()
        if (dayIndex in currentDays.indices) {
            val currentExercises = currentDays[dayIndex].exercises.toMutableList()
            val defaultSets = mutableListOf<ActualSet>()
            for (i in 0 until 3) defaultSets.add(ActualSet())
            
            currentExercises.add(PlannedExercise(exerciseName, 3, 10, 0.0, defaultSets))
            currentDays[dayIndex] = currentDays[dayIndex].copy(exercises = currentExercises)
            _uiState.value = _uiState.value.copy(days = currentDays)
            savePlan()
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

        savePlan()
    }

    fun updateActualSet(dayIndex: Int, exerciseIndex: Int, setIndex: Int, weightInput: String, repsInput: String) {
        val currentDays = _uiState.value.days.toMutableList()
        val day = currentDays[dayIndex]
        val exercises = day.exercises.toMutableList()
        val ex = exercises[exerciseIndex]
        
        val actualSets = ex.actualSets.toMutableList()
        val currentSet = actualSets[setIndex]
        actualSets[setIndex] = currentSet.copy(weightInput = weightInput, repsInput = repsInput)
        
        exercises[exerciseIndex] = ex.copy(actualSets = actualSets)
        
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        currentDays[dayIndex] = day.copy(exercises = exercises, lastLoggedDate = todayStr)
        _uiState.value = _uiState.value.copy(days = currentDays)
        
        savePlan()
    }

    fun saveExercise(dayIndex: Int, exerciseIndex: Int) {
        viewModelScope.launch {
            try {
                val day = _uiState.value.days[dayIndex]
                val exercise = day.exercises[exerciseIndex]
                val completed = exercise.actualSets.filter { it.isCompleted }
                if (completed.isEmpty()) return@launch

                val setLogs = completed.mapIndexed { i, s ->
                    com.gymtracker.network.SetLog(
                        exercise_name = exercise.name,
                        set_number = i + 1,
                        weight_kg = s.weight.toFloat(),
                        reps = s.reps,
                        rpe = 8.0f,
                        rest_seconds = 90,
                        set_type = s.setType
                    )
                }
                
                ApiClient.apiService.logInlineWorkout(
                    com.gymtracker.network.InlineLogRequest(
                        dayTitle = day.splitTitle,
                        exerciseName = exercise.name,
                        sets = setLogs
                    )
                )
                
                loadHistory()
                
                // Immediately sync with Notion to capture the batch sets flawlessly
                withContext(Dispatchers.IO) {
                    com.gymtracker.network.NotionSyncManager.syncWithNotion({}, {})
                    
                    // Also silently push the latest volume to the Leaderboard
                    try {
                        com.gymtracker.network.CommunityRepository().fetchLeaderboard()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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
                            aObj.put("setType", a.setType)
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

                val activeWorkoutJson = buildActiveWorkoutJson()
                com.gymtracker.network.CommunityRepository().syncLiveWorkout(activeWorkoutJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun buildActiveWorkoutJson(): String? {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val todayDay = _uiState.value.days.find { it.lastLoggedDate == todayStr } ?: return null

        val exercisesArr = org.json.JSONArray()
        todayDay.exercises.forEach { ex ->
            val completedSets = ex.actualSets.filter { it.isCompleted }
            if (completedSets.isNotEmpty()) {
                val exObj = org.json.JSONObject()
                exObj.put("title", ex.name)
                
                val setsArr = org.json.JSONArray()
                completedSets.forEach { s ->
                    val sObj = org.json.JSONObject()
                    sObj.put("weight_kg", s.weight)
                    sObj.put("reps", s.reps)
                    setsArr.put(sObj)
                }
                exObj.put("sets", setsArr)
                exercisesArr.put(exObj)
            }
        }

        return if (exercisesArr.length() > 0) exercisesArr.toString() else null
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
                            
                            if (actArr != null && actArr.length() > 0) {
                                for (k in 0 until actArr.length()) {
                                    val aObj = actArr.getJSONObject(k)
                                    actualSetsList.add(ActualSet(
                                        weightInput = aObj.optDouble("weight", 0.0).let { if (it > 0) it.toString().removeSuffix(".0") else "" },
                                        repsInput = aObj.optInt("reps", 0).let { if (it > 0) it.toString() else "" },
                                        isCompleted = if (isToday) aObj.optBoolean("isCompleted", false) else false,
                                        setType = aObj.optString("setType", "Normal")
                                    ))
                                }
                            } else {
                                // Default initialize based on sets
                                for (k in 0 until sets) {
                                    actualSetsList.add(ActualSet())
                                }
                            }
                            
                            exercisesList.add(
                                PlannedExercise(
                                    name = cleanLegacyName(exObj.optString("name")),
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

    private fun cleanLegacyName(oldName: String): String {
        val n = oldName.trim()
        if (n.contains("Overhand / Neutral Pull-up", ignoreCase = true)) return "Pull-up"
        if (n.contains("Neutral / Overhand Lat Pulldown", ignoreCase = true)) return "Lat Pulldown"
        if (n.contains("Seated Cable Row /", ignoreCase = true)) return "Seated Cable Row"
        if (n.contains("Incline DB Row /", ignoreCase = true)) return "Incline DB Row"
        if (n.contains("Butterfly", ignoreCase = true)) return "Butterfly (Pec-deck)"
        if (n.contains("Lateral Raise /", ignoreCase = true)) return "Lateral Raise"
        if (n.contains("Push-up Plus /", ignoreCase = true)) return "Push-up Plus"
        if (n.contains("Leg Press /", ignoreCase = true)) return "Leg Press"
        if (n.contains("Lying Leg Curl /", ignoreCase = true)) return "Lying Leg Curl"
        if (n.contains("Standing Calf Raise /", ignoreCase = true)) return "Standing Calf Raise"
        if (n.contains("Hip Abduction", ignoreCase = true)) return "Hip Abduction"
        if (n.contains("Adductor", ignoreCase = true)) return "Adductor"
        if (n.contains("Tibialis Raise", ignoreCase = true)) return "Tibialis Raise"
        if (n.contains("Straight-arm Pulldown /", ignoreCase = true)) return "Straight-arm Pulldown"
        if (n.contains("Hammer Curl /", ignoreCase = true)) return "Hammer Curl"
        if (n.contains("Reverse Curl /", ignoreCase = true)) return "Reverse Curl"
        if (n.contains("Incline Reverse Fly /", ignoreCase = true)) return "Incline Reverse Fly"
        if (n.contains("Incline Dumbbell Curl /", ignoreCase = true)) return "Incline Dumbbell Curl"
        if (n.contains("Machine Shoulder Press /", ignoreCase = true)) return "Machine Shoulder Press"
        if (n.contains("Upright Row /", ignoreCase = true)) return "Upright Row"
        
        return n.substringBefore(" /").trim()
    }

    private fun getDefaultSplit(): List<WorkoutDay> {
        return listOf(
            WorkoutDay("MON", "PULL A (BACK & REAR DELTS)", listOf(
                PlannedExercise("Pull-up", 3, 12),
                PlannedExercise("Seated Cable Row (Neutral Grip)", 3, 15),
                PlannedExercise("Lat Pulldown (Neutral Grip)", 3, 15),
                PlannedExercise("Incline DB Row (Neutral / Semi-pronated)", 3, 12),
                PlannedExercise("Face Pull", 3, 15),
                PlannedExercise("Machine Reverse Delt Fly", 3, 15),
                PlannedExercise("Dumbbell Shrug", 3, 15)
            )),
            WorkoutDay("TUE", "PUSH A (CHEST & SHOULDERS)", listOf(
                PlannedExercise("Push-up", 3, 15),
                PlannedExercise("Bench Press", 3, 15),
                PlannedExercise("Incline Dumbbell Press", 3, 12),
                PlannedExercise("Butterfly (Pec-deck)", 3, 15),
                PlannedExercise("Dumbbell Shoulder Press", 3, 15),
                PlannedExercise("Lateral Raise", 3, 15),
                PlannedExercise("Push-up Plus", 3, 20)
            )),
            WorkoutDay("WED", "LEGS", listOf(
                PlannedExercise("Weighted Squat", 3, 12),
                PlannedExercise("Leg Press", 3, 15),
                PlannedExercise("Lying Leg Curl", 3, 15),
                PlannedExercise("Hip Thrust (Barbell)", 3, 15),
                PlannedExercise("Leg Extension", 3, 15),
                PlannedExercise("Bulgarian Split Squat", 3, 12),
                PlannedExercise("Standing Calf Raise", 3, 20),
                PlannedExercise("Hip Abduction", 3, 20),
                PlannedExercise("Adductor", 3, 20),
                PlannedExercise("Tibialis Raise", 3, 20)
            )),
            WorkoutDay("THU", "PULL B (BACK & BICEPS)", listOf(
                PlannedExercise("Chin-up", 3, 12),
                PlannedExercise("Dumbbell Row", 3, 15),
                PlannedExercise("Straight-arm Pulldown", 3, 15),
                PlannedExercise("Hammer Curl", 3, 15),
                PlannedExercise("Reverse Curl", 3, 15),
                PlannedExercise("Incline Reverse Fly", 3, 15),
                PlannedExercise("Incline Dumbbell Curl", 3, 15)
            )),
            WorkoutDay("FRI", "PUSH B (CHEST & SHOULDERS)", listOf(
                PlannedExercise("Dips", 3, 15),
                PlannedExercise("Close-Grip Bench Press", 3, 15),
                PlannedExercise("Dumbbell Fly", 3, 15),
                PlannedExercise("Machine Shoulder Press", 3, 15),
                PlannedExercise("Upright Row", 3, 15),
                PlannedExercise("Triceps Extension", 3, 15),
                PlannedExercise("Serratus Punch", 3, 15),
                PlannedExercise("Neck Flexion", 3, 20),
                PlannedExercise("Neck Extension", 3, 20)
            )),
            WorkoutDay("SAT", "CARDIO + CORE", listOf(
                PlannedExercise("Run", 1, 10),
                PlannedExercise("Walk", 1, 3),
                PlannedExercise("Run", 1, 15),
                PlannedExercise("Incline Walk", 1, 10),
                PlannedExercise("Side Plank", 3, 1),
                PlannedExercise("Leg Raise", 3, 20),
                PlannedExercise("Hollow Body Hold", 3, 40),
                PlannedExercise("Pallof Press", 3, 15)
            )),
            WorkoutDay("SUN", "REST", emptyList())
        )
    }
}
