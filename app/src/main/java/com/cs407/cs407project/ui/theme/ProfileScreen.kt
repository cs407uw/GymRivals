package com.cs407.cs407project.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen() {
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email ?: "guest@gymrivals.app"
    val baseName = user?.displayName
        ?: email.substringBefore("@")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    val initials = baseName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "GR" }

    val handle = "@${email.substringBefore("@")}"

    val headerGradient = Brush.horizontalGradient(
        listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED))
    )

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
                .background(headerGradient)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column {
                Text(
                    text = "GymRivals",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Track. Compete. Dominate.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Top profile card
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val avatarGradient = Brush.verticalGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFF6366F1))
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(avatarGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = baseName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
                Text(
                    text = handle,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Stats row
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Total Points",
                value = "12,450",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Achievements",
                value = "23",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Rivals",
                value = "15",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))

        // Recent Badges
        SectionCard(
            title = "Recent Badges",
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BadgeBubble("🔥")
                    BadgeBubble("💪")
                    BadgeBubble("⚡")
                    BadgeBubble("🏅")
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BadgeBubble("📈")
                    BadgeBubble("🏆")
                    BadgeBubble("⭐")
                    BadgeBubble("⏱")
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Current Goals
        SectionCard(
            title = "Current Goals",
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            GoalRow(
                title = "Bench Press 225 lbs",
                progress = 0.85f,
                label = "85%"
            )
            Spacer(Modifier.height(10.dp))
            GoalRow(
                title = "Run 10 miles per week",
                progress = 0.6f,
                label = "60%"
            )
            Spacer(Modifier.height(10.dp))
            GoalRow(
                title = "3 Gym Sessions / week",
                progress = 0.4f,
                label = "40%"
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827)
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun BadgeBubble(emoji: String) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF3F4FF)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GoalRow(title: String, progress: Float, label: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF111827)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = Color(0xFF6366F1),
            trackColor = Color(0xFFE5E7EB)
        )
    }
}
