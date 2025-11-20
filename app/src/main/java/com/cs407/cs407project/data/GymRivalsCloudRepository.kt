package com.cs407.cs407project.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Central Firestore access for GymRivals.
 *
 * Layout:
 *   users/{uid}/meta/profile
 *   users/{uid}/runs/{runId}
 *   users/{uid}/strength_workouts/{workoutId}
 *   users/{uid}/rep_sessions/{sessionId}
 */
object GymRivalsCloudRepository {

    private const val TAG = "GymRivalsCloud"

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /** users/{uid} doc, or null if not logged in */
    private fun userDoc() =
        auth.currentUser?.let { user ->
            db.collection("users").document(user.uid)
        }

    // ------------------------------------------------------------------------
    //  PROFILE MODEL (already used by your ProfileScreen)
    // ------------------------------------------------------------------------

    data class UserProfile(
        val displayName: String = "",
        val handle: String = "",
        val totalPoints: Int = 0,
        val achievements: Int = 0,
        val rivals: Int = 0
    )

    /** Save (or overwrite) profile for the current user */
    fun saveBasicProfile(onResult: (Boolean) -> Unit = {}) {
        val firebaseUser = auth.currentUser
        val doc = userDoc()
        if (firebaseUser == null || doc == null) {
            Log.w(TAG, "saveBasicProfile: no logged-in user")
            onResult(false); return
        }

        val email = firebaseUser.email ?: "guest@gymrivals.app"
        val baseName = firebaseUser.displayName
            ?: email.substringBefore("@")
        val handle = "@${email.substringBefore("@")}"

        val profile = UserProfile(
            displayName = baseName,
            handle = handle,
            totalPoints = 0,
            achievements = 0,
            rivals = 0
        )

        doc.collection("meta")
            .document("profile")
            .set(profile)
            .addOnSuccessListener {
                Log.d(TAG, "Profile saved")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Profile save failed", e)
                onResult(false)
            }
    }

