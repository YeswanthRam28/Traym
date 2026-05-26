package com.gymtracker.network

import retrofit2.Response
import retrofit2.http.*

interface TraymApiService {
    
    // User endpoints
    @POST("users/sync")
    suspend fun syncUser(@Body body: SyncUserRequest): Response<UserProfileResponse>

    @POST("users/onboarding")
    suspend fun completeOnboarding(@Body body: OnboardingRequest): Response<UserProfileResponse>

    @GET("users/me")
    suspend fun getMe(): Response<UserProfileResponse>

    @Multipart
    @POST("users/upload-chatgpt-export")
    suspend fun uploadChatGPTExport(
        @Part file: okhttp3.MultipartBody.Part
    ): Response<Unit>

    // Workout endpoints
    @POST("workouts/start")
    suspend fun startWorkout(@Body body: StartWorkoutRequest): Response<WorkoutSummaryResponse>

    @POST("workouts/{workout_id}/sets")
    suspend fun logSets(
        @Path("workout_id") workoutId: String,
        @Body body: LogSetsRequest
    ): Response<LogSetsResponse>

    @POST("workouts/{workout_id}/complete")
    suspend fun completeWorkout(
        @Path("workout_id") workoutId: String,
        @Body body: CompleteWorkoutRequest
    ): Response<WorkoutSummaryResponse>

    @POST("workouts/inline_log")
    suspend fun logInlineWorkout(@Body body: InlineLogRequest): Response<WorkoutSummaryResponse>

    @GET("workouts/history")
    suspend fun getWorkoutHistory(): Response<List<WorkoutSummaryResponse>>

    @GET("workouts/prs")
    suspend fun getPRs(): Response<List<PrResponse>>

    @GET("workouts/plan")
    suspend fun getActivePlan(): Response<ActivePlanResponse>

    @POST("workouts/plan/update")
    suspend fun updateActivePlan(@Body body: UpdatePlanRequest): Response<ActivePlanResponse>

    @GET("coach/nudge")
    suspend fun getNudge(@Query("context") context: String): Response<NudgeResponse>
}

// ── Models ────────────────────────────────────────────────────────────────────

data class SyncUserRequest(val email: String?, val name: String?)

data class OnboardingRequest(
    val goal: String,
    val experience_years: Int,
    val equipment: List<String>,
    val philosophy: String
)

data class UserProfileResponse(
    val id: String,
    val clerk_user_id: String,
    val name: String?,
    val email: String?,
    val goal: String?,
    val philosophy: String?,
    val onboarding_complete: Boolean,
    val chatgpt_import_processed: Boolean
)

data class StartWorkoutRequest(val title: String)

data class LogSetsRequest(val sets: List<SetLog>)
data class SetLog(
    val exercise_name: String,
    val set_number: Int,
    val weight_kg: Float?,
    val reps: Int?,
    val rpe: Float?,
    val rest_seconds: Int?
)

data class LogSetsResponse(val logged: Int)

data class CompleteWorkoutRequest(val notes: String?)

data class InlineLogRequest(
    val dayTitle: String,
    val exerciseName: String,
    val sets: List<SetLog>
)

data class WorkoutSummaryResponse(
    val id: String,
    val title: String,
    val started_at: String,
    val completed_at: String?,
    val total_volume_kg: Float?,
    val duration_seconds: Int?,
    val total_sets: Int? = null,
    val total_reps: Int? = null
)

data class PrResponse(
    val exercise: String,
    val weight_kg: Float
)

data class ActivePlanResponse(
    val plan_json: String,
    val week_number: Int,
    val last_mutated_at: String?,
    val mutation_reason: String?
)

data class UpdatePlanRequest(
    val plan_json: String
)

data class NudgeResponse(val nudge: String)
