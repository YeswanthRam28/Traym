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
    val rank: Int
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
                        xp INT NOT NULL
                    )
                """)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLocalVolume(): Int {
        return try {
            val file = java.io.File(SessionManager.appContext.filesDir, "workouts.json")
            var total = 0.0
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
                        val date = format.parse(dateStr)
                        if (date != null) {
                            val c = java.util.Calendar.getInstance()
                            c.firstDayOfWeek = java.util.Calendar.MONDAY
                            c.time = date
                            if (c.get(java.util.Calendar.WEEK_OF_YEAR) == currentWeek && c.get(java.util.Calendar.YEAR) == currentYear) {
                                total += w.optDouble("total_volume_kg", 0.0)
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
            total.toInt()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun fetchLeaderboard() = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("CommunityRepo", "fetchLeaderboard started")
            val currentUserId = SessionManager.getUserId()
            val currentUserName = SessionManager.getUserName()
            val currentXp = getLocalVolume()
            android.util.Log.d("CommunityRepo", "User: $currentUserId, Name: $currentUserName, XP: $currentXp")

            val users = mutableListOf<LeaderboardUser>()
            getConnection().use { conn ->
                android.util.Log.d("CommunityRepo", "DB Connected")
                val upsertStmt = conn.prepareStatement(
                    """
                    INSERT INTO leaderboard_users (id, name, xp) 
                    VALUES (?, ?, ?) 
                    ON CONFLICT (id) 
                    DO UPDATE SET name = EXCLUDED.name, xp = EXCLUDED.xp
                    """.trimIndent()
                )
                upsertStmt.setString(1, currentUserId)
                upsertStmt.setString(2, currentUserName)
                upsertStmt.setInt(3, currentXp)
                val rows = upsertStmt.executeUpdate()
                android.util.Log.d("CommunityRepo", "Upsert executed, rows affected: $rows")

                val stmt = conn.createStatement()
                val rs = stmt.executeQuery("SELECT * FROM leaderboard_users ORDER BY xp DESC LIMIT 100")
                var rank = 1
                while (rs.next()) {
                    val u = LeaderboardUser(
                        id = rs.getString("id"),
                        name = rs.getString("name"),
                        xp = rs.getInt("xp"),
                        rank = rank++
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
}
