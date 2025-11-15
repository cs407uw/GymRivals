package com.cs407.cs407project.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents a single rep-counting session
 *
 * @property exerciseType The type of exercise performed (e.g., "Push-ups", "Squats")
 * @property totalReps Total number of reps counted during the session
 * @property timestampMs Timestamp when the session was completed (Unix epoch milliseconds)
 * @property durationSeconds Duration of the session in seconds
 */
data class RepSession(
    val exerciseType: String,
    val totalReps: Int,
    val timestampMs: Long,
    val durationSeconds: Int
)

/**
 * Repository for managing rep counting sessions
 *
 * Stores all rep-counting sessions in memory using StateFlow for reactive updates.
 * Data is stored as an in-memory list and is lost when the app is closed.
 */
object RepCountRepository {
    private val _sessions = MutableStateFlow<List<RepSession>>(emptyList())
    val sessions: StateFlow<List<RepSession>> = _sessions

    /**
     * Adds a new rep session to the repository
     *
     * @param session The RepSession to add
     */
    fun add(session: RepSession) {
        _sessions.value = _sessions.value + session
    }

    /**
     * Gets all sessions for a specific exercise type
     *
     * @param exerciseType The exercise type to filter by
     * @return List of RepSessions for the specified exercise type
     */
    fun getSessionsByType(exerciseType: String): List<RepSession> {
        return _sessions.value.filter { it.exerciseType == exerciseType }
    }

    /**
     * Gets the last N sessions for a specific exercise type
     *
     * @param exerciseType The exercise type to filter by
     * @param count The number of recent sessions to return
     * @return List of the most recent RepSessions for the specified exercise type
     */
    fun getRecentSessions(exerciseType: String, count: Int): List<RepSession> {
        return _sessions.value
            .filter { it.exerciseType == exerciseType }
            .sortedByDescending { it.timestampMs }
            .take(count)
    }
}
