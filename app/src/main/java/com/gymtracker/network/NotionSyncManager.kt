package com.gymtracker.network

import com.gymtracker.auth.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object NotionSyncManager {

    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val syncMutex = Mutex()

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

    private fun writeJsonArray(fileName: String, json: JSONArray) {
        try {
            getFile(fileName).writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncWithNotion(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val token = SessionManager.getNotionToken()
        val dbId = SessionManager.getNotionDatabaseId()

        if (token.isEmpty() || dbId.isEmpty()) {
            onError("Notion integration is not fully configured.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            syncMutex.withLock {
                try {
                    // 1. Fetch pages from Notion database
                    val request = Request.Builder()
                        .url("https://api.notion.com/v1/databases/$dbId/query")
                        .post("{}".toRequestBody(mediaType))
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Notion-Version", "2022-06-28")
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string() ?: "{}"

                    if (!response.isSuccessful) {
                        val errMsg = try {
                            JSONObject(responseBody).optString("message", "Unknown error")
                        } catch (e: Exception) {
                            "HTTP ${response.code}"
                        }
                        onError("Notion query failed: $errMsg")
                        return@launch
                    }

                    val jsonResponse = JSONObject(responseBody)
                    val results = jsonResponse.optJSONArray("results") ?: JSONArray()

                    val localWorkouts = readJsonArray("workouts.json")
                    val syncedNotionPageIds = mutableSetOf<String>()
                    val localWorkoutNotionIds = mutableSetOf<String>()

                    for (i in 0 until localWorkouts.length()) {
                        val w = localWorkouts.getJSONObject(i)
                        val notionId = w.optString("notion_page_id")
                        if (notionId.isNotEmpty()) {
                            localWorkoutNotionIds.add(notionId)
                        }
                        val sets = w.optJSONArray("sets")
                        if (sets != null) {
                            for (j in 0 until sets.length()) {
                                val s = sets.getJSONObject(j)
                                val setNotionId = s.optString("notion_page_id")
                                if (setNotionId.isNotEmpty()) {
                                    localWorkoutNotionIds.add(setNotionId)
                                }
                            }
                        }
                    }

                    var fetchedCount = 0

                    // 2. Parse Notion pages and insert missing ones locally
                    for (i in 0 until results.length()) {
                        val page = results.getJSONObject(i)
                        val pageId = page.optString("id")
                        if (pageId.isEmpty()) continue
                        
                        syncedNotionPageIds.add(pageId)

                        // If this Notion page is not synced locally, import it
                        if (!localWorkoutNotionIds.contains(pageId)) {
                            val properties = page.optJSONObject("properties") ?: continue
                            
                            val nameObj = properties.optJSONObject("Name")
                            val nameArray = nameObj?.optJSONArray("title")
                            val title = if (nameArray != null && nameArray.length() > 0) {
                                nameArray.getJSONObject(0).optJSONObject("text")?.optString("content") ?: "Workout"
                            } else "Workout"

                            val dateObj = properties.optJSONObject("Date")?.optJSONObject("date")
                            val startedAt = dateObj?.optString("start") ?: ""

                            val volume = properties.optJSONObject("Volume")?.optDouble("number", 0.0) ?: 0.0
                            val duration = properties.optJSONObject("Duration(min)")?.optInt("number", 0) ?: 0
                            val reps = properties.optJSONObject("Reps")?.optInt("number", 0) ?: 0
                            val weight = properties.optJSONObject("Weight")?.optDouble("number", 0.0) ?: 0.0
                            val type = properties.optJSONObject("Type")?.optJSONObject("select")?.optString("name") ?: "Working"

                            val newLocalWorkout = JSONObject().apply {
                                put("id", pageId)
                                put("notion_page_id", pageId)
                                put("title", title)
                                put("started_at", startedAt)
                                put("completed_at", startedAt)
                                put("total_volume_kg", volume)
                                put("duration_seconds", duration * 60)
                                put("total_sets", 1)
                                put("total_reps", reps)
                                
                                val setsArr = JSONArray().put(JSONObject().apply {
                                    put("set_number", 1)
                                    put("weight_kg", weight)
                                    put("reps", reps)
                                    put("rpe", 8.0)
                                    put("notion_page_id", pageId)
                                })
                                put("sets", setsArr)
                            }
                            localWorkouts.put(newLocalWorkout)
                            localWorkoutNotionIds.add(pageId)
                            fetchedCount++
                        }
                    }

                    if (fetchedCount > 0) {
                        writeJsonArray("workouts.json", localWorkouts)
                    }

                    // 3. Push/Update local workouts/sets to Notion
                    var pushedCount = 0
                    var updatedCount = 0
                    
                    for (i in 0 until localWorkouts.length()) {
                        val localW = localWorkouts.getJSONObject(i)
                        val sets = localW.optJSONArray("sets")
                        
                        if (sets != null && sets.length() > 0) {
                            for (j in 0 until sets.length()) {
                                val s = sets.getJSONObject(j)
                                val notionId = s.optString("notion_page_id")
                                
                                if (notionId.isEmpty() || !syncedNotionPageIds.contains(notionId)) {
                                    val createdPageId = pushSetToNotionSync(token, dbId, localW, s)
                                    if (createdPageId != null) {
                                        s.put("notion_page_id", createdPageId)
                                        pushedCount++
                                    }
                                } else {
                                    // Update existing page
                                    val updatedPageId = pushSetToNotionSync(token, dbId, localW, s)
                                    if (updatedPageId != null) {
                                        updatedCount++
                                    }
                                }
                            }
                        } else {
                            // Cardio or legacy without sets array
                            val notionId = localW.optString("notion_page_id")
                            if (notionId.isEmpty() || !syncedNotionPageIds.contains(notionId)) {
                                val createdPageId = pushSetToNotionSync(token, dbId, localW, null)
                                if (createdPageId != null) {
                                    localW.put("notion_page_id", createdPageId)
                                    pushedCount++
                                }
                            } else {
                                val updatedPageId = pushSetToNotionSync(token, dbId, localW, null)
                                if (updatedPageId != null) {
                                    updatedCount++
                                }
                            }
                        }
                    }

                    if (pushedCount > 0 || updatedCount > 0) {
                        writeJsonArray("workouts.json", localWorkouts)
                    }

                    onSuccess("Sync complete. Fetched $fetchedCount from Notion. Pushed $pushedCount new items, updated $updatedCount items in Notion.")
                } catch (e: Exception) {
                    onError("Sync error: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    fun pushWorkoutToNotion(workout: WorkoutSummaryResponse) {
        val token = SessionManager.getNotionToken()
        val dbId = SessionManager.getNotionDatabaseId()
        val enabled = SessionManager.isNotionSyncEnabled()

        if (token.isEmpty() || dbId.isEmpty() || !enabled) return

        syncWithNotion(
            onSuccess = { System.out.println("Auto-sync success: $it") },
            onError = { System.err.println("Auto-sync error: $it") }
        )
    }

    private fun getDowFromDateString(dateString: String): String {
        try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            val date = format.parse(dateString) ?: java.util.Date()
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            return when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.MONDAY -> "🟦 Mon"
                java.util.Calendar.TUESDAY -> "🟩 Tue"
                java.util.Calendar.WEDNESDAY -> "🟨 Wed"
                java.util.Calendar.THURSDAY -> "🟧 Thu"
                java.util.Calendar.FRIDAY -> "🟥 Fri"
                java.util.Calendar.SATURDAY -> "🟪 Sat"
                java.util.Calendar.SUNDAY -> "⬛ Sun"
                else -> "🟦 Mon"
            }
        } catch (e: Exception) {
            return "🟦 Mon"
        }
    }

    private fun pushSetToNotionSync(token: String, dbId: String, workout: JSONObject, setObj: JSONObject?): String? {
        try {
            val notionPageId = setObj?.optString("notion_page_id") ?: workout.optString("notion_page_id")
            val isUpdate = notionPageId.isNotEmpty()
            
            val title = workout.optString("title", "Workout")
            val startedAt = workout.optString("started_at")
            val activityType = workout.optString("activity_type", "Strength")
            val cardioType = workout.optString("cardio_type", "")
            val equipment = workout.optString("equipment", "Bodyweight")
            val muscle = workout.optString("muscle", "Full Body")
            val workoutTitle = workout.optString("workout_title", "Custom Workout")
            
            val workoutType = when {
                workoutTitle.lowercase().contains("pull") -> "Pull"
                workoutTitle.lowercase().contains("push") -> "Push"
                workoutTitle.lowercase().contains("leg") -> "Legs"
                activityType == "Cardio" -> "Cardio"
                else -> "Full Body"
            }

            val bodyJson = JSONObject().apply {
                if (!isUpdate) {
                    put("parent", JSONObject().put("database_id", dbId))
                }
                
                val properties = JSONObject().apply {
                    put("Name", JSONObject().put("title", JSONArray().put(JSONObject().put("text", JSONObject().put("content", title)))))
                    if (startedAt.isNotEmpty()) {
                        put("Date", JSONObject().put("date", JSONObject().put("start", startedAt)))
                    }
                    put("Activity Type", JSONObject().put("select", JSONObject().put("name", activityType)))
                    put("Workout", JSONObject().put("select", JSONObject().put("name", workoutTitle)))
                    put("Workout Type", JSONObject().put("select", JSONObject().put("name", workoutType)))
                    put("Exercise", JSONObject().put("select", JSONObject().put("name", title)))
                    put("Muscle", JSONObject().put("select", JSONObject().put("name", muscle)))
                    put("Equipment", JSONObject().put("select", JSONObject().put("name", equipment)))
                    put("Completed", JSONObject().put("checkbox", true))
                    put("DOW", JSONObject().put("select", JSONObject().put("name", getDowFromDateString(startedAt))))
                    
                    if (setObj != null) {
                        val weight = setObj.optDouble("weight_kg", 0.0)
                        val reps = setObj.optInt("reps", 0)
                        val rpe = setObj.optDouble("rpe", 8.0)
                        
                        put("Type", JSONObject().put("select", JSONObject().put("name", "Working")))
                        put("Weight", JSONObject().put("number", weight))
                        put("Reps", JSONObject().put("number", reps))
                        put("RPE", JSONObject().put("number", rpe))
                        put("Volume", JSONObject().put("number", weight * reps))
                    } else {
                        val typeName = if (activityType == "Cardio") "Session" else "Working"
                        put("Type", JSONObject().put("select", JSONObject().put("name", typeName)))
                        
                        val durationSeconds = workout.optInt("duration_seconds", 0)
                        val durationMin = durationSeconds / 60
                        put("Duration(min)", JSONObject().put("number", durationMin.toDouble()))
                        
                        if (activityType == "Cardio") {
                            if (cardioType.isNotEmpty()) {
                                put("Cardio Type", JSONObject().put("select", JSONObject().put("name", cardioType)))
                            }
                            put("Reps", JSONObject().put("number", 0))
                            put("Weight", JSONObject().put("number", 0))
                        }
                    }
                }
                put("properties", properties)
            }

            val url = if (isUpdate) "https://api.notion.com/v1/pages/$notionPageId" else "https://api.notion.com/v1/pages"
            val builder = Request.Builder().url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Notion-Version", "2022-06-28")
                
            if (isUpdate) {
                builder.patch(bodyJson.toString().toRequestBody(mediaType))
            } else {
                builder.post(bodyJson.toString().toRequestBody(mediaType))
            }

            val response = client.newCall(builder.build()).execute()
            val responseBody = response.body?.string() ?: "{}"
            if (response.isSuccessful) {
                val createdPage = JSONObject(responseBody)
                return createdPage.optString("id")
            } else {
                System.err.println("Notion Page Create/Update Error: $responseBody")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
