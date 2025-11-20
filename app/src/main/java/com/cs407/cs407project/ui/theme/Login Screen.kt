package com.example.gymrivals.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymRivalsLoginScreen(
    onLogin: (email: String, password: String, rememberMe: Boolean) -> Unit = { _, _, _ -> },
    onGoogleLogin: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onSignUp: () -> Unit = {},
) {
    // ---------------------------
    // MUST be inside the composable
    // ---------------------------
    val auth = remember { FirebaseAuth.getInstance() }

    val gradient = Brush.verticalGradient(
        0f to Color(0xFF0EA5E9),
        1f to Color(0xFF7C3AED)
    )

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focus = LocalFocusManager.current

    // ---------------------------
    // Firebase Sign-in / Sign-up
    // ---------------------------
    fun handleAuth(isSignUp: Boolean) {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || password.isBlank()) {
            errorMessage = "Please enter both email and password."
            return
        }

        isLoading = true
        errorMessage = null

        val task =
            if (isSignUp)
                auth.createUserWithEmailAndPassword(trimmedEmail, password)
            else
                auth.signInWithEmailAndPassword(trimmedEmail, password)

        task.addOnCompleteListener { result ->
            isLoading = false
            if (result.isSuccessful) {
                onLogin(trimmedEmail, password, rememberMe)
            } else {
                val ex = result.exception
                val code = (ex as? FirebaseAuthException)?.errorCode

                // Log full details to Logcat
                Log.e("Auth", "Auth failed: code=$code, message=${ex?.message}", ex)

                errorMessage = when (code) {
                    "ERROR_NETWORK_REQUEST_FAILED" ->
                        "Network error: please check your internet connection."
                    "ERROR_OPERATION_NOT_ALLOWED" ->
                        "Email/password sign-in is disabled in Firebase."
                    "ERROR_APP_NOT_AUTHORIZED" ->
                        "This app is not authorized to use Firebase Authentication. Check your google-services.json and package name."
                    "ERROR_INVALID_API_KEY" ->
                        "Invalid API key in Firebase config."
                    else ->
                        ex?.localizedMessage ?: "Authentication failed."
                }
            }
        }
    }

    // ---------------------------
    // UI
    // ---------------------------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GR",
                    color = Color(0xFF0EA5E9),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(12.dp))

            Text("GymRivals", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("Track. Compete. Dominate.", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)

            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Welcome Back", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

                    Spacer(Modifier.height(18.dp))

                    // ---------- Email ----------
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    // ---------- Password ----------
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focus.clearFocus()
                                handleAuth(false)
                            }
                        ),
                        trailingIcon = {
                            TextButton(onClick = { showPassword = !showPassword }) {
                                Text(if (showPassword) "Hide" else "Show", fontSize = 12.sp)
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                        Text("Remember me")

                        Spacer(Modifier.weight(1f))

                        Text(
                            "Forgot Password?",
                            color = Color(0xFF6366F1),
                            modifier = Modifier.clickable { onForgotPassword() },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // ---------- Error message ----------
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // ---------- Login Button ----------
                    val buttonBrush = Brush.horizontalGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(buttonBrush)
                            .clickable(enabled = !isLoading) {
                                focus.clearFocus()
                                handleAuth(false)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading)
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        else
                            Text("Log In", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(16.dp))

                    // ---------- Google login ----------
                    OutlinedButton(
                        onClick = { if (!isLoading) onGoogleLogin() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(22.dp).clip(CircleShape).background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("Google")
                    }

                    Spacer(Modifier.height(14.dp))

                    // ---------- Sign Up ----------
                    Row {
                        Text("Don't have an account? ")
                        Text(
                            "Sign Up",
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable(enabled = !isLoading) {
                                focus.clearFocus()
                                handleAuth(true)   // Firebase sign-up
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            val footer: AnnotatedString = buildAnnotatedString {
                append("By continuing, you agree to our ")
                pushStyle(SpanStyle(color = Color.White, textDecoration = TextDecoration.Underline))
                append("Terms of Service")
                pop()
                append(" and ")
                pushStyle(SpanStyle(color = Color.White, textDecoration = TextDecoration.Underline))
                append("Privacy Policy")
                pop()
            }
            Text(
                text = footer,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewGymRivalsLoginScreen() {
    MaterialTheme {
        GymRivalsLoginScreen()
    }
}