package com.as307.aryaa.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.as307.aryaa.ui.theme.InterFamily

@Composable
fun MedicalIdCard(
    bloodType: String?,
    allergies: String?,
    medications: String?,
    conditions: String?,
    emergencyContactName: String?,
    emergencyContactPhone: String?,
    organDonor: Boolean,
    notes: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Red Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC62828)) // Red
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "🆘 MEDICAL ID",
                    color = Color.White,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Blood Type Section (Prominent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BLOOD TYPE",
                            color = Color.Gray,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (bloodType.isNullOrBlank() || bloodType == "Unknown") "Unknown" else bloodType,
                            color = Color(0xFFC62828), // Crimson
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                    }

                    if (organDonor) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8F5E9)) // Light green
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🫀 Organ Donor",
                                color = Color(0xFF2E7D32), // Green
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Grid/Details: Allergies & Medications
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        MedicalField(
                            label = "ALLERGIES",
                            value = allergies
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        MedicalField(
                            label = "MEDICATIONS",
                            value = medications
                        )
                    }
                }

                // Grid/Details: Conditions & Notes
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        MedicalField(
                            label = "CONDITIONS",
                            value = conditions
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        MedicalField(
                            label = "NOTES",
                            value = notes
                        )
                    }
                }

                // Emergency Contact Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "EMERGENCY CONTACT",
                        color = Color.Gray,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val contactName = if (emergencyContactName.isNullOrBlank()) "None" else emergencyContactName
                    val contactPhone = if (emergencyContactPhone.isNullOrBlank()) "None" else emergencyContactPhone

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = contactName,
                                color = Color(0xFF212121),
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            if (contactPhone != "None") {
                                Text(
                                    text = contactPhone,
                                    color = Color(0xFF616161),
                                    fontFamily = InterFamily,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        if (contactPhone != "None") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFC62828))
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:$contactPhone")
                                        }
                                        context.startActivity(intent)
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = "Call Emergency Contact",
                                    tint = Color.White,
                                    modifier = Modifier.clickable {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:$contactPhone")
                                        }
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }

                // Footer
                Text(
                    text = "Generated by ARYAA — आर्या",
                    color = Color.Gray,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MedicalField(label: String, value: String?) {
    Column {
        Text(
            text = label,
            color = Color.Gray,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )
        Text(
            text = if (value.isNullOrBlank()) "None" else value,
            color = if (value.isNullOrBlank()) Color.LightGray else Color(0xFF212121),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 3
        )
    }
}
