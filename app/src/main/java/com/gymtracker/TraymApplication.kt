package com.gymtracker

import android.app.Application
import com.gymtracker.auth.SessionManager
import org.json.JSONArray
import java.io.File

class TraymApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        cleanLocalDuplicates()
    }

    private fun cleanLocalDuplicates() {
        try {
            val file = File(filesDir, "workouts.json")
            if (!file.exists()) return
            
            val workouts = JSONArray(file.readText())
            val deduplicated = JSONArray()
            val seen = mutableSetOf<String>()
            
            for (i in 0 until workouts.length()) {
                val w = workouts.getJSONObject(i)
                val title = w.optString("title")
                val date = w.optString("started_at").take(10)
                
                // The app design groups all sets for an exercise per day into a single object
                val key = "$title-$date"
                if (seen.add(key)) {
                    deduplicated.put(w)
                }
            }
            
            if (deduplicated.length() < workouts.length()) {
                file.writeText(deduplicated.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
