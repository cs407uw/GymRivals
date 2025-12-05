package com.cs407.cs407project.ui.repcounter

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.cs407project.repcounter.ExerciseType
import com.cs407.cs407project.viewmodel.RepCounterViewModel
import com.cs407.cs407project.viewmodel.SessionSummary

/**
 * Main screen for rep counting with camera and pose detection
 *
 * Features:
 * - Live camera preview
 * - Exercise type selector (Push-ups / Squats)
 * - Real-time rep count display
 * - Session timer
 * - Start/Pause/Resume/Stop controls
 *
 * @param onBack Callback when back button is pressed
 * @param viewModel The RepCounterViewModel (automatically injected)
 */
@Composable
fun RepCounterScreen(
    onBack: () -> Unit,
    viewModel: RepCounterViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera preview view - must be created before permission launcher
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setCameraPermission(isGranted)
        if (isGranted) {
            // Initialize camera after permission is granted
            viewModel.initializeCamera(lifecycleOwner, previewView)
        }
    }

    // Check and request camera permission on first launch
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.setCameraPermission(true)
            viewModel.initializeCamera(lifecycleOwner, previewView)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Camera preview
        if (state.permissionGranted) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Dark overlay for better text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            // Pose visualization overlay
            if (state.isRunning && state.currentPose != null) {
                PoseOverlay(
                    pose = state.currentPose!!,
                    exerciseType = state.exerciseType
                )
            }
        } else {
            // Permission not granted - show message
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📷",
                        fontSize = 64.sp
                    )
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Allow camera access to count reps",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    GradientButton(
                        text = "Grant Permission",
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }
            }
        }

        // UI Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Timer
                if (state.isRunning) {
                    Text(
                        text = formatTime(state.elapsedSeconds),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Exercise Type Selector (only when not running)
            if (!state.isRunning) {
                ExerciseTypeSelector(
                    selectedType = state.exerciseType,
                    onTypeSelected = { viewModel.setExerciseType(it) }
                )
                Spacer(Modifier.height(24.dp))
            }

            // Rep Count Display
            RepCountDisplay(
                count = state.repCount,
                exerciseType = state.exerciseType
            )

            Spacer(Modifier.height(24.dp))

            // Control Buttons
            ControlButtons(
                isRunning = state.isRunning,
                isPaused = state.isPaused,
                permissionGranted = state.permissionGranted,
                onStart = { viewModel.startCounting() },
                onPause = { viewModel.pauseCounting() },
                onResume = { viewModel.resumeCounting() },
                onStop = { viewModel.stopCounting() }
            )

            Spacer(Modifier.height(16.dp))
        }

        // Error message
        state.cameraError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            }
        }
    }

    // Session summary dialog
    state.sessionSummary?.let { summary ->
        SessionSummaryDialog(
            summary = summary,
            onDismiss = {
                viewModel.dismissSummary()
                onBack()
            }
        )
    }
}

/**
 * Exercise type selector with radio buttons
 */
