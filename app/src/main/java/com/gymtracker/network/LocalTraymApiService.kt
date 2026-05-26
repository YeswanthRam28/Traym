package com.gymtracker.network

import com.gymtracker.auth.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocalTraymApiService : TraymApiService {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    private var exercisesDbCache: JSONArray? = null

    private fun getCleanMuscleGroup(bodyPart: String, target: String): String {
        val bp = bodyPart.lowercase()
        val tg = target.lowercase()
        return when {
            bp == "chest" -> "Chest"
            bp == "back" -> "Back"
            bp == "shoulders" -> "Shoulders"
            bp == "cardio" -> "Cardio"
            tg == "biceps" -> "Biceps"
            tg == "triceps" -> "Triceps"
            bp == "upper arms" && tg.contains("bicep") -> "Biceps"
            bp == "upper arms" && tg.contains("tricep") -> "Triceps"
            bp == "upper arms" -> "Arms"
            bp == "lower arms" -> "Arms"
            bp == "waist" || tg == "abs" -> "Core"
            bp == "upper legs" || bp == "lower legs" || tg == "quads" || tg == "hamstrings" || tg == "glutes" || tg == "calves" -> "Legs"
            else -> "Full Body"
        }
    }

    private fun getCleanEquipment(equipment: String): String {
        val eq = equipment.lowercase()
        return when {
            eq.contains("body weight") || eq.contains("bodyweight") -> "Bodyweight"
            eq.contains("barbell") -> "Barbell"
            eq.contains("dumbbell") -> "Dumbbell"
            eq.contains("cable") -> "Cable"
            eq.contains("machine") || eq.contains("leverage") -> "Machine"
            else -> "Bodyweight"
        }
    }

    private fun getExerciseMetadata(name: String): JSONObject {
        try {
            if (exercisesDbCache == null) {
                val inputStream: InputStream = SessionManager.appContext.assets.open("exercises_db.json")
                val jsonStr = inputStream.bufferedReader().use { it.readText() }
                exercisesDbCache = JSONArray(jsonStr)
            }
            val db = exercisesDbCache
            if (db != null) {
                val searchName = name.lowercase().trim()
                
                // 1. Try exact match first
                for (i in 0 until db.length()) {
                    val ex = db.getJSONObject(i)
                    val dbName = ex.optString("name").lowercase()
                    if (dbName == searchName) {
                        return ex
                    }
                }
                
                // 2. Try normalized exact match
                fun normalize(s: String): String {
                    return s.replace("db", "dumbbell")
                            .replace("barbell / db", "barbell")
                            .replace("db / barbell", "dumbbell")
                            .replace("neutral / overhand", "")
                            .replace("overhand / neutral", "")
                            .replace(" / ", " ")
                            .replace("-", " ")
                            .replace("(", "")
                            .replace(")", "")
                            .trim()
                }
                val normSearch = normalize(searchName)
                for (i in 0 until db.length()) {
                    val ex = db.getJSONObject(i)
                    val dbName = normalize(ex.optString("name").lowercase())
                    if (dbName == normSearch) {
                        return ex
                    }
                }
                
                // 3. Try helper keyword maps
                if (searchName.contains("bench press")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "barbell bench press") return ex
                    }
                }
                if (searchName.contains("squat")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "barbell full squat" || dbName == "barbell squat") return ex
                    }
                }
                if (searchName.contains("deadlift")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "barbell deadlift") return ex
                    }
                }
                if (searchName.contains("lat pulldown")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "cable lat pulldown full range of motion" || dbName.contains("lat pulldown")) return ex
                    }
                }
                if (searchName.contains("pull-up") || searchName.contains("pull up")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "pull-up" || dbName == "assisted pull-up") return ex
                    }
                }
                if (searchName.contains("chin-up") || searchName.contains("chin up")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "chin-up") return ex
                    }
                }
                if (searchName.contains("push-up") || searchName.contains("push up")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "push-up") return ex
                    }
                }
                if (searchName.contains("bicep curl") || searchName.contains("biceps curl") || (searchName.contains("curl") && searchName.contains("db"))) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "dumbbell biceps curl" || dbName == "barbell curl") return ex
                    }
                }
                if (searchName.contains("shoulder press") || searchName.contains("overhead press")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "dumbbell shoulder press" || dbName == "barbell standing overhead press") return ex
                    }
                }
                if (searchName.contains("lateral raise")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "dumbbell lateral raise") return ex
                    }
                }
                if (searchName.contains("leg press")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName.contains("leg press")) return ex
                    }
                }
                if (searchName.contains("leg curl")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "lever lying leg curl" || dbName.contains("leg curl")) return ex
                    }
                }
                if (searchName.contains("row")) {
                    for (i in 0 until db.length()) {
                        val ex = db.getJSONObject(i)
                        val dbName = ex.optString("name")
                        if (dbName == "dumbbell row" || dbName == "cable seated row" || dbName == "barbell bent over row") return ex
                    }
                }

                // 4. Word containment match
                val searchWords = searchName.split(" ", "/", "-").filter { it.length > 2 }
                var bestMatch: JSONObject? = null
                var maxMatchedWords = 0
                for (i in 0 until db.length()) {
                    val ex = db.getJSONObject(i)
                    val dbName = ex.optString("name").lowercase()
                    var matchedWords = 0
                    for (word in searchWords) {
                        if (dbName.contains(word)) {
                            matchedWords++
                        }
                    }
                    if (matchedWords > maxMatchedWords) {
                        maxMatchedWords = matchedWords
                        bestMatch = ex
                    }
                }
                if (bestMatch != null && maxMatchedWords >= 1) {
                    return bestMatch
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return JSONObject()
    }

    private fun getFile(fileName: String): File {
        return File(SessionManager.appContext.filesDir, fileName)
    }

    private fun readJson(fileName: String): JSONObject {
        val file = getFile(fileName)
        if (!file.exists()) {
            if (fileName == "profile.json") {
                val defaultProfile = JSONObject().apply {
                    put("name", "Athlete")
                    put("email", "athlete@example.com")
                    put("goal", "muscle hypertrophy")
                    put("philosophy", "hypertrophy")
                    put("experience_years", 3)
                    put("equipment", JSONArray().apply {
                        put("barbell")
                        put("dumbbell")
                        put("cables")
                        put("machines")
                    })
                    put("onboarding_complete", true)
                }
                try {
                    file.writeText(defaultProfile.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return defaultProfile
            }
            return JSONObject()
        }
        return try {
            JSONObject(file.readText())
        } catch (e: Exception) {
            JSONObject()
        }
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

    private fun writeJson(fileName: String, json: JSONObject) {
        try {
            getFile(fileName).writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeJsonArray(fileName: String, json: JSONArray) {
        try {
            getFile(fileName).writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun syncUser(body: SyncUserRequest): Response<UserProfileResponse> {
        val profile = readJson("profile.json")
        val email = body.email ?: profile.optString("email", "offline_user@example.com")
        val name = body.name ?: profile.optString("name", "Local Athlete")
        
        profile.put("email", email)
        profile.put("name", name)
        if (!profile.has("onboarding_complete")) {
            profile.put("onboarding_complete", SessionManager.onboardingComplete)
        }
        writeJson("profile.json", profile)

        return Response.success(
            UserProfileResponse(
                id = profile.optString("id", UUID.randomUUID().toString()),
                clerk_user_id = "local_clerk_id",
                name = name,
                email = email,
                goal = profile.optString("goal", null),
                philosophy = profile.optString("philosophy", null),
                onboarding_complete = profile.optBoolean("onboarding_complete", false),
                chatgpt_import_processed = true
            )
        )
    }

    override suspend fun completeOnboarding(body: OnboardingRequest): Response<UserProfileResponse> {
        val profile = readJson("profile.json")
        profile.put("goal", body.goal)
        profile.put("experience_years", body.experience_years)
        
        val eqArray = JSONArray()
        body.equipment.forEach { eqArray.put(it) }
        profile.put("equipment", eqArray)
        profile.put("philosophy", body.philosophy)
        profile.put("onboarding_complete", true)
        
        writeJson("profile.json", profile)
        SessionManager.onboardingComplete = true

        // Generate and save a default active training plan locally
        saveDefaultPlan(body.philosophy)

        return Response.success(
            UserProfileResponse(
                id = profile.optString("id", UUID.randomUUID().toString()),
                clerk_user_id = "local_clerk_id",
                name = profile.optString("name", "Local Athlete"),
                email = profile.optString("email", "offline_user@example.com"),
                goal = body.goal,
                philosophy = body.philosophy,
                onboarding_complete = true,
                chatgpt_import_processed = true
            )
        )
    }

    override suspend fun getMe(): Response<UserProfileResponse> {
        val profile = readJson("profile.json")
        return Response.success(
            UserProfileResponse(
                id = profile.optString("id", UUID.randomUUID().toString()),
                clerk_user_id = "local_clerk_id",
                name = profile.optString("name", "Local Athlete"),
                email = profile.optString("email", "offline_user@example.com"),
                goal = profile.optString("goal", null),
                philosophy = profile.optString("philosophy", null),
                onboarding_complete = profile.optBoolean("onboarding_complete", false),
                chatgpt_import_processed = true
            )
        )
    }

    override suspend fun uploadChatGPTExport(file: okhttp3.MultipartBody.Part): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun startWorkout(body: StartWorkoutRequest): Response<WorkoutSummaryResponse> {
        val id = UUID.randomUUID().toString()
        val startedAt = dateFormat.format(Date())
        
        val currentWorkout = JSONObject().apply {
            put("id", id)
            put("title", body.title)
            put("started_at", startedAt)
        }
        writeJson("current_workout.json", currentWorkout)
        writeJsonArray("current_workout_sets.json", JSONArray())

        return Response.success(
            WorkoutSummaryResponse(
                id = id,
                title = body.title,
                started_at = startedAt,
                completed_at = null,
                total_volume_kg = 0.0f,
                duration_seconds = 0
            )
        )
    }

    override suspend fun logSets(workoutId: String, body: LogSetsRequest): Response<LogSetsResponse> {
        val sets = readJsonArray("current_workout_sets.json")
        body.sets.forEach { set ->
            val setObj = JSONObject().apply {
                put("exercise_name", set.exercise_name)
                put("set_number", set.set_number)
                put("weight_kg", set.weight_kg?.toDouble() ?: 0.0)
                put("reps", set.reps ?: 0)
                put("rpe", set.rpe?.toDouble() ?: 8.0)
                put("rest_seconds", set.rest_seconds ?: 90)
            }
            sets.put(setObj)
        }
        writeJsonArray("current_workout_sets.json", sets)

        return Response.success(LogSetsResponse(logged = body.sets.size))
    }

    private fun isBigThree(exerciseName: String): Boolean {
        val lower = exerciseName.lowercase()
        return lower.contains("bench press") || lower.contains("deadlift") || lower.contains("squat")
    }

    override suspend fun completeWorkout(workoutId: String, body: CompleteWorkoutRequest): Response<WorkoutSummaryResponse> {
        val currentWorkout = readJson("current_workout.json")
        val currentSets = readJsonArray("current_workout_sets.json")
        
        val completedAt = dateFormat.format(Date())
        val startedAtStr = currentWorkout.optString("started_at")
        val startedAt = try { dateFormat.parse(startedAtStr) ?: Date() } catch (e: Exception) { Date() }
        val durationSeconds = ((Date().time - startedAt.time) / 1000).toInt()

        var totalVolume = 0.0
        var totalReps = 0
        
        val prs = readJson("prs.json")
        
        for (i in 0 until currentSets.length()) {
            val s = currentSets.getJSONObject(i)
            val weight = s.optDouble("weight_kg", 0.0)
            val reps = s.optInt("reps", 0)
            totalVolume += weight * reps
            totalReps += reps

            // PR Check
            val exName = s.optString("exercise_name")
            if (isBigThree(exName)) {
                val currentPrWeight = prs.optDouble(exName, 0.0)
                if (weight > currentPrWeight) {
                    prs.put(exName, weight)
                }
            }
        }
        writeJson("prs.json", prs)

        val summary = WorkoutSummaryResponse(
            id = currentWorkout.optString("id"),
            title = currentWorkout.optString("title"),
            started_at = startedAtStr,
            completed_at = completedAt,
            total_volume_kg = totalVolume.toFloat(),
            duration_seconds = durationSeconds
        )

        // Save to workouts history
        val workouts = readJsonArray("workouts.json")
        
        // Group currentSets by exercise name
        val exerciseSetsMap = mutableMapOf<String, MutableList<JSONObject>>()
        for (i in 0 until currentSets.length()) {
            val s = currentSets.getJSONObject(i)
            val exName = s.optString("exercise_name")
            if (exName.isNotEmpty()) {
                exerciseSetsMap.getOrPut(exName) { mutableListOf() }.add(s)
            }
        }
        
        if (exerciseSetsMap.isEmpty()) {
            val summaryObj = JSONObject().apply {
                put("id", summary.id)
                put("title", summary.title)
                put("started_at", summary.started_at)
                put("completed_at", summary.completed_at)
                put("total_volume_kg", summary.total_volume_kg?.toDouble() ?: 0.0)
                put("duration_seconds", summary.duration_seconds ?: 0)
                put("total_sets", 0)
                put("total_reps", 0)
            }
            workouts.put(summaryObj)
        } else {
            for ((exName, sets) in exerciseSetsMap) {
                var exVolume = 0.0
                var exReps = 0
                for (s in sets) {
                    val weight = s.optDouble("weight_kg", 0.0)
                    val reps = s.optInt("reps", 0)
                    exVolume += weight * reps
                    exReps += reps
                }
                
                val summaryObj = JSONObject().apply {
                    put("id", UUID.randomUUID().toString())
                    put("title", exName)
                    put("started_at", summary.started_at)
                    put("completed_at", summary.completed_at)
                    put("total_volume_kg", exVolume)
                    put("duration_seconds", durationSeconds / exerciseSetsMap.size)
                    put("total_sets", sets.size)
                    put("total_reps", exReps)
                    
                    // Notion sync metadata
                    val meta = getExerciseMetadata(exName)
                    val bodyPart = meta.optString("bodyPart", "Full Body")
                    val target = meta.optString("target", "Full Body")
                    val eq = meta.optString("equipment", "Bodyweight")

                    put("workout_title", summary.title)
                    put("equipment", getCleanEquipment(eq))
                    put("muscle", getCleanMuscleGroup(bodyPart, target))
                    put("activity_type", if (meta.optString("category", "strength") == "cardio") "Cardio" else "Strength")
                    
                    // For cardio workouts, try to infer the type
                    if (exName.lowercase().contains("run") || exName.lowercase().contains("jog")) put("cardio_type", "Running")
                    else if (exName.lowercase().contains("walk")) put("cardio_type", "Walking")
                    else if (exName.lowercase().contains("cycle") || exName.lowercase().contains("bike")) put("cardio_type", "Cycling")
                    else if (exName.lowercase().contains("hiit")) put("cardio_type", "HIIT")

                    val setsArray = JSONArray()
                    for (s in sets) {
                        val setObj = JSONObject().apply {
                            put("set_number", s.optInt("set_number"))
                            put("weight_kg", s.optDouble("weight_kg", 0.0))
                            put("reps", s.optInt("reps", 0))
                            put("rpe", s.optDouble("rpe", 8.0))
                            put("rest_seconds", s.optInt("rest_seconds", 90))
                        }
                        setsArray.put(setObj)
                    }
                    put("sets", setsArray)
                }
                workouts.put(summaryObj)
            }
        }
        writeJsonArray("workouts.json", workouts)

        // Clean current temporary files
        getFile("current_workout.json").delete()
        getFile("current_workout_sets.json").delete()

        // Sync to Notion database if enabled
        NotionSyncManager.pushWorkoutToNotion(summary)

        return Response.success(summary)
    }

    override suspend fun logInlineWorkout(body: InlineLogRequest): Response<WorkoutSummaryResponse> {
        val workouts = readJsonArray("workouts.json")
        val prs = readJson("prs.json")
        val todayStr = dateFormat.format(Date()).substring(0, 10)
        
        var exVolume = 0.0
        var exReps = 0
        var maxWeight = 0.0
        val completedSetsCount = body.sets.size
        
        for (s in body.sets) {
            val weight = s.weight_kg?.toDouble() ?: 0.0
            val reps = s.reps ?: 0
            exVolume += weight * reps
            exReps += reps
            if (weight > maxWeight) maxWeight = weight
        }

        if (isBigThree(body.exerciseName)) {
            val currentPrWeight = prs.optDouble(body.exerciseName, 0.0)
            if (maxWeight > currentPrWeight) {
                prs.put(body.exerciseName, maxWeight)
                writeJson("prs.json", prs)
            }
        }
        
        var targetObj: JSONObject? = null
        for (i in 0 until workouts.length()) {
            val w = workouts.getJSONObject(i)
            if (w.optString("title") == body.exerciseName && w.optString("started_at").startsWith(todayStr)) {
                targetObj = w
                break
            }
        }

        if (targetObj == null) {
            targetObj = JSONObject().apply {
                put("id", UUID.randomUUID().toString())
                put("title", body.exerciseName)
                put("started_at", dateFormat.format(Date()))
                put("completed_at", dateFormat.format(Date()))
                
                val meta = getExerciseMetadata(body.exerciseName)
                val bodyPart = meta.optString("bodyPart", "Full Body")
                val target = meta.optString("target", "Full Body")
                val eq = meta.optString("equipment", "Bodyweight")

                put("workout_title", body.dayTitle)
                put("equipment", getCleanEquipment(eq))
                put("muscle", getCleanMuscleGroup(bodyPart, target))
                put("activity_type", if (meta.optString("category", "strength") == "cardio") "Cardio" else "Strength")
                
                val lowerName = body.exerciseName.lowercase()
                if (lowerName.contains("run") || lowerName.contains("jog")) put("cardio_type", "Running")
                else if (lowerName.contains("walk")) put("cardio_type", "Walking")
                else if (lowerName.contains("cycle") || lowerName.contains("bike")) put("cardio_type", "Cycling")
                else if (lowerName.contains("hiit")) put("cardio_type", "HIIT")
            }
            workouts.put(targetObj)
        }

        val oldSets = targetObj.optJSONArray("sets") ?: JSONArray()
        val oldSetsMap = mutableMapOf<Int, String>()
        for (i in 0 until oldSets.length()) {
            val s = oldSets.getJSONObject(i)
            val setNum = s.optInt("set_number")
            val pageId = s.optString("notion_page_id")
            if (pageId.isNotEmpty()) {
                oldSetsMap[setNum] = pageId
            }
        }

        val setsArray = JSONArray()
        for (s in body.sets) {
            val setObj = JSONObject().apply {
                put("set_number", s.set_number)
                put("weight_kg", s.weight_kg?.toDouble() ?: 0.0)
                put("reps", s.reps ?: 0)
                put("rpe", s.rpe?.toDouble() ?: 8.0)
                put("rest_seconds", s.rest_seconds ?: 90)
                val existingPageId = oldSetsMap[s.set_number]
                if (existingPageId != null) {
                    put("notion_page_id", existingPageId)
                }
            }
            setsArray.put(setObj)
        }
        targetObj.put("sets", setsArray)

        targetObj.put("total_volume_kg", exVolume)
        targetObj.put("total_sets", completedSetsCount)
        targetObj.put("total_reps", exReps)
        targetObj.put("duration_seconds", completedSetsCount * 60)

        writeJsonArray("workouts.json", workouts)
        
        val summary = WorkoutSummaryResponse(
            id = targetObj.optString("id"),
            title = targetObj.optString("title"),
            started_at = targetObj.optString("started_at"),
            completed_at = targetObj.optString("completed_at"),
            total_volume_kg = exVolume.toFloat(),
            duration_seconds = targetObj.optInt("duration_seconds", 0),
            total_sets = completedSetsCount,
            total_reps = exReps
        )
        
        NotionSyncManager.pushWorkoutToNotion(summary)
        
        return Response.success(summary)
    }

    override suspend fun getWorkoutHistory(): Response<List<WorkoutSummaryResponse>> {
        val workouts = readJsonArray("workouts.json")
        val list = mutableListOf<WorkoutSummaryResponse>()
        for (i in 0 until workouts.length()) {
            val w = workouts.getJSONObject(i)
            list.add(
                WorkoutSummaryResponse(
                    id = w.optString("id"),
                    title = w.optString("title"),
                    started_at = w.optString("started_at"),
                    completed_at = w.optString("completed_at"),
                    total_volume_kg = w.optDouble("total_volume_kg", 0.0).toFloat(),
                    duration_seconds = w.optInt("duration_seconds", 0),
                    total_sets = w.optInt("total_sets", 0),
                    total_reps = w.optInt("total_reps", 0)
                )
            )
        }
        // Return reverse chronological order (newest first)
        list.reverse()
        return Response.success(list)
    }

    override suspend fun getPRs(): Response<List<PrResponse>> {
        val prs = readJson("prs.json")
        val list = mutableListOf<PrResponse>()
        val keys = prs.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (isBigThree(key)) {
                val weight = prs.optDouble(key, 0.0)
                list.add(PrResponse(exercise = key, weight_kg = weight.toFloat()))
            }
        }
        return Response.success(list)
    }

    override suspend fun getActivePlan(): Response<ActivePlanResponse> {
        val plan = readJson("plan.json")
        if (plan.length() == 0) {
            // Generate a default plan
            val defaultPlan = saveDefaultPlan("hypertrophy")
            return Response.success(defaultPlan)
        }
        return Response.success(
            ActivePlanResponse(
                plan_json = plan.toString(),
                week_number = plan.optInt("week_number", 1),
                last_mutated_at = plan.optString("last_mutated_at", null),
                mutation_reason = plan.optString("mutation_reason", null)
            )
        )
    }

    override suspend fun updateActivePlan(body: UpdatePlanRequest): Response<ActivePlanResponse> {
        try {
            val planObj = JSONObject(body.plan_json)
            writeJson("plan.json", planObj)
            return Response.success(
                ActivePlanResponse(
                    plan_json = planObj.toString(),
                    week_number = planObj.optInt("week_number", 1),
                    last_mutated_at = dateFormat.format(Date()),
                    mutation_reason = "user_edit"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return Response.success(
                ActivePlanResponse(
                    plan_json = body.plan_json,
                    week_number = 1,
                    last_mutated_at = dateFormat.format(Date()),
                    mutation_reason = "error"
                )
            )
        }
    }

    override suspend fun getNudge(context: String): Response<NudgeResponse> {
        val nudges = when (context) {
            "RESTING" -> listOf(
                "Rest up. Focus on slow, deep breaths to bring your heart rate down.",
                "90 seconds rest. Visualize your next set. Perfect form is the goal.",
                "Drink some water. Prepare your mind for the next fight."
            )
            "WORKING" -> listOf(
                "Keep the intensity high! Focus on full range of motion.",
                "Brace your core. Control the eccentric phase.",
                "Don't compromise form for weight. Make every rep clean."
            )
            else -> listOf(
                "Consistency is key. Every single rep counts towards your goals.",
                "You are stronger than your excuses. Let's get to work.",
                "Traym is watching. Make today's session legendary."
            )
        }
        return Response.success(NudgeResponse(nudge = nudges.random()))
    }

    private fun saveDefaultPlan(philosophy: String): ActivePlanResponse {
        val defaultPlanJson = """
        {
          "split": "PPL-Cardio",
          "week_number": 1,
          "days": [
            {
              "day": "MON",
              "title": "PULL A (BACK & REAR DELTS)",
              "exercises": [
                {"name": "Pull-up", "sets": 3, "reps": 8},
                {"name": "Cable Seated Row", "sets": 3, "reps": 10},
                {"name": "Cable Lat Pulldown", "sets": 3, "reps": 10},
                {"name": "Dumbbell Incline Row", "sets": 3, "reps": 10},
                {"name": "Cable Face Pull", "sets": 3, "reps": 12},
                {"name": "Machine Reverse Fly", "sets": 3, "reps": 12},
                {"name": "Dumbbell Shrug", "sets": 3, "reps": 12},
                {"name": "Side Plank", "sets": 3, "reps": 1}
              ]
            },
            {
              "day": "TUE",
              "title": "PUSH A (CHEST & SHOULDERS)",
              "exercises": [
                {"name": "Push-up", "sets": 3, "reps": 12},
                {"name": "Barbell Bench Press", "sets": 3, "reps": 10},
                {"name": "Dumbbell Incline Bench Press", "sets": 3, "reps": 10},
                {"name": "Dumbbell Fly", "sets": 3, "reps": 12},
                {"name": "Dumbbell Shoulder Press", "sets": 3, "reps": 10},
                {"name": "Dumbbell Lateral Raise", "sets": 3, "reps": 12},
                {"name": "Push-up", "sets": 3, "reps": 15},
                {"name": "Hanging Leg Raise", "sets": 3, "reps": 15}
              ]
            },
            {
              "day": "WED",
              "title": "LEGS",
              "exercises": [
                {"name": "Barbell Squat", "sets": 3, "reps": 10},
                {"name": "Sled 45° Leg Press", "sets": 3, "reps": 12},
                {"name": "Lever Lying Leg Curl", "sets": 3, "reps": 12},
                {"name": "Barbell Romanian Deadlift", "sets": 3, "reps": 10},
                {"name": "Lever Leg Extension", "sets": 3, "reps": 12},
                {"name": "Hyperextension", "sets": 3, "reps": 12},
                {"name": "Barbell Standing Calf Raise", "sets": 3, "reps": 15},
                {"name": "Lever Seated Hip Abduction", "sets": 3, "reps": 15},
                {"name": "Tibialis Raise", "sets": 3, "reps": 15}
              ]
            },
            {
              "day": "THU",
              "title": "PULL B (BACK & BICEPS)",
              "exercises": [
                {"name": "Chin-up", "sets": 3, "reps": 8},
                {"name": "Dumbbell One Arm Row", "sets": 3, "reps": 10},
                {"name": "Dumbbell Pullover", "sets": 3, "reps": 12},
                {"name": "Dumbbell Shrug", "sets": 3, "reps": 12},
                {"name": "Barbell Reverse Curl", "sets": 3, "reps": 12},
                {"name": "Dumbbell Rear Lateral Raise", "sets": 3, "reps": 12},
                {"name": "Hollow Body Hold", "sets": 3, "reps": 30}
              ]
            },
            {
              "day": "FRI",
              "title": "PUSH B (CHEST & SHOULDERS VARIATION)",
              "exercises": [
                {"name": "Dumbbell Floor Press", "sets": 3, "reps": 12},
                {"name": "Barbell Close-grip Bench Press", "sets": 3, "reps": 10},
                {"name": "Cable Fly", "sets": 3, "reps": 12},
                {"name": "Barbell Standing Overhead Press", "sets": 3, "reps": 10},
                {"name": "Lever Shoulder Press", "sets": 3, "reps": 12},
                {"name": "Barbell Upright Row", "sets": 3, "reps": 12},
                {"name": "Cable Woodchop", "sets": 3, "reps": 12},
                {"name": "Neck Flexion", "sets": 3, "reps": 15},
                {"name": "Neck Extension", "sets": 3, "reps": 15}
              ]
            },
            {
              "day": "SAT",
              "title": "CARDIO + STAMINA",
              "exercises": [
                {"name": "Walk (Warm-up)", "sets": 1, "reps": 5},
                {"name": "Run (Main)", "sets": 1, "reps": 10},
                {"name": "Walk/Rest", "sets": 1, "reps": 3},
                {"name": "Run (Main)", "sets": 1, "reps": 15},
                {"name": "Incline Walk", "sets": 1, "reps": 10},
                {"name": "Walk (Cool-down)", "sets": 1, "reps": 5},
                {"name": "Stretch (Cool-down)", "sets": 1, "reps": 5}
              ]
            },
            {
              "day": "SUN",
              "title": "REST",
              "exercises": []
            }
          ]
        }
        """.trimIndent()
        
        val planObj = JSONObject(defaultPlanJson)
        planObj.put("week_number", 1)
        planObj.put("last_mutated_at", dateFormat.format(Date()))
        planObj.put("mutation_reason", "initial_onboarding")
        
        writeJson("plan.json", planObj)
        
        return ActivePlanResponse(
            plan_json = planObj.toString(),
            week_number = 1,
            last_mutated_at = planObj.optString("last_mutated_at"),
            mutation_reason = planObj.optString("mutation_reason")
        )
    }
}