    /** Listen for profile changes; returns a registration you can remove later */
    fun listenUserProfile(onLoaded: (UserProfile?) -> Unit): ListenerRegistration? {
        val doc = userDoc()
        if (doc == null) {
            Log.w(TAG, "listenUserProfile: no logged-in user")
            onLoaded(null); return null
        }

        return doc.collection("meta")
            .document("profile")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.e(TAG, "listenUserProfile snapshot error", e)
                    onLoaded(null)
                    return@addSnapshotListener
                }
                if (snap != null && snap.exists()) {
                    val profile = snap.toObject(UserProfile::class.java)
                    onLoaded(profile)
                } else {
                    onLoaded(null)
                }
            }
    }

    // ------------------------------------------------------------------------
    //  RUNS: users/{uid}/runs/{runId}
    // ------------------------------------------------------------------------

    /** Firestore DTO for a RunEntry (needs default values for Firestore). */
    private data class FireRunEntry(
        val timestampMs: Long = 0L,
        val distanceMeters: Double = 0.0,
        val elapsedMillis: Long = 0L
    ) {
        fun toDomain(): RunEntry =
            RunEntry(
                timestampMs = timestampMs,
                distanceMeters = distanceMeters,
                elapsedMillis = elapsedMillis
            )

        companion object {
            fun from(domain: RunEntry) = FireRunEntry(
                timestampMs = domain.timestampMs,
                distanceMeters = domain.distanceMeters,
                elapsedMillis = domain.elapsedMillis
            )
        }
    }

    /** Add a run for the current user. */
    fun addRun(entry: RunEntry, onResult: (Boolean) -> Unit = {}) {
        val doc = userDoc()
        if (doc == null) {
            Log.w(TAG, "addRun: no logged-in user")
            onResult(false); return
        }

        doc.collection("runs")
            .add(FireRunEntry.from(entry))
            .addOnSuccessListener {
                Log.d(TAG, "Run added to Firestore")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "addRun failed", e)
                onResult(false)
            }
    }

    /**
     * Listen to this user's runs in Firestore and get a sorted list (oldest → newest).
     * Caller should store this in RunHistoryRepository or state.
     */
    fun listenRuns(onUpdate: (List<RunEntry>) -> Unit): ListenerRegistration? {
        val doc = userDoc()
        if (doc == null) {
            Log.w(TAG, "listenRuns: no logged-in user")
            onUpdate(emptyList()); return null
        }

        return doc.collection("runs")
            .orderBy("timestampMs")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.e(TAG, "listenRuns snapshot error", e)
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(FireRunEntry::class.java)?.toDomain() }
                    ?: emptyList()
                onUpdate(list)
            }
    }

    // ------------------------------------------------------------------------
    //  STRENGTH WORKOUTS: users/{uid}/strength_workouts/{workoutId}
    // ------------------------------------------------------------------------

    /** Firestore DTO for StrengthExercise (needs default values). */
    private data class FireStrengthExercise(
        val name: String = "",
        val sets: Int = 0,
        val reps: Int = 0,
        val restSec: Int = 0,
        val weightLbs: Int = 0
    ) {
        fun toDomain(): StrengthExercise =
            StrengthExercise(
                name = name,
                sets = sets,
                reps = reps,
                restSec = restSec,
                weightLbs = weightLbs
            )

        companion object {
            fun from(domain: StrengthExercise) = FireStrengthExercise(
                name = domain.name,
                sets = domain.sets,
                reps = domain.reps,
                restSec = domain.restSec,
                weightLbs = domain.weightLbs
            )
        }
    }

    /** Firestore DTO for StrengthWorkout. */
    private data class FireStrengthWorkout(
        val title: String = "",
        val timestampMs: Long = 0L,
        val exercises: List<FireStrengthExercise> = emptyList()
    ) {
        fun toDomain(): StrengthWorkout =
            StrengthWorkout(
                title = title,
                timestampMs = timestampMs,
                exercises = exercises.map { it.toDomain() }
            )

        companion object {
            fun from(domain: StrengthWorkout) = FireStrengthWorkout(
                title = domain.title,
                timestampMs = domain.timestampMs,
                exercises = domain.exercises.map { FireStrengthExercise.from(it) }
            )
        }
    }

    /** Add a strength workout for the current user. */
    fun addStrengthWorkout(workout: StrengthWorkout, onResult: (Boolean) -> Unit = {}) {
        val doc = userDoc()
        if (doc == null) {
            Log.w(TAG, "addStrengthWorkout: no logged-in user")
            onResult(false); return
        }

        doc.collection("strength_workouts")
            .add(FireStrengthWorkout.from(workout))
            .addOnSuccessListener {
                Log.d(TAG, "Strength workout added to Firestore")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "addStrengthWorkout failed", e)
                onResult(false)
            }
    }

    /** Listen to this user's strength workouts. */
    fun listenStrengthWorkouts(onUpdate: (List<StrengthWorkout>) -> Unit): ListenerRegistration? {
        val doc = userDoc()
        if (doc == null) {
            Log.w(TAG, "listenStrengthWorkouts: no logged-in user")
            onUpdate(emptyList()); return null
        }

        return doc.collection("strength_workouts")
            .orderBy("timestampMs")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.e(TAG, "listenStrengthWorkouts snapshot error", e)
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(FireStrengthWorkout::class.java)?.toDomain() }
                    ?: emptyList()
                onUpdate(list)
            }
    }

    // ------------------------------------------------------------------------
    //  REP SESSIONS: users/{uid}/rep_sessions/{sessionId}
    // ------------------------------------------------------------------------

    /** Firestore DTO for RepSession. */
    private data class FireRepSession(
        val exerciseType: String = "",
        val totalReps: Int = 0,
        val timestampMs: Long = 0L,
        val durationSeconds: Int = 0
    ) {
        fun toDomain(): RepSession =
            RepSession(
                exerciseType = exerciseType,
                totalReps = totalReps,
                timestampMs = timestampMs,
                durationSeconds = durationSeconds
            )

        companion object {
            fun from(domain: RepSession) = FireRepSession(
                exerciseType = domain.exerciseType,
                totalReps = domain.totalReps,
                timestampMs = domain.timestampMs,
                durationSeconds = domain.durationSeconds
            )
        }
    }
    

    /** Add a rep-counting session for the current user. */
    fun addRepSession(session: RepSession, onResult: (Boolean) -> Unit = {}) {
        val doc = userDoc()
        if (doc == null) {
            Log.w(TAG, "addRepSession: no logged-in user")
            onResult(false); return
        }

        doc.collection("rep_sessions")
            .add(FireRepSession.from(session))
            .addOnSuccessListener {
                Log.d(TAG, "RepSession added to Firestore")
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "addRepSession failed", e)
                onResult(false)
            }
    }

    /** Listen to this user's rep sessions. */
    fun listenRepSessions(onUpdate: (List<RepSession>) -> Unit): ListenerRegistration? {
        val doc = userDoc()
        if (doc == null) {
            Log.w(TAG, "listenRepSessions: no logged-in user")
            onUpdate(emptyList()); return null
        }

        return doc.collection("rep_sessions")
            .orderBy("timestampMs")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    Log.e(TAG, "listenRepSessions snapshot error", e)
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toObject(FireRepSession::class.java)?.toDomain() }
                    ?: emptyList()
                onUpdate(list)
            }
    }
}
