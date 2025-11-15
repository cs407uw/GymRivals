package com.cs407.cs407project.viewmodel

import android.app.Application
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.cs407.cs407project.data.RepCountRepository
import com.cs407.cs407project.data.RepSession
import com.cs407.cs407project.repcounter.ExerciseType
import com.cs407.cs407project.repcounter.RepCounter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * UI state for the Rep Counter screen
 *
 * @property isRunning Whether the rep counting session is active
 * @property isPaused Whether the session is paused
 * @property repCount Current number of reps counted
 * @property exerciseType Current exercise type being counted
 * @property elapsedSeconds Time elapsed since session start (in seconds)
 * @property cameraError Error message if camera initialization fails
 * @property permissionGranted Whether camera permission has been granted
 */
data class RepCounterUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val repCount: Int = 0,
    val exerciseType: ExerciseType = ExerciseType.PUSH_UP,
    val elapsedSeconds: Int = 0,
    val cameraError: String? = null,
    val permissionGranted: Boolean = false
)

/**
 * ViewModel for managing rep counting with ML Kit Pose Detection
 *
 * This ViewModel handles:
 * - Camera lifecycle and frame processing
 * - ML Kit Pose Detection setup
 * - Rep counting logic via RepCounter
 * - Timer for session duration
 * - Saving sessions to RepCountRepository
 *
 * ## Usage:
 * 1. Call `setCameraPermission(true)` after camera permission is granted
 * 2. Call `initializeCamera(lifecycleOwner, previewView)` to start camera
 * 3. Call `setExerciseType()` to choose push-ups or squats
 * 4. Call `startCounting()` to begin counting reps
 * 5. Call `stopCounting()` to end session and save results
 */
class RepCounterViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "RepCounterViewModel"
    }

    // UI State
    private val _state = MutableStateFlow(RepCounterUiState())
    val state = _state.asStateFlow()

    // ML Kit Pose Detector
    private var poseDetector: PoseDetector? = null

    // Rep counting logic
    private var repCounter: RepCounter? = null

    // Camera components
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService? = null

    // Timer tracking
    private var sessionStartTime: Long = 0L
    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        // Initialize ML Kit Pose Detector with accurate model
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()

        poseDetector = PoseDetection.getClient(options)
        cameraExecutor = Executors.newSingleThreadExecutor()

        Log.d(TAG, "RepCounterViewModel initialized")
    }

    /**
     * Sets whether camera permission has been granted
     *
     * @param granted True if permission is granted
     */
    fun setCameraPermission(granted: Boolean) {
        _state.value = _state.value.copy(permissionGranted = granted)
        if (!granted) {
            _state.value = _state.value.copy(cameraError = "Camera permission required")
        }
    }

    /**
     * Sets the exercise type to count
     *
     * @param type The exercise type (PUSH_UP or SQUAT)
     */
    fun setExerciseType(type: ExerciseType) {
        if (_state.value.isRunning) {
            Log.w(TAG, "Cannot change exercise type while counting")
            return
        }

        _state.value = _state.value.copy(exerciseType = type)
        Log.d(TAG, "Exercise type set to: $type")
    }

    /**
     * Initializes the camera and starts preview
     *
     * Must be called after camera permission is granted and before starting counting.
     *
     * @param lifecycleOwner The lifecycle owner (usually the Activity)
     * @param previewView The PreviewView to display camera feed
     */
    fun initializeCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // Setup preview
                val preview = androidx.camera.core.Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Setup image analysis for pose detection
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor!!) { imageProxy ->
                            processImageProxy(imageProxy)
                        }
                    }

                // Select front camera
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                // Unbind all use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                Log.d(TAG, "Camera initialized successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                _state.value = _state.value.copy(cameraError = "Camera error: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    /**
     * Starts the rep counting session
     */
    fun startCounting() {
        if (_state.value.isRunning) {
            Log.w(TAG, "Already counting")
            return
        }

        // Initialize rep counter with current exercise type
        repCounter = RepCounter(_state.value.exerciseType) { newCount ->
            _state.value = _state.value.copy(repCount = newCount)
        }

        // Reset state
        sessionStartTime = System.currentTimeMillis()
        _state.value = _state.value.copy(
            isRunning = true,
            isPaused = false,
            repCount = 0,
            elapsedSeconds = 0
        )

        // Start timer
        startTimer()

        Log.d(TAG, "Started counting ${_state.value.exerciseType}")
    }

    /**
     * Pauses the rep counting session
     */
    fun pauseCounting() {
        if (!_state.value.isRunning || _state.value.isPaused) return

        _state.value = _state.value.copy(isPaused = true)
        Log.d(TAG, "Counting paused")
    }

    /**
     * Resumes the rep counting session
     */
    fun resumeCounting() {
        if (!_state.value.isRunning || !_state.value.isPaused) return

        _state.value = _state.value.copy(isPaused = false)
        Log.d(TAG, "Counting resumed")
    }

    /**
     * Stops the rep counting session and saves the results
     */
    fun stopCounting() {
        if (!_state.value.isRunning) return

        stopTimer()

        // Save session to repository
        val session = RepSession(
            exerciseType = _state.value.exerciseType.name.replace("_", " ")
                .lowercase()
                .replaceFirstChar { it.uppercase() },
            totalReps = _state.value.repCount,
            timestampMs = System.currentTimeMillis(),
            durationSeconds = _state.value.elapsedSeconds
        )

        RepCountRepository.add(session)

        _state.value = _state.value.copy(isRunning = false, isPaused = false)

        Log.d(TAG, "Stopped counting. Session saved: ${session.totalReps} reps in ${session.durationSeconds}s")
    }

    /**
     * Processes a camera frame for pose detection
     *
     * Called by CameraX ImageAnalysis for each frame.
     * Converts the frame to ML Kit InputImage, runs pose detection,
     * and passes the result to RepCounter.
     *
     * @param imageProxy The camera frame from CameraX
     */
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // Skip processing if not actively counting
        if (!_state.value.isRunning || _state.value.isPaused) {
            imageProxy.close()
            return
        }

        // Convert to ML Kit InputImage
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        // Run pose detection
        poseDetector?.process(inputImage)
            ?.addOnSuccessListener { pose ->
                // Pass pose to rep counter
                repCounter?.processPose(pose)
            }
            ?.addOnFailureListener { e ->
                Log.e(TAG, "Pose detection failed", e)
            }
            ?.addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * Starts the session timer
     *
     * Updates elapsed time every second while the session is running.
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.isRunning) {
                if (!_state.value.isPaused) {
                    val elapsed = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
                    _state.value = _state.value.copy(elapsedSeconds = elapsed)
                }
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    /**
     * Stops the session timer
     */
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Cleans up resources when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()

        stopTimer()
        cameraProvider?.unbindAll()
        poseDetector?.close()
        cameraExecutor?.shutdown()

        Log.d(TAG, "RepCounterViewModel cleared")
    }
}
