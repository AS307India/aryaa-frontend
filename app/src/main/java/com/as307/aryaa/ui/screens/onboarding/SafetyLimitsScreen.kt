package com.as307.aryaa.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.AryaaTypography

@Composable
fun SafetyLimitsScreen(
    onContinue: () -> Unit
) {
    var isChecked by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AryaaColors.Navy)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Before you continue",
                style = AryaaTypography.displayLarge.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                color = AryaaColors.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ARYAA helps the right people find you fast — but it has real limits you should know about.",
                style = AryaaTypography.bodyLarge,
                color = AryaaColors.SlateLight
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Cards
            LimitationCard(
                title = "It needs a network connection",
                description = "If you're somewhere with no signal or data, alerts can't reach your contacts until you're back in range."
            )

            Spacer(modifier = Modifier.height(16.dp))

            LimitationCard(
                title = "It needs battery",
                description = "A dead phone can't send an alert. Keep some charge in reserve if you know you're heading somewhere risky."
            )

            Spacer(modifier = Modifier.height(16.dp))

            LimitationCard(
                title = "Location isn't always exact",
                description = "GPS accuracy depends on your surroundings — indoors, underground, or in dense areas it can be off by a wide margin."
            )

            Spacer(modifier = Modifier.height(16.dp))

            LimitationCard(
                title = "Contacts can view from lock screen",
                description = "They can respond instantly without unlocking their phone. Anyone holding an unlocked responder's phone during an active alert can see it too."
            )

            Spacer(modifier = Modifier.height(16.dp))

            LimitationCard(
                title = "ARYAA does not call emergency services",
                description = "It helps you and your contacts reach 112 faster — it doesn't replace them."
            )

            Spacer(modifier = Modifier.height(16.dp))

            LimitationCard(
                title = "Alerts are prioritized by proximity",
                description = "Family-tier contacts are notified after a 30-second delay if no nearby responders check in first, to mobilize local help and reduce family alarm."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = AryaaColors.NavyBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { isChecked = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = AryaaColors.Saffron,
                    uncheckedColor = AryaaColors.Slate,
                    checkmarkColor = AryaaColors.White
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "I understand ARYAA's limitations and that it cannot guarantee help will arrive.",
                style = AryaaTypography.bodyMedium,
                color = AryaaColors.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            enabled = isChecked,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AryaaColors.Saffron,
                contentColor = AryaaColors.Navy,
                disabledContainerColor = AryaaColors.NavyBorder,
                disabledContentColor = AryaaColors.Slate
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Continue",
                style = AryaaTypography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if (isChecked) AryaaColors.Navy else AryaaColors.Slate)
            )
        }
    }
}

@Composable
fun LimitationCard(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AryaaColors.NavyCard, shape = RoundedCornerShape(12.dp))
            .border(1.dp, AryaaColors.NavyBorder, shape = RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = AryaaTypography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AryaaColors.Saffron)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = AryaaTypography.bodyMedium.copy(color = AryaaColors.SlateLight)
        )
    }
}
