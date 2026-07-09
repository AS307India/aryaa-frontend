package com.as307.aryaa.ui.screens.fakecall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.InterFamily
import com.as307.aryaa.ui.theme.PlayfairDisplayFamily

@Composable
fun FakeCallCountdownScreen(
    viewModel: FakeCallViewModel,
    onCancel: () -> Unit,
    onCountdownFinished: () -> Unit
) {
    val countdown by viewModel.countdownState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // When the countdown completes or transition to Ringing happens, navigate to Ringing screen
    LaunchedEffect(uiState) {
        if (uiState is FakeCallUiState.Ringing) {
            onCountdownFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AryaaColors.Navy)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Your call is coming in...",
            color = AryaaColors.White,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "$countdown",
            color = AryaaColors.Saffron,
            fontFamily = PlayfairDisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 80.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                viewModel.cancelScheduled()
                onCancel()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = AryaaColors.NavyCard,
                contentColor = AryaaColors.Slate
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "Cancel",
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
