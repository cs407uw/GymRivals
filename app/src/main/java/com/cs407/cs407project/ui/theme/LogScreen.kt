package com.cs407.cs407project.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogScreen(
    onTrackRun: () -> Unit,
    onAddStrength: () -> Unit,
    onRepCounter: () -> Unit
) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED)))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(Color(0xFFF6F7FB))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                Text("Log", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Start a new activity", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Track a Run
        ActionCard(
            title = "Track a Run",
            desc = "Use GPS to capture pace (min/mi), distance, and time."
        ) { PrimaryGradientButton("Start Run", onClick = onTrackRun) }

        Spacer(Modifier.height(12.dp))

        // Add Strength Workout
        ActionCard(
            title = "Add Strength Workout",
            desc = "Build a workout by choosing exercises, sets, reps, and rest."
        ) { PrimaryGradientButton("Add Strength Workout", onClick = onAddStrength) }

        Spacer(Modifier.height(12.dp))

        // Rep Counter
        ActionCard(
            title = "Rep Counter",
            desc = "Use your camera to automatically count push-ups and squats with AI."
        ) { PrimaryGradientButton("Start Rep Counter", onClick = onRepCounter) }
    }
}

@Composable
private fun ActionCard(title: String, desc: String, trailing: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
            Text(desc, color = Color(0xFF6B7280))
            trailing()
        }
    }
}


@Composable
private fun PrimaryGradientButton(text: String, onClick: () -> Unit) {
    val brush = Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(brush, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp)
            .wrapContentHeight(Alignment.CenterVertically)
            .noRippleClickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// noRippleClickable helper (inline so no extra imports needed)

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() })