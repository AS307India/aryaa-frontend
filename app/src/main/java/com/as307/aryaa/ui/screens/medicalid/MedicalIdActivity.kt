package com.as307.aryaa.ui.screens.medicalid

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.as307.aryaa.data.local.MedicalIdPreferences
import com.as307.aryaa.ui.components.MedicalIdCard
import com.as307.aryaa.ui.theme.AryaaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MedicalIdActivity : ComponentActivity() {

    @Inject
    lateinit var medicalIdPreferences: MedicalIdPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set lock screen display flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Retrieve Medical ID data synchronously (runBlocking is a permitted exception here)
        val bloodType: String?
        val allergies: String?
        val medications: String?
        val conditions: String?
        val emergencyContactName: String?
        val emergencyContactPhone: String?
        val organDonor: Boolean
        val notes: String?

        runBlocking {
            bloodType = medicalIdPreferences.getBloodType()
            allergies = medicalIdPreferences.getAllergies()
            medications = medicalIdPreferences.getMedications()
            conditions = medicalIdPreferences.getConditions()
            emergencyContactName = medicalIdPreferences.getEmergencyContactName()
            emergencyContactPhone = medicalIdPreferences.getEmergencyContactPhone()
            organDonor = medicalIdPreferences.getOrganDonor()
            notes = medicalIdPreferences.getNotes()
        }

        setContent {
            AryaaTheme {
                Scaffold(
                    containerColor = Color.White
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(innerPadding)
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
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
                }
            }
        }
    }
}
