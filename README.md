# Traym 🏋️‍♂️

A modern, dynamic, and intuitive workout tracker built with Kotlin and Jetpack Compose. 
Traym allows you to manage your fitness journey with customizable workout splits, beautiful UI/UX, and AI-driven features.

## 🚀 Features
- **Dynamic Workout Splits**: Completely customizable drag-and-drop daily routines.
- **AI Chat Coach**: Built-in AI powered by OpenRouter for fitness advice.
- **Rich Exercise Database**: Backed by RapidAPI for comprehensive exercise instructions.
- **Community Leaderboard**: Compete globally based on weekly volume (backed by Neon PostgreSQL).
- **Notion Sync**: Seamlessly sync your workout history to a Notion database.
- **Offline & Sync Capabilities**: Robust state management syncing with your local device.
- **Web Landing Page**: A beautifully designed React + Vite landing page to distribute the app directly and guide users through setting up the Notion integration.

## 🛠️ Setup Instructions

### 1. Secrets & Environment Variables
For security, API keys are **not** committed to version control. You must create a `.env` file in the root directory (`Traym/.env`) before building the project.

Create a file named `.env` and add the following keys:
```properties
OPENROUTER_API_KEY=your_openrouter_api_key_here
RAPID_API_KEY=your_rapidapi_key_here
```

### 2. Building the Project
Once the `.env` file is created, open the project in **Android Studio**.
1. Sync Project with Gradle Files.
2. The `app/build.gradle.kts` file will automatically read your `.env` and securely inject the variables into `BuildConfig`.
3. Build and run on an Emulator or Physical Device!

## 🤝 Tech Stack
- **Kotlin & Jetpack Compose**
- **Coil** (Image Loading)
- **Vico** (Charting/Graphs)
- **Retrofit & OkHttp** (Networking)
- **Reorderable** (Drag-and-Drop functionality)
- **React, Vite & Tailwind CSS** (Web Landing Page)
