package com.gymtracker.network

import com.gymtracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import com.gymtracker.auth.SessionManager

data class LeaderboardUser(
    val id: String,
    val name: String,
    val xp: Int,
    val rank: Int,
    val latestWorkoutJson: String? = null,
    val isNotionConnected: Boolean = false,
    val benchPr: Int = 0,
    val deadliftPr: Int = 0,
    val squatPr: Int = 0,
    val bodyWeight: Float = 0f,
    val liveWorkoutJson: String? = null,
    val liveUpdatedAt: java.sql.Timestamp? = null,
    val nowPlayingJson: String? = null
)

data class UserMessage(
    val id: Int,
    val targetVersion: Int?,
    val message: String
)

class CommunityRepository {

    private val _leaderboard = MutableStateFlow<List<LeaderboardUser>>(emptyList())
    val leaderboard: Flow<List<LeaderboardUser>> = _leaderboard.asStateFlow()

    private fun getConnection(): Connection {
        val uri = java.net.URI(BuildConfig.NEON_DATABASE_URL)
        val userInfo = uri.userInfo?.split(":")
        val username = userInfo?.getOrNull(0)
        val password = userInfo?.getOrNull(1)
        
        var query = uri.query ?: ""
        // Remove channel_binding if present as older JDBC drivers might not support it
        query = query.replace("&channel_binding=require", "").replace("channel_binding=require", "")
        
        val jdbcUrl = "jdbc:postgresql://${uri.host}:${if (uri.port != -1) uri.port else 5432}${uri.path}?$query"
        return DriverManager.getConnection(jdbcUrl, username, password)
    }

