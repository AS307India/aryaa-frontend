package com.as307.aryaa.ui.screens.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.AryaaShapes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    initialEmail: String?,
    onNavigateToSignup: (String) -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel
) {
    var email by remember { mutableStateOf(initialEmail ?: "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is LoginViewModel.UiState.Success) {
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AryaaColors.Navy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Wordmark
            Text(
                text = "ARYAA",
                fontFamily = FontFamily.Serif, // Mapped to Playfair Display in Theme
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = AryaaColors.White,
                letterSpacing = 4.sp
            )

            Text(
                text = "AETHERIC SECURITY",
                fontSize = 11.sp,
                color = AryaaColors.Saffron,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Input Fields Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AryaaColors.NavyCard, RoundedCornerShape(12.dp))
                    .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (uiState is LoginViewModel.UiState.Error) viewModel.resetState()
                    },
                    label = { Text("Email", color = AryaaColors.Slate) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AryaaColors.White,
                        unfocusedTextColor = AryaaColors.White,
                        focusedBorderColor = AryaaColors.Saffron,
                        unfocusedBorderColor = AryaaColors.NavyBorder,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (uiState is LoginViewModel.UiState.Error) viewModel.resetState()
                    },
                    label = { Text("Password", color = AryaaColors.Slate) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = "Toggle password visibility", tint = AryaaColors.Slate)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AryaaColors.White,
                        unfocusedTextColor = AryaaColors.White,
                        focusedBorderColor = AryaaColors.Saffron,
                        unfocusedBorderColor = AryaaColors.NavyBorder,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inline Error Banner
            if (uiState is LoginViewModel.UiState.Error) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AryaaColors.CrimsonDim, RoundedCornerShape(8.dp))
                        .border(1.dp, AryaaColors.Crimson.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = (uiState as LoginViewModel.UiState.Error).message,
                        color = AryaaColors.Crimson,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Primary CTA Button
            val isLoading = uiState is LoginViewModel.UiState.Loading
            Button(
                onClick = { viewModel.login(email, password) },
                enabled = !isLoading,
                shape = AryaaShapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AryaaColors.Saffron,
                    contentColor = AryaaColors.White,
                    disabledContainerColor = AryaaColors.Saffron.copy(alpha = 0.5f),
                    disabledContentColor = AryaaColors.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = AryaaColors.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Link
            Text(
                text = "New here? Create account",
                color = AryaaColors.Saffron,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable { onNavigateToSignup(email) }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
