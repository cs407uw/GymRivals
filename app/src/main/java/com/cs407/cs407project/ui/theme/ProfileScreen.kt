package com.cs407.cs407project.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cs407.cs407project.data.GymRivalsCloudRepository
import com.cs407.cs407project.data.GymRivalsCloudRepository.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

@Composable
fun ProfileScreen(
    onSettingsClick: () -> Unit,

){

    // ----------------------
    // FirebaseAuth basics
    // ----------------------
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val email = firebaseUser?.email ?: "guest@gymrivals.app"

    // ----------------------
    // Firestore Profile State
    // ----------------------
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loading by remember { mutableStateOf(true) }

    // ----------------------
    // Firestore listener
    // ----------------------
    DisposableEffect(Unit) {
        var registration: ListenerRegistration? = null

        registration = GymRivalsCloudRepository.listenUserProfile { loaded ->
            profile = loaded
            loading = false
        }

        onDispose {
            registration?.remove()
        }
    }

    // ----------------------
    // Display name + initials fallback
    // ----------------------
    val displayName =
        profile?.displayName
            ?: firebaseUser?.displayName
            ?: email.substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    val initials =
        displayName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "GR" }

    val handle =
        profile?.handle ?: "@${email.substringBefore("@")}"

    val totalPoints = profile?.totalPoints ?: 0
    val achievements = profile?.achievements ?: 0
    val rivals = profile?.rivals ?: 0

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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

                IconButton(
                    onClick = onSettingsClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ----------------------
        // Top profile card
        // ----------------------
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

                if (loading) {
                    Text(
                        text = "Loading...",
                        fontSize = 16.sp,
                        color = Color(0xFF6B7280)
                    )
                } else {
                    Text(
                        text = displayName,
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
        }

        Spacer(Modifier.height(14.dp))

        // ----------------------
        // Stats (from Firestore)
        // ----------------------
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Total Points",
                value = totalPoints.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Achievements",
                value = achievements.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Rivals",
                value = rivals.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))

        // Hard-coded for now — you can later pull badges from Firestore too.
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

        // Mock goals — can later be stored per user.
        SectionCard(
            title = "Current Goals",
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            GoalRow("Bench Press 225 lbs", 0.85f, "85%")
            Spacer(Modifier.height(10.dp))
            GoalRow("Run 10 miles per week", 0.6f, "60%")
            Spacer(Modifier.height(10.dp))
            GoalRow("3 Gym Sessions / week", 0.4f, "40%")
        }

        Spacer(Modifier.height(18.dp))

        // 🔧 Account section: Settings + Logout



    }
}

@Composable
private fun AccountRow(
    title: String,
    subtitle: String,
    isDestructive: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isDestructive) Color(0xFFDC2626) else Color(0xFF111827)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
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

