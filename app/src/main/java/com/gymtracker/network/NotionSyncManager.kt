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

    private fun extractNotionNumber(property: JSONObject?): Double {
        if (property == null) return 0.0
        if (property.has("number") && !property.isNull("number")) {
            return property.optDouble("number", 0.0)
        }
        val formula = property.optJSONObject("formula")
        if (formula != null && formula.has("number") && !formula.isNull("number")) {
            return formula.optDouble("number", 0.0)
        }
        val rollup = property.optJSONObject("rollup")
        if (rollup != null && rollup.has("number") && !rollup.isNull("number")) {
            return rollup.optDouble("number", 0.0)
        }
        return 0.0
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

                            val volume = extractNotionNumber(properties.optJSONObject("Volume"))
                            val duration = extractNotionNumber(properties.optJSONObject("Duration(min)")).toInt()
                            val reps = extractNotionNumber(properties.optJSONObject("Reps")).toInt()
                            val weight = extractNotionNumber(properties.optJSONObject("Weight"))
                            val type = properties.optJSONObject("Type")?.optJSONObject("select")?.optString("name") ?: "Working"

                            val activityType = properties.optJSONObject("Activity Type")?.optJSONObject("select")?.optString("name") ?: "Strength"
                            val workoutTitle = properties.optJSONObject("Workout")?.optJSONObject("select")?.optString("name") ?: "Custom Workout"
                            val muscle = properties.optJSONObject("Muscle")?.optJSONObject("select")?.optString("name") ?: "Full Body"
                            val equipment = properties.optJSONObject("Equipment")?.optJSONObject("select")?.optString("name") ?: "Bodyweight"
                            val cardioType = properties.optJSONObject("Cardio Type")?.optJSONObject("select")?.optString("name") ?: ""

                            val dateKey = startedAt.take(10)
                            var existingWorkout: JSONObject? = null
                            for (k in 0 until localWorkouts.length()) {
                                val w = localWorkouts.getJSONObject(k)
                                if (w.optString("title") == title && w.optString("started_at").take(10) == dateKey) {
                                    existingWorkout = w
                                    break
                                }
                            }

                            if (existingWorkout != null) {
                                val setsArr = existingWorkout.optJSONArray("sets") ?: JSONArray()
                                setsArr.put(JSONObject().apply {
                                    put("set_number", setsArr.length() + 1)
                                    put("weight_kg", weight)
                                    put("reps", reps)
                                    put("rpe", 8.0)
                                    put("notion_page_id", pageId)
                                })
                                existingWorkout.put("sets", setsArr)
                                existingWorkout.put("total_sets", existingWorkout.optInt("total_sets", 0) + 1)
                                existingWorkout.put("total_volume_kg", existingWorkout.optDouble("total_volume_kg", 0.0) + volume)
                                existingWorkout.put("total_reps", existingWorkout.optInt("total_reps", 0) + reps)
                                val existingDuration = existingWorkout.optInt("duration_seconds", 0)
                                if (duration * 60 > existingDuration) {
                                    existingWorkout.put("duration_seconds", duration * 60)
                                }
                                existingWorkout.put("activity_type", activityType)
                                existingWorkout.put("workout_title", workoutTitle)
                                existingWorkout.put("muscle", muscle)
                                existingWorkout.put("equipment", equipment)
                                existingWorkout.put("cardio_type", cardioType)
                            } else {
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
                                    put("activity_type", activityType)
                                    put("workout_title", workoutTitle)
                                    put("muscle", muscle)
                                    put("equipment", equipment)
                                    put("cardio_type", cardioType)
                                    
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
                            }
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
                                
                                if (notionId.isEmpty()) {
                                    val createdPageId = pushSetToNotionSync(token, dbId, localW, s)
                                    if (createdPageId != null) {
                                        s.put("notion_page_id", createdPageId)
                                        pushedCount++
                                    }
                                }
                            }
                        } else {
                            // Cardio or legacy without sets array
                            val notionId = localW.optString("notion_page_id")
                            if (notionId.isEmpty()) {
                                val createdPageId = pushSetToNotionSync(token, dbId, localW, null)
                                if (createdPageId != null) {
                                    localW.put("notion_page_id", createdPageId)
                                    pushedCount++
                                }
                            }
                        }
                    }

                    if (pushedCount > 0 || updatedCount > 0) {
                        writeJsonArray("workouts.json", localWorkouts)
                    }

                    // Rebuild PRs from the updated localWorkouts array
                    val prs = JSONObject()
                    for (i in 0 until localWorkouts.length()) {
                        val w = localWorkouts.getJSONObject(i)
                        val title = w.optString("title")
                        val lowerTitle = title.lowercase()
                        if (lowerTitle.contains("bench press") || lowerTitle.contains("squat") || lowerTitle.contains("deadlift")) {
                            val sets = w.optJSONArray("sets") ?: JSONArray()
                            for (j in 0 until sets.length()) {
                                val s = sets.getJSONObject(j)
                                val weightKg = s.optDouble("weight_kg", 0.0)
                                val currentPr = prs.optDouble(title, 0.0)
                                if (weightKg > currentPr) {
                                    prs.put(title, weightKg)
                                }
                            }
                        }
                    }
                    if (prs.length() > 0) {
                        try { getFile("prs.json").writeText(prs.toString()) } catch (e: Exception) {}
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
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
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
            val notionPageId = if (setObj != null) setObj.optString("notion_page_id", "") else workout.optString("notion_page_id", "")
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

    fun createDatabase(token: String, onResult: (Boolean, String) -> Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // 1. Search for a page shared with the integration
                val searchBody = JSONObject().apply {
                    put("query", "")
                    put("filter", JSONObject().apply {
                        put("value", "page")
                        put("property", "object")
                    })
                }
                
                val searchReq = okhttp3.Request.Builder()
                    .url("https://api.notion.com/v1/search")
                    .post(okhttp3.RequestBody.create(mediaType, searchBody.toString()))
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Notion-Version", "2022-06-28")
                    .build()
                    
                var pageId: String? = null
                client.newCall(searchReq).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val resJson = JSONObject(bodyStr)
                        val results = resJson.optJSONArray("results")
                        if (results != null && results.length() > 0) {
                            pageId = results.getJSONObject(0).getString("id")
                        }
                    }
                }
                
                if (pageId == null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, "No page found. Please make sure you selected a page during Notion login.")
                    }
                    return@launch
                }
                
                // 2. Create the Database
                val dbBody = JSONObject().apply {
                    put("parent", JSONObject().apply {
                        put("type", "page_id")
                        put("page_id", pageId)
                    })
                    put("title", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", JSONObject().apply { put("content", "Traym Fitness Log") })
                        })
                    })
                    
                    val props = JSONObject()
                    props.put("Name", JSONObject().apply { put("title", JSONObject()) })
                    props.put("Date", JSONObject().apply { put("date", JSONObject()) })
                    props.put("Activity Type", JSONObject().apply { put("select", JSONObject()) })
                    props.put("Workout", JSONObject().apply { put("rich_text", JSONObject()) })
                    props.put("Workout Type", JSONObject().apply { put("select", JSONObject()) })
                    props.put("Exercise", JSONObject().apply { put("rich_text", JSONObject()) })
                    props.put("Muscle", JSONObject().apply { put("select", JSONObject()) })
                    props.put("Equipment", JSONObject().apply { put("select", JSONObject()) })
                    props.put("Type", JSONObject().apply { put("select", JSONObject()) })
                    props.put("Weight", JSONObject().apply { put("number", JSONObject().apply { put("format", "number") }) })
                    props.put("Reps", JSONObject().apply { put("number", JSONObject().apply { put("format", "number") }) })
                    props.put("Cardio Type", JSONObject().apply { put("rich_text", JSONObject()) })
                    props.put("RPE", JSONObject().apply { put("number", JSONObject().apply { put("format", "number") }) })
                    props.put("Duration(min)", JSONObject().apply { put("number", JSONObject().apply { put("format", "number") }) })
                    props.put("Distance (km)", JSONObject().apply { put("number", JSONObject().apply { put("format", "number") }) })
                    props.put("Completed", JSONObject().apply { put("checkbox", JSONObject()) })
                    props.put("1RM Estimation", JSONObject().apply { put("number", JSONObject().apply { put("format", "number") }) })
                    props.put("Volume", JSONObject().apply { put("number", JSONObject().apply { put("format", "number") }) })
                    props.put("DOW", JSONObject().apply { put("rich_text", JSONObject()) })
                    
                    put("properties", props)
                }

                val createReq = okhttp3.Request.Builder()
                    .url("https://api.notion.com/v1/databases")
                    .post(okhttp3.RequestBody.create(mediaType, dbBody.toString()))
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("Notion-Version", "2022-06-28")
                    .build()
                    
                client.newCall(createReq).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val dbJson = JSONObject(bodyStr)
                        val dbId = dbJson.getString("id")
                        com.gymtracker.auth.SessionManager.saveNotionConfig(token, dbId, true)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, "Database created successfully")
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(false, "API Error: ${response.code} $bodyStr")
                        }
                    }
                }
                
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, e.message ?: "Unknown error")
                }
            }
        }
    }
}
