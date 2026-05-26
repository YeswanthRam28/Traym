package com.gymtracker.network

object NetworkConfig {
    /**
     * Change this to switch between environments.
     */
    var currentEnvironment = Environment.NGROK

    enum class Environment(val baseUrl: String, val wsUrl: String) {
        LOCAL(
            baseUrl = "http://10.0.2.2:8000/", // Emulator loopback
            wsUrl = "ws://10.0.2.2:8000/"
        ),
        NGROK(
            baseUrl = "https://intrusive-margit-multiovulated.ngrok-free.dev/",
            wsUrl = "wss://intrusive-margit-multiovulated.ngrok-free.dev/"
        )
    }

    val BASE_URL: String
        get() = currentEnvironment.baseUrl

    val WS_URL: String
        get() = currentEnvironment.wsUrl
}
