package com.as307.aryaa.ui.screens.medicalid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.as307.aryaa.ui.components.MedicalIdCard
import com.as307.aryaa.ui.theme.AryaaColors
import com.as307.aryaa.ui.theme.InterFamily

fun isValidIndianPhone(phone: String): Boolean {
    if (phone.isBlank()) return true
    val regex = Regex("^(\\+91[\\-\\s]?)?[6-9]\\d{9}$")
    return regex.matches(phone)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalIdEditScreen(
    viewModel: MedicalIdViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val bloodType by viewModel.bloodType.collectAsState()
    val allergies by viewModel.allergies.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val conditions by viewModel.conditions.collectAsState()
    val emergencyContactName by viewModel.emergencyContactName.collectAsState()
    val emergencyContactPhone by viewModel.emergencyContactPhone.collectAsState()
    val organDonor by viewModel.organDonor.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()

    var bloodDropdownExpanded by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.saveMedicalId(
            blood = bloodType,
            allerg = allergies,
            meds = medications,
            conds = conditions,
            contactName = emergencyContactName,
            contactPhone = emergencyContactPhone,
            donor = organDonor,
            noteTexts = notes
        )
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            viewModel.resetSavedState()
            onBack()
        }
    }

    Scaffold(
        containerColor = AryaaColors.Navy,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Go Back",
                        tint = AryaaColors.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EDIT MEDICAL ID",
                    color = AryaaColors.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 1.5.sp,
                    fontFamily = InterFamily
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Form Section "Medical Info"
            MedicalFormSection(title = "Medical Info") {
                // Blood Type Dropdown selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Blood Type",
                        color = AryaaColors.Slate,
                        fontFamily = InterFamily,
                        fontSize = 13.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AryaaColors.Navy)
                            .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(12.dp))
                            .clickable { bloodDropdownExpanded = true }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bloodType ?: "Unknown",
                                color = AryaaColors.White,
                                fontFamily = InterFamily,
                                fontSize = 16.sp
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = AryaaColors.Slate
                            )
                        }

                        DropdownMenu(
                            expanded = bloodDropdownExpanded,
                            onDismissRequest = { bloodDropdownExpanded = false },
                            modifier = Modifier.background(AryaaColors.NavyCard)
                        ) {
                            val options = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown")
                            options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(text = option, color = AryaaColors.White) },
                                    onClick = {
                                        viewModel.setBloodType(option)
                                        bloodDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Allergies Input
                MedicalIdTextField(
                    label = "Allergies",
                    value = allergies ?: "",
                    onValueChange = { viewModel.setAllergies(it) }
                )

                // Medications Input
                MedicalIdTextField(
                    label = "Medications",
                    value = medications ?: "",
                    onValueChange = { viewModel.setMedications(it) }
                )

                // Conditions Input
                MedicalIdTextField(
                    label = "Conditions",
                    value = conditions ?: "",
                    onValueChange = { viewModel.setConditions(it) }
                )
            }

            // Form Section "Emergency Contact"
            MedicalFormSection(title = "Emergency Contact") {
                MedicalIdTextField(
                    label = "Name",
                    value = emergencyContactName ?: "",
                    onValueChange = { viewModel.setEmergencyContactName(it) }
                )

                // Phone field with validation check
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = emergencyContactPhone ?: "",
                        onValueChange = {
                            viewModel.setEmergencyContactPhone(it)
                            phoneError = !isValidIndianPhone(it)
                        },
                        label = { Text("Phone Number", color = AryaaColors.Slate) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = phoneError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AryaaColors.White,
                            unfocusedTextColor = AryaaColors.White,
                            focusedBorderColor = if (phoneError) AryaaColors.Crimson else AryaaColors.Saffron,
                            unfocusedBorderColor = if (phoneError) AryaaColors.Crimson else AryaaColors.NavyBorder,
                            focusedContainerColor = AryaaColors.Navy,
                            unfocusedContainerColor = AryaaColors.Navy
                        ),
                        singleLine = true
                    )
                    if (phoneError) {
                        Text(
                            text = "Invalid Indian mobile phone format (e.g. 9876543210 or +919876543210)",
                            color = AryaaColors.Crimson,
                            fontFamily = InterFamily,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Form Section "Other"
            MedicalFormSection(title = "Other") {
                // Organ Donor Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Organ Donor",
                        color = AryaaColors.White,
                        fontFamily = InterFamily,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = organDonor,
                        onCheckedChange = { viewModel.setOrganDonor(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AryaaColors.White,
                            checkedTrackColor = AryaaColors.Saffron,
                            uncheckedThumbColor = AryaaColors.Slate,
                            uncheckedTrackColor = AryaaColors.NavyCard
                        )
                    )
                }

                // Notes Input
                MedicalIdTextField(
                    label = "Notes",
                    value = notes ?: "",
                    onValueChange = { viewModel.setNotes(it) }
                )
            }

            // Preview Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Preview (First Responder Lockscreen Card)",
                    color = AryaaColors.Slate,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )
                MedicalIdCard(
                    bloodType = bloodType,
                    allergies = allergies,
                    medications = medications,
                    conditions = conditions,
                    emergencyContactName = emergencyContactName,
                    emergencyContactPhone = emergencyContactPhone,
                    organDonor = organDonor,
                    notes = notes
                )
            }

            // Save Button
            Button(
                onClick = {
                    if (phoneError) return@Button
                    val phoneStr = emergencyContactPhone ?: ""
                    if (phoneStr.isNotBlank() && !isValidIndianPhone(phoneStr)) {
                        phoneError = true
                        return@Button
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.saveMedicalId(
                            blood = bloodType,
                            allerg = allergies,
                            meds = medications,
                            conds = conditions,
                            contactName = emergencyContactName,
                            contactPhone = emergencyContactPhone,
                            donor = organDonor,
                            noteTexts = notes
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AryaaColors.Saffron,
                    contentColor = AryaaColors.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Save",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MedicalFormSection(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AryaaColors.NavyCard)
            .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            color = AryaaColors.Saffron,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
private fun MedicalIdTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = AryaaColors.Slate) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AryaaColors.White,
            unfocusedTextColor = AryaaColors.White,
            focusedBorderColor = AryaaColors.Saffron,
            unfocusedBorderColor = AryaaColors.NavyBorder,
            focusedContainerColor = AryaaColors.Navy,
            unfocusedContainerColor = AryaaColors.Navy
        )
    )
}