@Composable
private fun ExerciseTypeSelector(
    selectedType: ExerciseType,
    onTypeSelected: (ExerciseType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Select Exercise",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExerciseTypeButton(
                text = "Push-ups",
                emoji = "💪",
                isSelected = selectedType == ExerciseType.PUSH_UP,
                onClick = { onTypeSelected(ExerciseType.PUSH_UP) },
                modifier = Modifier.weight(1f)
            )

            ExerciseTypeButton(
                text = "Squats",
                emoji = "🦵",
                isSelected = selectedType == ExerciseType.SQUAT,
                onClick = { onTypeSelected(ExerciseType.SQUAT) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Exercise type button with emoji
 */
@Composable
private fun ExerciseTypeButton(
    text: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        Color(0xFF3B82F6)
    } else {
        Color.White.copy(alpha = 0.1f)
    }

    val borderColor = if (isSelected) {
        Color(0xFF3B82F6)
    } else {
        Color.White.copy(alpha = 0.3f)
    }

    Box(
        modifier = modifier
            .height(80.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Large rep count display
 */
@Composable
private fun RepCountDisplay(
    count: Int,
    exerciseType: ExerciseType
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            color = Color.White,
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = when (exerciseType) {
                ExerciseType.PUSH_UP -> "Push-ups"
                ExerciseType.SQUAT -> "Squats"
            },
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Control buttons for starting/stopping counting
 */
@Composable
private fun ControlButtons(
    isRunning: Boolean,
    isPaused: Boolean,
    permissionGranted: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    when {
        !isRunning -> {
            // Start button
            GradientButton(
                text = "Start Counting",
                onClick = onStart,
                enabled = permissionGranted,
                modifier = Modifier.fillMaxWidth()
            )
        }
        isPaused -> {
            // Resume and Stop buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlineButton(
                    text = "Stop",
                    onClick = onStop,
                    modifier = Modifier.weight(1f)
                )
                GradientButton(
                    text = "Resume",
                    onClick = onResume,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        else -> {
            // Pause and Stop buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlineButton(
                    text = "Stop",
                    onClick = onStop,
                    modifier = Modifier.weight(1f)
                )
                GradientButton(
                    text = "Pause",
                    onClick = onPause,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Gradient button component
 */
@Composable
private fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val brush = if (enabled) {
        Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6)))
    } else {
        Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray))
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .background(brush, RoundedCornerShape(14.dp))
            .then(
                if (enabled) {
                    Modifier.noRippleClickable(onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Outline button component
 */
@Composable
private fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .noRippleClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * No-ripple clickable modifier
 */
@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    )

/**
 * Formats elapsed seconds into MM:SS format
 */
private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

/**
 * Pose visualization overlay that draws skeleton and landmarks
 *
 * Highlights important body parts based on exercise type:
 * - Push-ups: Arms and shoulders
 * - Squats: Legs and hips
 */
@Composable
private fun PoseOverlay(
    pose: Pose,
    exerciseType: ExerciseType
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Helper function to get landmark position
        fun getLandmarkPosition(landmarkType: Int): Offset? {
            val landmark = pose.getPoseLandmark(landmarkType) ?: return null
            // ML Kit returns normalized coordinates, need to scale to screen size
            return Offset(
                x = landmark.position.x,
                y = landmark.position.y
            )
        }

        // Helper function to draw a line between two landmarks
        fun drawConnection(from: Int, to: Int, color: Color, strokeWidth: Float = 8f) {
            val start = getLandmarkPosition(from)
            val end = getLandmarkPosition(to)
            if (start != null && end != null) {
                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // Helper function to draw a landmark point
        fun drawLandmark(landmarkType: Int, color: Color, radius: Float = 12f) {
            val position = getLandmarkPosition(landmarkType)
            if (position != null) {
                drawCircle(
                    color = color,
                    radius = radius,
                    center = position
                )
            }
        }

        // Define colors for different body parts
        val primaryColor = Color(0xFF3B82F6)  // Blue
        val highlightColor = Color(0xFFFFD700)  // Gold for important parts
        val secondaryColor = Color(0xFF10B981)  // Green

        // Determine which parts to highlight based on exercise
        val shouldHighlightArms = exerciseType == ExerciseType.PUSH_UP
        val shouldHighlightLegs = exerciseType == ExerciseType.SQUAT

        val armColor = if (shouldHighlightArms) highlightColor else primaryColor
        val legColor = if (shouldHighlightLegs) highlightColor else primaryColor

        // Draw skeleton connections
        // Arms
        drawConnection(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, armColor, 10f)
        drawConnection(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, armColor, 10f)
        drawConnection(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, armColor, 10f)
        drawConnection(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, armColor, 10f)

        // Torso
        drawConnection(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, secondaryColor)
        drawConnection(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, secondaryColor)
        drawConnection(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, secondaryColor)
        drawConnection(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, secondaryColor)

        // Legs
        drawConnection(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, legColor, 10f)
        drawConnection(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE, legColor, 10f)
        drawConnection(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, legColor, 10f)
        drawConnection(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, legColor, 10f)

        // Draw landmark points
        val landmarks = listOf(
            // Arms
            PoseLandmark.LEFT_SHOULDER to armColor,
            PoseLandmark.LEFT_ELBOW to armColor,
            PoseLandmark.LEFT_WRIST to armColor,
            PoseLandmark.RIGHT_SHOULDER to armColor,
            PoseLandmark.RIGHT_ELBOW to armColor,
            PoseLandmark.RIGHT_WRIST to armColor,
            // Torso
            PoseLandmark.LEFT_HIP to secondaryColor,
            PoseLandmark.RIGHT_HIP to secondaryColor,
            // Legs
            PoseLandmark.LEFT_KNEE to legColor,
            PoseLandmark.LEFT_ANKLE to legColor,
            PoseLandmark.RIGHT_KNEE to legColor,
            PoseLandmark.RIGHT_ANKLE to legColor
        )

        landmarks.forEach { (landmarkType, color) ->
            drawLandmark(landmarkType, color)
        }
    }
}

/**
 * Session summary dialog shown after completing a rep counting session
 *
 * Displays:
 * - Total reps completed
 * - Total time
 * - Calories burned
 */
@Composable
private fun SessionSummaryDialog(
    summary: SessionSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold)
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎉",
                    fontSize = 48.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Session Complete!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = summary.exerciseName,
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                // Rep count
                SummaryItem(
                    icon = "💪",
                    label = "Reps",
                    value = "${summary.reps}"
                )

                // Time
                SummaryItem(
                    icon = "⏱️",
                    label = "Time",
                    value = formatTime(summary.timeSeconds)
                )

                // Calories
                SummaryItem(
                    icon = "🔥",
                    label = "Calories",
                    value = "${summary.calories} cal"
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Summary item component for the dialog
 */
@Composable
private fun SummaryItem(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp)
            Text(
                text = label,
                fontSize = 16.sp,
                color = Color(0xFF6B7280)
            )
        }
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
    }
}