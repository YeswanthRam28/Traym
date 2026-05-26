package com.gymtracker.data

import com.gymtracker.auth.SessionManager
import org.json.JSONArray
import org.json.JSONObject

data class Exercise(
    val id: String,
    val name: String,
    val bodyPart: String,
    val equipment: String,
    val target: String,
    val secondaryMuscles: List<String>,
    val instructions: List<String>,
    val description: String,
    val difficulty: String,
    val category: String
)

object ExerciseRepository {
    private var allExercises: List<Exercise> = emptyList()
    
    // Using the user's provided RapidAPI Key
    const val RAPID_API_KEY = "39d4c7d724msh48bea839fcbd664p17d019jsn67f5e1cf7dee"
    const val RAPID_API_HOST = "exercisedb.p.rapidapi.com"

    init {
        loadData()
    }

    private fun loadData() {
        try {
            val inputStream = SessionManager.appContext.assets.open("exercises_db.json")
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(jsonStr)
            
            val list = mutableListOf<Exercise>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val secondaryArray = obj.optJSONArray("secondaryMuscles")
                val secondary = mutableListOf<String>()
                if (secondaryArray != null) {
                    for (j in 0 until secondaryArray.length()) {
                        secondary.add(secondaryArray.getString(j))
                    }
                }
                
                val instrArray = obj.optJSONArray("instructions")
                val instructions = mutableListOf<String>()
                if (instrArray != null) {
                    for (j in 0 until instrArray.length()) {
                        instructions.add(instrArray.getString(j))
                    }
                }

                list.add(
                    Exercise(
                        id = obj.optString("id"),
                        name = obj.optString("name").replaceFirstChar { it.uppercase() },
                        bodyPart = obj.optString("bodyPart"),
                        equipment = obj.optString("equipment"),
                        target = obj.optString("target"),
                        secondaryMuscles = secondary,
                        instructions = instructions,
                        description = obj.optString("description"),
                        difficulty = obj.optString("difficulty", "beginner"),
                        category = obj.optString("category")
                    )
                )
            }
            allExercises = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllExercises(): List<Exercise> = allExercises

    fun getBodyParts(): List<String> = allExercises.map { it.bodyPart.replaceFirstChar { c -> c.uppercase() } }.distinct().sorted()
    fun getTargets(): List<String> = allExercises.map { it.target.replaceFirstChar { c -> c.uppercase() } }.distinct().sorted()
    fun getEquipment(): List<String> = allExercises.map { it.equipment.replaceFirstChar { c -> c.uppercase() } }.distinct().sorted()

    fun searchByName(query: String): List<Exercise> {
        return allExercises.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun getByBodyPart(bodyPart: String): List<Exercise> {
        return allExercises.filter { it.bodyPart.equals(bodyPart, ignoreCase = true) }
    }

    fun getByTarget(target: String): List<Exercise> {
        return allExercises.filter { it.target.equals(target, ignoreCase = true) }
    }

    fun getByEquipment(equipment: String): List<Exercise> {
        return allExercises.filter { it.equipment.equals(equipment, ignoreCase = true) }
    }

    fun getById(id: String): Exercise? {
        return allExercises.find { it.id == id }
    }
}
