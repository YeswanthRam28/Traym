package com.gymtracker.data

import com.gymtracker.auth.SessionManager
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

data class ExerciseLog(
    val title: String,
    val startedAt: String,
    val volume: Double,
    val durationSeconds: Int,
    val sets: Int,
    val reps: Int,
    val muscle: String,
    val equipment: String
)

data class ProgressStats(
    val workoutDates: List<String>,
    val currentStreakWeeks: Int,
    val restDays: Int,
    val topMusclesBySets: List<Pair<String, Int>>,
    val topExercisesBySets: List<Pair<String, Int>>,
    val monthlyVolumeKg: Double,
    val topMuscleWeek: String // The muscle group with the most sets in the last 7 days
)

object LocalStatsRepository {
    
    private fun getFile(fileName: String): File {
        return File(SessionManager.appContext.filesDir, fileName)
    }

    private fun readJsonArray(fileName: String): JSONArray {
        val file = getFile(fileName)
        if (!file.exists()) return JSONArray()
        return try {
            JSONArray(file.readText())
        } catch (e: Exception) {
            JSONArray()
        }
    }

    fun getLogs(): List<ExerciseLog> {
        val array = readJsonArray("workouts.json")
        val logs = mutableListOf<ExerciseLog>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            logs.add(
                ExerciseLog(
                    title = obj.optString("title", "Unknown"),
                    startedAt = obj.optString("started_at", ""),
                    volume = obj.optDouble("total_volume_kg", 0.0),
                    durationSeconds = obj.optInt("duration_seconds", 0),
                    sets = obj.optInt("total_sets", 0),
                    reps = obj.optInt("total_reps", 0),
                    muscle = obj.optString("muscle", "Full Body"),
                    equipment = obj.optString("equipment", "Bodyweight")
                )
            )
        }
        return logs
    }

    fun getProgressStats(): ProgressStats {
        val logs = getLogs()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        // 1. Workout Dates (Unique days)
        val workoutDates = logs.mapNotNull { 
            if (it.startedAt.length >= 10) it.startedAt.take(10) else null 
        }.distinct().sortedDescending()

        // 2. Compute Rest Days
        val today = Calendar.getInstance().apply { 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        var restDays = 0
        if (workoutDates.isNotEmpty()) {
            try {
                val lastWorkoutDate = format.parse(workoutDates.first())
                if (lastWorkoutDate != null) {
                    val diff = today.time - lastWorkoutDate.time
                    restDays = (diff / (1000 * 60 * 60 * 24)).toInt()
                    if (restDays < 0) restDays = 0
                }
            } catch (e: Exception) { }
        }

        // 3. Compute Streak
        // Simplistic streak: count consecutive weeks worked out
        var streak = 0
        if (workoutDates.isNotEmpty()) {
            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY
            val activeWeeks = mutableSetOf<String>()
            for (dateStr in workoutDates) {
                try {
                    val d = format.parse(dateStr)
                    if (d != null) {
                        cal.time = d
                        activeWeeks.add("${cal.get(Calendar.YEAR)}-${cal.get(Calendar.WEEK_OF_YEAR)}")
                    }
                } catch (e: Exception) {}
            }
            
            cal.time = today
            while (true) {
                val weekKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.WEEK_OF_YEAR)}"
                if (activeWeeks.contains(weekKey)) {
                    streak++
                    cal.add(Calendar.WEEK_OF_YEAR, -1)
                } else {
                    // Check if they haven't worked out THIS week yet, but worked out LAST week.
                    if (streak == 0) {
                        cal.add(Calendar.WEEK_OF_YEAR, -1)
                        val prevWeekKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.WEEK_OF_YEAR)}"
                        if (activeWeeks.contains(prevWeekKey)) {
                            streak++
                            cal.add(Calendar.WEEK_OF_YEAR, -1)
                            continue
                        }
                    }
                    break
                }
            }
        }

        // 4. Muscle Stats
        val musclesMap = mutableMapOf<String, Int>()
        val exerciseMap = mutableMapOf<String, Int>()
        var monthlyVol = 0.0

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        // Past 7 Days Muscle logic
        val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
        val sevenDaysMuscles = mutableMapOf<String, Int>()

        for (log in logs) {
            musclesMap[log.muscle] = (musclesMap[log.muscle] ?: 0) + log.sets
            exerciseMap[log.title] = (exerciseMap[log.title] ?: 0) + log.sets
            
            try {
                val d = format.parse(log.startedAt.take(10))
                if (d != null) {
                    val c = Calendar.getInstance()
                    c.time = d
                    if (c.get(Calendar.MONTH) == currentMonth && c.get(Calendar.YEAR) == currentYear) {
                        monthlyVol += log.volume
                    }
                    
                    if (d.after(sevenDaysAgo)) {
                        sevenDaysMuscles[log.muscle] = (sevenDaysMuscles[log.muscle] ?: 0) + log.sets
                    }
                }
            } catch (e: Exception) {}
        }

        val topMuscles = musclesMap.toList().sortedByDescending { it.second }
        val topExercises = exerciseMap.toList().sortedByDescending { it.second }
        val topMuscleWeek = sevenDaysMuscles.maxByOrNull { it.value }?.key ?: "Full Body"

        return ProgressStats(
            workoutDates = workoutDates,
            currentStreakWeeks = streak,
            restDays = restDays,
            topMusclesBySets = topMuscles,
            topExercisesBySets = topExercises,
            monthlyVolumeKg = monthlyVol,
            topMuscleWeek = topMuscleWeek
        )
    }
}
