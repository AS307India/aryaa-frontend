package com.as307.aryaa.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.as307.aryaa.ui.theme.AryaaColors

@Composable
fun PracticeSummaryScreen(
    duration: Int,
    contacts: Int,
    accuracy: Int,
    duress: Boolean,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AryaaColors.Navy)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large checklist completion icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = AryaaColors.Emerald,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Practice Complete!",
            color = AryaaColors.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Metrics Card List
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(AryaaColors.NavyCard, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "✓ You held the SOS button for $duration seconds",
                color = AryaaColors.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "✓ $contacts contacts would have been notified",
                color = AryaaColors.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "✓ Your location was accurate to ~${accuracy}m",
                color = AryaaColors.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            if (duress) {
                Text(
                    text = "✓ You practiced the duress gesture correctly",
                    color = AryaaColors.Emerald,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Reminder text
        Text(
            text = "Do this regularly so you're ready if you ever need it for real.",
            color = AryaaColors.Slate,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Done button
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(
                containerColor = AryaaColors.Saffron,
                contentColor = AryaaColors.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Done",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