    suspend fun initializeDatabase() = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val stmt = conn.createStatement()
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS leaderboard_users (
                        id VARCHAR(255) PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        xp INT NOT NULL,
                        bench_pr INT DEFAULT 0,
                        deadlift_pr INT DEFAULT 0,
                        squat_pr INT DEFAULT 0,
                        body_weight FLOAT DEFAULT 0
                    )
                """)
                
                // Add columns safely if they don't exist
                try {
                    stmt.execute("ALTER TABLE leaderboard_users ADD COLUMN IF NOT EXISTS bench_pr INT DEFAULT 0")
                    stmt.execute("ALTER TABLE leaderboard_users ADD COLUMN IF NOT EXISTS deadlift_pr INT DEFAULT 0")
                    stmt.execute("ALTER TABLE leaderboard_users ADD COLUMN IF NOT EXISTS squat_pr INT DEFAULT 0")
                    stmt.execute("ALTER TABLE leaderboard_users ADD COLUMN IF NOT EXISTS body_weight FLOAT DEFAULT 0")
                    stmt.execute("ALTER TABLE leaderboard_users ADD COLUMN IF NOT EXISTS live_workout_json TEXT")
                    stmt.execute("ALTER TABLE leaderboard_users ADD COLUMN IF NOT EXISTS live_updated_at TIMESTAMP")
                    stmt.execute("ALTER TABLE leaderboard_users ADD COLUMN IF NOT EXISTS now_playing_json TEXT")
                } catch(e: Exception) {}

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS system_prompts (
                        key VARCHAR(255) PRIMARY KEY,
                        content TEXT NOT NULL,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS user_exercise_logs (
                        log_id VARCHAR(255) PRIMARY KEY,
                        user_id VARCHAR(255) NOT NULL,
                        exercise_name VARCHAR(255) NOT NULL,
                        workout_title VARCHAR(255),
                        muscle VARCHAR(255),
                        equipment VARCHAR(255),
                        weight_kg FLOAT DEFAULT 0,
                        reps INT DEFAULT 0,
                        set_number INT DEFAULT 1,
                        started_at VARCHAR(255),
                        synced_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncWorkoutsToCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            try { initializeDatabase() } catch (e: Exception) { e.printStackTrace() }
            val userId = SessionManager.getUserId()
            if (userId.isEmpty()) return@withContext Result.failure(Exception("User not logged in"))

            val file = java.io.File(SessionManager.appContext.filesDir, "workouts.json")
            if (!file.exists()) return@withContext Result.success(0)

            val jsonArray = org.json.JSONArray(file.readText())
            if (jsonArray.length() == 0) return@withContext Result.success(0)

            var count = 0
            getConnection().use { conn ->
                val sql = """
                    INSERT INTO user_exercise_logs (log_id, user_id, exercise_name, workout_title, muscle, equipment, weight_kg, reps, set_number, started_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (log_id) 
                    DO UPDATE SET 
                        exercise_name = EXCLUDED.exercise_name,
                        workout_title = EXCLUDED.workout_title,
                        muscle = EXCLUDED.muscle,
                        equipment = EXCLUDED.equipment,
                        weight_kg = EXCLUDED.weight_kg,
                        reps = EXCLUDED.reps,
                        set_number = EXCLUDED.set_number,
                        started_at = EXCLUDED.started_at
                """.trimIndent()
                val stmt = conn.prepareStatement(sql)

                for (i in 0 until jsonArray.length()) {
                    val w = jsonArray.getJSONObject(i)
                    val workoutId = w.optString("id").ifEmpty { java.util.UUID.randomUUID().toString() }
                    val exerciseName = w.optString("title", "Workout")
                    val workoutTitle = w.optString("workout_title").ifEmpty { exerciseName }
                    val muscle = w.optString("muscle", "Full Body")
                    val equipment = w.optString("equipment", "Bodyweight")
                    val startedAt = w.optString("started_at", "")

                    val sets = w.optJSONArray("sets")
                    if (sets != null && sets.length() > 0) {
                        for (j in 0 until sets.length()) {
                            val s = sets.getJSONObject(j)
                            val setNum = s.optInt("set_number", j + 1)
                            val weight = s.optDouble("weight_kg", 0.0)
                            val reps = s.optInt("reps", 0)
                            val logId = "${workoutId}_set_${setNum}"

                            stmt.setString(1, logId)
                            stmt.setString(2, userId)
                            stmt.setString(3, exerciseName)
                            stmt.setString(4, workoutTitle)
                            stmt.setString(5, muscle)
                            stmt.setString(6, equipment)
                            stmt.setDouble(7, weight)
                            stmt.setInt(8, reps)
                            stmt.setInt(9, setNum)
                            stmt.setString(10, startedAt)
                            stmt.addBatch()
                            count++
                        }
                    } else {
                        val logId = workoutId
                        stmt.setString(1, logId)
                        stmt.setString(2, userId)
                        stmt.setString(3, exerciseName)
                        stmt.setString(4, workoutTitle)
                        stmt.setString(5, muscle)
                        stmt.setString(6, equipment)
                        stmt.setDouble(7, w.optDouble("total_volume_kg", 0.0))
                        stmt.setInt(8, w.optInt("total_reps", 0))
                        stmt.setInt(9, 1)
                        stmt.setString(10, startedAt)
                        stmt.addBatch()
                        count++
                    }
                }
                stmt.executeBatch()
            }
            Result.success(count)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importWorkoutsFromCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val userId = SessionManager.getUserId()
            if (userId.isEmpty()) return@withContext Result.failure(Exception("User not logged in"))

            try { initializeDatabase() } catch (e: Exception) { e.printStackTrace() }

            val workoutsMap = mutableMapOf<String, org.json.JSONObject>()

            getConnection().use { conn ->
                val stmt = conn.prepareStatement("""
                    SELECT log_id, exercise_name, workout_title, muscle, equipment, weight_kg, reps, set_number, started_at
                    FROM user_exercise_logs
                    WHERE user_id = ?
                    ORDER BY started_at ASC, log_id ASC
                """.trimIndent())
                stmt.setString(1, userId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    val exerciseName = rs.getString("exercise_name") ?: "Workout"
                    val workoutTitle = rs.getString("workout_title") ?: exerciseName
                    val muscle = rs.getString("muscle") ?: "Full Body"
                    val equipment = rs.getString("equipment") ?: "Bodyweight"
                    val weightKg = rs.getDouble("weight_kg")
                    val reps = rs.getInt("reps")
                    val setNum = rs.getInt("set_number")
                    val startedAt = rs.getString("started_at") ?: ""

                    val groupKey = "${startedAt}_${workoutTitle}_${exerciseName}"

                    val workoutObj = workoutsMap.getOrPut(groupKey) {
                        org.json.JSONObject().apply {
                            put("id", java.util.UUID.randomUUID().toString())
                            put("title", exerciseName)
                            put("workout_title", workoutTitle)
                            put("started_at", startedAt)
                            put("completed_at", startedAt)
                            put("muscle", muscle)
                            put("equipment", equipment)
                            put("sets", org.json.JSONArray())
                            put("total_volume_kg", 0.0)
                            put("total_sets", 0)
                            put("total_reps", 0)
                        }
                    }

                    val setsArr = workoutObj.getJSONArray("sets")
                    val setObj = org.json.JSONObject().apply {
                        put("set_number", setNum)
                        put("weight_kg", weightKg)
                        put("reps", reps)
                        put("exercise_name", exerciseName)
                    }
                    setsArr.put(setObj)

                    workoutObj.put("total_volume_kg", workoutObj.optDouble("total_volume_kg", 0.0) + (weightKg * reps))
                    workoutObj.put("total_sets", setsArr.length())
                    workoutObj.put("total_reps", workoutObj.optInt("total_reps", 0) + reps)
                }
            }

            if (workoutsMap.isNotEmpty()) {
                val file = java.io.File(SessionManager.appContext.filesDir, "workouts.json")
                val jsonArray = org.json.JSONArray()
                workoutsMap.values.forEach { jsonArray.put(it) }
                file.writeText(jsonArray.toString())
            }

            Result.success(workoutsMap.size)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun getLocalStats(): LocalStatsResult {
        return try {
            val file = java.io.File(SessionManager.appContext.filesDir, "workouts.json")
            var total = 0.0
            var latestWorkoutJson: String? = null
            var latestDateStr: String? = null
            var benchPr = 0.0
            var deadliftPr = 0.0
            var squatPr = 0.0

            if (file.exists()) {
                val array = org.json.JSONArray(file.readText())
                val cal = java.util.Calendar.getInstance()
                cal.firstDayOfWeek = java.util.Calendar.MONDAY
                val currentWeek = cal.get(java.util.Calendar.WEEK_OF_YEAR)
                val currentYear = cal.get(java.util.Calendar.YEAR)
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

                for (i in 0 until array.length()) {
                    val w = array.getJSONObject(i)
                    try {
                        val dateStr = w.optString("started_at").take(10)
                        if (latestDateStr == null || dateStr > latestDateStr!!) {
                            latestDateStr = dateStr
                        }
                        
                        val date = format.parse(dateStr)
                        if (date != null) {
                            val c = java.util.Calendar.getInstance()
                            c.firstDayOfWeek = java.util.Calendar.MONDAY
                            c.time = date
                            if (c.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek && c.get(java.util.Calendar.YEAR) == currentYear) {
                                total += w.optDouble("total_volume_kg", 0.0)
                            }
                        }
                        
                        // PR calculation
                        val sets = w.optJSONArray("sets") ?: org.json.JSONArray()
                        for (j in 0 until sets.length()) {
                            val s = sets.getJSONObject(j)
                            val exName = (s.optString("exercise_name").ifEmpty { s.optString("name") }).lowercase()
                            val weight = s.optDouble("weight_kg", 0.0)

                            val isSquat = exName.contains("squat") && !exName.contains("bulgarian") && !exName.contains("split") && !exName.contains("hack") && !exName.contains("goblet") && !exName.contains("sissy")
                            val isBench = exName.contains("bench press") && !exName.contains("machine") && !exName.contains("smith") && !exName.contains("dumbbell") && !exName.contains("db")
                            val isDeadlift = exName.contains("deadlift") && !exName.contains("romanian") && !exName.contains("stiff") && !exName.contains("rdl")

                            if (isBench && weight > benchPr) benchPr = weight
                            else if (isDeadlift && weight > deadliftPr) deadliftPr = weight
                            else if (isSquat && weight > squatPr) squatPr = weight
                        }
                    } catch (e: Exception) {}
                }
                
                if (latestDateStr != null) {
                    val todaysExercises = org.json.JSONArray()
                    for (i in 0 until array.length()) {
                        val w = array.getJSONObject(i)
                        val dateStr = w.optString("started_at").take(10)
                        if (dateStr == latestDateStr) {
                            todaysExercises.put(w)
                        }
                    }
                    if (todaysExercises.length() > 0) {
                        latestWorkoutJson = todaysExercises.toString()
                    }
                }
            }
            LocalStatsResult(total.toInt(), latestWorkoutJson, benchPr.toInt(), deadliftPr.toInt(), squatPr.toInt())
        } catch (e: Exception) {
            LocalStatsResult(0, null, 0, 0, 0)
        }
    }

    private data class LocalStatsResult(val xp: Int, val latestWorkoutJson: String?, val benchPr: Int, val deadliftPr: Int, val squatPr: Int)

    suspend fun fetchLeaderboard() = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("CommunityRepo", "fetchLeaderboard started")
            val currentUserId = SessionManager.getUserId()
            val currentUserName = SessionManager.getUserName()
            val stats = getLocalStats()
            
            // Determine Notion connection status
            val prefs = SessionManager.appContext.getSharedPreferences("notion_prefs", android.content.Context.MODE_PRIVATE)
            val isNotionConnected = prefs.getString("access_token", null) != null
            
            android.util.Log.d("CommunityRepo", "User: $currentUserId, Name: $currentUserName, XP: ${stats.xp}, Notion: $isNotionConnected")

            val currentWeightStr = SessionManager.getUserWeight()
            val currentWeight = currentWeightStr.toFloatOrNull() ?: 0f

            val users = mutableListOf<LeaderboardUser>()
            getConnection().use { conn ->
                android.util.Log.d("CommunityRepo", "DB Connected")
                val upsertStmt = conn.prepareStatement(
                    """
                    INSERT INTO leaderboard_users (id, name, xp, latest_workout_json, is_notion_connected, bench_pr, deadlift_pr, squat_pr, body_weight) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) 
                    ON CONFLICT (id) 
                    DO UPDATE SET name = EXCLUDED.name, xp = EXCLUDED.xp, latest_workout_json = EXCLUDED.latest_workout_json, is_notion_connected = EXCLUDED.is_notion_connected, bench_pr = EXCLUDED.bench_pr, deadlift_pr = EXCLUDED.deadlift_pr, squat_pr = EXCLUDED.squat_pr, body_weight = EXCLUDED.body_weight
                    """.trimIndent()
                )
                upsertStmt.setString(1, currentUserId)
                upsertStmt.setString(2, currentUserName)
                upsertStmt.setInt(3, stats.xp)
                if (stats.latestWorkoutJson != null) {
                    upsertStmt.setString(4, stats.latestWorkoutJson)
                } else {
                    upsertStmt.setNull(4, java.sql.Types.VARCHAR)
                }
                upsertStmt.setBoolean(5, isNotionConnected)
                upsertStmt.setInt(6, stats.benchPr)
                upsertStmt.setInt(7, stats.deadliftPr)
                upsertStmt.setInt(8, stats.squatPr)
                upsertStmt.setFloat(9, currentWeight)
                val rows = upsertStmt.executeUpdate()
                android.util.Log.d("CommunityRepo", "Upsert executed, rows affected: $rows")

                val stmt = conn.createStatement()
                val rs = stmt.executeQuery("SELECT * FROM leaderboard_users WHERE is_banned = FALSE OR is_banned IS NULL ORDER BY xp DESC LIMIT 100")
                var rank = 1
                val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                while (rs.next()) {
                    val u = LeaderboardUser(
                        id = rs.getString("id"),
                        name = rs.getString("name"),
                        xp = rs.getInt("xp"),
                        rank = rank++,
                        latestWorkoutJson = rs.getString("latest_workout_json"),
                        isNotionConnected = rs.getBoolean("is_notion_connected"),
                        benchPr = rs.getInt("bench_pr"),
                        deadliftPr = rs.getInt("deadlift_pr"),
                        squatPr = rs.getInt("squat_pr"),
                        bodyWeight = rs.getFloat("body_weight"),
                        liveWorkoutJson = rs.getString("live_workout_json"),
                        liveUpdatedAt = rs.getTimestamp("live_updated_at", utcCal),
                        nowPlayingJson = rs.getString("now_playing_json")
                    )
                    users.add(u)
                }
                android.util.Log.d("CommunityRepo", "Fetched ${users.size} users")
            }
            _leaderboard.value = users
        } catch (e: Exception) {
            android.util.Log.e("CommunityRepo", "Error in fetchLeaderboard", e)
        }
    }

    suspend fun syncLiveWorkout(workoutJson: String?) = withContext(Dispatchers.IO) {
        try {
            val currentUserId = SessionManager.getUserId()
            getConnection().use { conn ->
                val stmt = conn.prepareStatement(
                    """
                    UPDATE leaderboard_users 
                    SET live_workout_json = ?, live_updated_at = CURRENT_TIMESTAMP 
                    WHERE id = ?
                    """.trimIndent()
                )
                if (workoutJson != null) {
                    stmt.setString(1, workoutJson)
                } else {
                    stmt.setNull(1, java.sql.Types.VARCHAR)
                }
                stmt.setString(2, currentUserId)
                stmt.executeUpdate()
            }
        } catch (e: Exception) {
            android.util.Log.e("CommunityRepo", "Error syncing live workout", e)
        }
    }

    suspend fun syncNowPlaying(trackJson: String?) = withContext(Dispatchers.IO) {
        try {
            val currentUserId = SessionManager.getUserId()
            getConnection().use { conn ->
                val stmt = conn.prepareStatement(
                    """
                    UPDATE leaderboard_users 
                    SET now_playing_json = ? 
                    WHERE id = ?
                    """.trimIndent()
                )
                if (trackJson != null) {
                    stmt.setString(1, trackJson)
                } else {
                    stmt.setNull(1, java.sql.Types.VARCHAR)
                }
                stmt.setString(2, currentUserId)
                stmt.executeUpdate()
            }
        } catch (e: Exception) {
            android.util.Log.e("CommunityRepo", "Error syncing now playing", e)
        }
    }

    suspend fun getSystemPrompt(key: String): String? = withContext(Dispatchers.IO) {
        try {
            var prompt: String? = null
            getConnection().use { conn ->
                val stmt = conn.prepareStatement("SELECT content FROM system_prompts WHERE key = ?")
                stmt.setString(1, key)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    prompt = rs.getString("content")
                }
            }
            prompt
        } catch (e: Exception) {
            android.util.Log.e("CommunityRepo", "Error fetching prompt", e)
            null
        }
    }
    
    suspend fun fetchInboxMessages(): List<UserMessage> = withContext(Dispatchers.IO) {
        try {
            val messages = mutableListOf<UserMessage>()
            val currentUserId = SessionManager.getUserId()
            getConnection().use { conn ->
                val stmt = conn.prepareStatement("SELECT id, target_version, message FROM user_messages WHERE target_user_id IS NULL OR target_user_id = ? ORDER BY id ASC")
                stmt.setString(1, currentUserId)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    messages.add(
                        UserMessage(
                            id = rs.getInt("id"),
                            targetVersion = if (rs.getObject("target_version") != null) rs.getInt("target_version") else null,
                            message = rs.getString("message")
                        )
                    )
                }
            }
            messages
        } catch (e: Exception) {
            android.util.Log.e("CommunityRepo", "Error fetching messages", e)
            emptyList()
        }
    }
}
