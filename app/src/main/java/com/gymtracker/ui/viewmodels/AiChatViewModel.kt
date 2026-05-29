package com.gymtracker.ui.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.auth.SessionManager
import com.gymtracker.network.ApiClient
import com.gymtracker.network.UpdatePlanRequest
import com.gymtracker.network.NotionSyncManager
import com.gymtracker.ui.screens.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AiChatViewModel : ViewModel() {
    
    val messages = mutableStateListOf<ChatMessage>()
    
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    init {
        loadChatHistory()
    }

    private fun loadChatHistory() {
        try {
            val filesDir = SessionManager.appContext.filesDir
            val historyFile = File(filesDir, "chat_history.json")
            if (historyFile.exists()) {
                val array = JSONArray(historyFile.readText())
                messages.clear()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    messages.add(ChatMessage(
                        text = obj.getString("text"),
                        isUser = obj.getBoolean("isUser")
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveChatHistory() {
        try {
            val filesDir = SessionManager.appContext.filesDir
            val historyFile = File(filesDir, "chat_history.json")
            val array = JSONArray()
            for (msg in messages) {
                if (msg.text.isNotEmpty() && msg.text != "Thinking...") {
                    val obj = JSONObject().apply {
                        put("text", msg.text)
                        put("isUser", msg.isUser)
                    }
                    array.put(obj)
                }
            }
            historyFile.writeText(array.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun connect() {
        if (messages.isNotEmpty()) {
            return
        }
        _isConnecting.value = true
        viewModelScope.launch {
            delay(300)
            _isConnecting.value = false
            // The AI does not need to send a 'hi' every time it starts.
            // The chat history will load in init{}, and if empty, we just wait for the user.
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        messages.add(ChatMessage(text, true))
        saveChatHistory()
        
        viewModelScope.launch {
            messages.add(ChatMessage("Thinking...", false))
            val messageIndex = messages.size - 1
            
            val response = callOpenRouterApi(text)
            
            messages[messageIndex] = ChatMessage("", false)
            val words = response.split(" ")
            var currentText = ""
            for (i in words.indices) {
                currentText += (if (i > 0) " " else "") + words[i]
                messages[messageIndex] = ChatMessage(currentText, false)
                delay(40)
            }
            saveChatHistory()
        }
    }

    private suspend fun callOpenRouterApi(userMessage: String): String {
        val messagesArr = JSONArray()
        
        // System message
        val systemPrompt = """
            You are Traym, a premium, hyper-personalized AI strength and conditioning coach.
            Your purpose is to motivate, advise, analyze form, and suggest plan modifications.
            Always maintain a helpful, encouraging, and expert tone. Keep responses relatively concise but highly informative and practical.
            
            You have access to tools that can directly modify the user's plan, update their profile settings, or run a Notion sync. If the user asks you to modify their training days, exercises, or sets/reps, or update their details, or backup/sync, use the appropriate tool.
            
            Here is the current state of the user's progress, training plan, and history:
            
            ${getUserContext()}
        """.trimIndent()
        
        messagesArr.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })
        
        // Conversation history (excluding typing indicators/placeholders)
        for (msg in messages) {
            if (msg.text.isNotEmpty() && msg.text != "Thinking...") {
                messagesArr.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "assistant")
                    put("content", msg.text)
                })
            }
        }
        
        return executeChatCompletion(messagesArr)
    }

    private suspend fun executeChatCompletion(messagesArr: JSONArray): String {
        val apiKey = com.gymtracker.BuildConfig.OPENROUTER_API_KEY
        val baseUrl = "https://openrouter.ai/api/v1"
        val modelName = "deepseek/deepseek-v4-flash"

        val toolsArr = JSONArray().apply {
            // update_plan
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "update_plan")
                    put("description", "Updates the active training plan split and exercises. Call this when the user asks to modify their training routine, add/remove/reorder exercises, or adjust sets/reps.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("plan_json", JSONObject().apply {
                                put("type", "string")
                                put("description", "The complete plan JSON. Structure matches plan.json: {\"split\": \"...\", \"week_number\": 1, \"days\": [{\"day\": \"MON\", \"title\": \"...\", \"exercises\": [{\"name\": \"...\", \"sets\": 3, \"reps\": 10}]}]}")
                            })
                        })
                        put("required", JSONArray().apply { put("plan_json") })
                    })
                })
            })
            // update_profile
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "update_profile")
                    put("description", "Updates the user's profile details such as name, goal, philosophy, experience level, or available equipment.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("name", JSONObject().apply { put("type", "string") })
                            put("goal", JSONObject().apply { put("type", "string") })
                            put("philosophy", JSONObject().apply { put("type", "string") })
                            put("experience_years", JSONObject().apply { put("type", "integer") })
                            put("equipment", JSONObject().apply {
                                put("type", "array")
                                put("items", JSONObject().apply { put("type", "string") })
                            })
                        })
                    })
                })
            })
            // modify_workout_history
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "modify_workout_history")
                    put("description", "Adds, deletes, or clears workout history logs. Action can be 'add', 'delete', or 'clear'.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("action", JSONObject().apply {
                                put("type", "string")
                                put("enum", JSONArray().apply { put("add"); put("delete"); put("clear") })
                            })
                            put("workout_id", JSONObject().apply {
                                put("type", "string")
                                put("description", "Required for 'delete' action. The ID of the workout to delete.")
                            })
                            put("title", JSONObject().apply {
                                put("type", "string")
                                put("description", "Required for 'add' action. Name of the workout or exercise logged.")
                            })
                            put("completed_at", JSONObject().apply {
                                put("type", "string")
                                put("description", "Optional for 'add' action. The date and time of completion (ISO 8601 format like YYYY-MM-DDTHH:mm:ssZ).")
                            })
                            put("total_volume_kg", JSONObject().apply {
                                put("type", "number")
                                put("description", "Required for 'add' action. Total weight lifted in kg.")
                            })
                            put("duration_seconds", JSONObject().apply {
                                put("type", "integer")
                                put("description", "Required for 'add' action. Workout duration in seconds.")
                            })
                            put("total_sets", JSONObject().apply {
                                put("type", "integer")
                                put("description", "Required for 'add' action. Total number of sets completed.")
                            })
                            put("total_reps", JSONObject().apply {
                                put("type", "integer")
                                put("description", "Required for 'add' action. Total number of reps completed.")
                            })
                        })
                        put("required", JSONArray().apply { put("action") })
                    })
                })
            })
            // update_pr
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "update_pr")
                    put("description", "Updates or adds a personal record (PR) weight for a specific exercise.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("exercise_name", JSONObject().apply { put("type", "string") })
                            put("weight_kg", JSONObject().apply { put("type", "number") })
                        })
                        put("required", JSONArray().apply { put("exercise_name"); put("weight_kg") })
                    })
                })
            })
            // delete_pr
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "delete_pr")
                    put("description", "Deletes a personal record (PR) for a specific exercise.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("exercise_name", JSONObject().apply { put("type", "string") })
                        })
                        put("required", JSONArray().apply { put("exercise_name") })
                    })
                })
            })
            // trigger_notion_sync
            put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", "trigger_notion_sync")
                    put("description", "Triggers a sync of completed workouts with the user's Notion database. Call this when the user asks to sync or backup their workouts.")
                    put("parameters", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject())
                    })
                })
            })
        }

        val jsonBody = JSONObject().apply {
            put("model", modelName)
            put("messages", messagesArr)
            put("tools", toolsArr)
            put("temperature", 0.7)
            put("max_tokens", 1024)
        }

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .post(jsonBody.toString().toRequestBody(mediaType))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val responseJson = JSONObject(responseBody)
                        val choices = responseJson.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val choiceObj = choices.getJSONObject(0)
                            val messageObj = choiceObj.getJSONObject("message")
                            val toolCalls = messageObj.optJSONArray("tool_calls")
                            
                            if (toolCalls != null && toolCalls.length() > 0) {
                                // Add assistant's message with tool calls to messages array
                                messagesArr.put(messageObj)
                                
                                for (i in 0 until toolCalls.length()) {
                                    val toolCall = toolCalls.getJSONObject(i)
                                    val callId = toolCall.optString("id")
                                    val functionObj = toolCall.getJSONObject("function")
                                    val funcName = functionObj.optString("name")
                                    val funcArgs = functionObj.optString("arguments")
                                    
                                    val toolResult = executeTool(funcName, funcArgs)
                                    
                                    // Append tool output message to messages array
                                    messagesArr.put(JSONObject().apply {
                                        put("role", "tool")
                                        put("tool_call_id", callId)
                                        put("name", funcName)
                                        put("content", toolResult)
                                    })
                                }
                                // Recurse with the tool responses appended
                                executeChatCompletion(messagesArr)
                            } else {
                                messageObj.optString("content", "")
                            }
                        } else {
                            "Error: Empty response choices from API."
                        }
                    } else {
                        val errorMsg = try {
                            JSONObject(responseBody).optString("message", "HTTP error ${response.code}")
                        } catch (e: Exception) {
                            "HTTP error ${response.code}"
                        }
                        "I'm sorry, I encountered an issue communicating with the service: $errorMsg"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "I'm sorry, I'm having trouble connecting right now. Please check your internet connection."
            }
        }
    }

    private suspend fun executeTool(name: String, argumentsStr: String): String {
        return try {
            val args = JSONObject(argumentsStr)
            when (name) {
                "update_plan" -> {
                    val planJson = args.getString("plan_json")
                    val response = ApiClient.apiService.updateActivePlan(com.gymtracker.network.UpdatePlanRequest(planJson))
                    if (response.isSuccessful) {
                        "Plan updated successfully!"
                    } else {
                        "Error updating plan: ${response.message()}"
                    }
                }
                "update_profile" -> {
                    val filesDir = SessionManager.appContext.filesDir
                    val profileFile = File(filesDir, "profile.json")
                    val profile = if (profileFile.exists()) JSONObject(profileFile.readText()) else JSONObject()
                    
                    if (args.has("name")) profile.put("name", args.getString("name"))
                    if (args.has("goal")) profile.put("goal", args.getString("goal"))
                    if (args.has("philosophy")) profile.put("philosophy", args.getString("philosophy"))
                    if (args.has("experience_years")) profile.put("experience_years", args.getInt("experience_years"))
                    if (args.has("equipment")) {
                        val eqArray = args.getJSONArray("equipment")
                        profile.put("equipment", eqArray)
                    }
                    
                    profileFile.writeText(profile.toString())
                    "Profile updated successfully! Updated profile: $profile"
                }
                "modify_workout_history" -> {
                    val filesDir = SessionManager.appContext.filesDir
                    val workoutsFile = File(filesDir, "workouts.json")
                    val action = args.getString("action")
                    when (action) {
                        "add" -> {
                            val workouts = if (workoutsFile.exists()) JSONArray(workoutsFile.readText()) else JSONArray()
                            val newWorkout = JSONObject().apply {
                                put("id", UUID.randomUUID().toString())
                                put("title", args.getString("title"))
                                val completedAt = if (args.has("completed_at")) {
                                    args.getString("completed_at")
                                } else {
                                    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                                    df.format(Calendar.getInstance().time)
                                }
                                put("completed_at", completedAt)
                                put("started_at", completedAt)
                                put("total_volume_kg", args.getDouble("total_volume_kg"))
                                put("duration_seconds", args.getInt("duration_seconds"))
                                put("total_sets", args.getInt("total_sets"))
                                put("total_reps", args.getInt("total_reps"))
                            }
                            workouts.put(newWorkout)
                            workoutsFile.writeText(workouts.toString())
                            "Workout log added successfully: $newWorkout"
                        }
                        "delete" -> {
                            val workoutId = args.getString("workout_id")
                            if (workoutsFile.exists()) {
                                val workouts = JSONArray(workoutsFile.readText())
                                val updatedWorkouts = JSONArray()
                                var found = false
                                for (i in 0 until workouts.length()) {
                                    val w = workouts.getJSONObject(i)
                                    if (w.optString("id") == workoutId) {
                                        found = true
                                    } else {
                                        updatedWorkouts.put(w)
                                    }
                                }
                                if (found) {
                                    workoutsFile.writeText(updatedWorkouts.toString())
                                    "Workout with ID $workoutId deleted successfully."
                                } else {
                                    "Workout ID $workoutId not found in history."
                                }
                            } else {
                                "No workout history to delete from."
                            }
                        }
                        "clear" -> {
                            workoutsFile.writeText(JSONArray().toString())
                            "All workout history cleared successfully."
                        }
                        else -> "Unknown action: $action"
                    }
                }
                "update_pr" -> {
                    val filesDir = SessionManager.appContext.filesDir
                    val prsFile = File(filesDir, "prs.json")
                    val prs = if (prsFile.exists()) JSONObject(prsFile.readText()) else JSONObject()
                    val exerciseName = args.getString("exercise_name")
                    val weight = args.getDouble("weight_kg")
                    prs.put(exerciseName, weight)
                    prsFile.writeText(prs.toString())
                    "PR for '$exerciseName' updated to $weight kg successfully."
                }
                "delete_pr" -> {
                    val filesDir = SessionManager.appContext.filesDir
                    val prsFile = File(filesDir, "prs.json")
                    if (prsFile.exists()) {
                        val prs = JSONObject(prsFile.readText())
                        val exerciseName = args.getString("exercise_name")
                        if (prs.has(exerciseName)) {
                            prs.remove(exerciseName)
                            prsFile.writeText(prs.toString())
                            "PR for '$exerciseName' deleted successfully."
                        } else {
                            "PR for '$exerciseName' not found."
                        }
                    } else {
                        "No PR database found."
                    }
                }
                "trigger_notion_sync" -> {
                    var syncResult = ""
                    val latch = java.util.concurrent.CountDownLatch(1)
                    withContext(Dispatchers.Main) {
                        com.gymtracker.network.NotionSyncManager.syncWithNotion(
                            onSuccess = { msg ->
                                syncResult = "Notion Sync Succeeded: $msg"
                                latch.countDown()
                            },
                            onError = { err ->
                                syncResult = "Notion Sync Failed: $err"
                                latch.countDown()
                            }
                        )
                    }
                    latch.await()
                    syncResult
                }
                else -> "Unknown tool: $name"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error executing tool: ${e.message}"
        }
    }

    private fun getUserContext(): String {
        val context = StringBuilder()
        val filesDir = SessionManager.appContext.filesDir

        // 1. Profile
        val profileFile = File(filesDir, "profile.json")
        if (profileFile.exists()) {
            try {
                val profileObj = JSONObject(profileFile.readText())
                context.append("### User Profile:\n")
                context.append("- Name: ${profileObj.optString("name", "Athlete")}\n")
                context.append("- Email: ${profileObj.optString("email")}\n")
                context.append("- Goal: ${profileObj.optString("goal")}\n")
                context.append("- Philosophy: ${profileObj.optString("philosophy")}\n")
                context.append("- Experience: ${profileObj.optString("experience_years")} years\n")
                val eq = profileObj.optJSONArray("equipment")
                if (eq != null) {
                    val eqList = mutableListOf<String>()
                    for (i in 0 until eq.length()) {
                        eqList.add(eq.getString(i))
                    }
                    context.append("- Available Equipment: ${eqList.joinToString(", ")}\n")
                }
                context.append("\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Plan
        val planFile = File(filesDir, "plan.json")
        if (planFile.exists()) {
            try {
                val planObj = JSONObject(planFile.readText())
                context.append("### Active Training Plan:\n")
                context.append("- Split: ${planObj.optString("split")}\n")
                context.append("- Week Number: ${planObj.optInt("week_number")}\n")
                val days = planObj.optJSONArray("days")
                if (days != null) {
                    for (i in 0 until days.length()) {
                        val dayObj = days.getJSONObject(i)
                        context.append("  * Day: ${dayObj.optString("day")} - Title: ${dayObj.optString("title")}\n")
                        val exercises = dayObj.optJSONArray("exercises")
                        if (exercises != null) {
                            for (j in 0 until exercises.length()) {
                                val ex = exercises.getJSONObject(j)
                                context.append("    - ${ex.optString("name")}: ${ex.optInt("sets")} sets x ${ex.optInt("reps")} reps\n")
                            }
                        }
                    }
                }
                context.append("\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. PRs
        val prsFile = File(filesDir, "prs.json")
        if (prsFile.exists()) {
            try {
                val prsObj = JSONObject(prsFile.readText())
                context.append("### Personal Records (PRs):\n")
                val keys = prsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    context.append("- $key: ${prsObj.optDouble(key)} kg\n")
                }
                context.append("\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Workouts History
        val workoutsFile = File(filesDir, "workouts.json")
        if (workoutsFile.exists()) {
            try {
                val workoutsArr = JSONArray(workoutsFile.readText())
                context.append("### Recent Workout Logs:\n")
                val limit = minOf(10, workoutsArr.length())
                for (i in 0 until limit) {
                    val w = workoutsArr.getJSONObject(workoutsArr.length() - 1 - i)
                    context.append("- Date: ${w.optString("completed_at")} - Workout: ${w.optString("title")}\n")
                    context.append("  * Total Sets: ${w.optInt("total_sets")}, Total Reps: ${w.optInt("total_reps")}\n")
                    context.append("  * Volume: ${w.optDouble("total_volume_kg")} kg, Duration: ${w.optInt("duration_seconds") / 60} min\n")
                }
                context.append("\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return context.toString()
    }
}
