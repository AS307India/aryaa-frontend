package com.as307.aryaa.ui.screens.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.as307.aryaa.ui.theme.AryaaColors

private val RELATIONSHIPS = listOf("FAMILY", "FRIEND", "COLLEAGUE", "NEIGHBOUR", "OTHER")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, relationship: String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("FAMILY") }
    var relationshipExpanded by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AryaaColors.Saffron,
        unfocusedBorderColor = AryaaColors.NavyBorder,
        focusedLabelColor = AryaaColors.Saffron,
        unfocusedLabelColor = AryaaColors.Slate,
        cursorColor = AryaaColors.Saffron,
        focusedTextColor = AryaaColors.SlateLight,
        unfocusedTextColor = AryaaColors.SlateLight
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = AryaaColors.NavyCard,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add Trusted Contact",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = AryaaColors.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Error banner
            AnimatedVisibility(visible = errorMessage != null || localError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AryaaColors.Crimson.copy(alpha = 0.12f))
                        .border(1.dp, AryaaColors.Crimson.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        tint = AryaaColors.Crimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = errorMessage ?: localError ?: "",
                        color = AryaaColors.Crimson,
                        fontSize = 13.sp
                    )
                }
            }

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; localError = null },
                label = { Text("Full Name") },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            // Phone field
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it; localError = null },
                label = { Text("Phone Number") },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            // Relationship dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = relationship,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Relationship") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = AryaaColors.Slate,
                            modifier = Modifier.clickable { relationshipExpanded = true }
                        )
                    },
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { relationshipExpanded = true }
                )
                DropdownMenu(
                    expanded = relationshipExpanded,
                    onDismissRequest = { relationshipExpanded = false }
                ) {
                    RELATIONSHIPS.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                relationship = option
                                relationshipExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // CTA button
            Button(
                onClick = {
                    val phoneRegex = Regex("^(?:\\+91)?[6-9]\\d{9}$")
                    localError = when {
                        name.isBlank() -> "Name is required"
                        phone.isBlank() -> "Phone number is required"
                        !phone.matches(phoneRegex) -> "Enter a valid 10-digit Indian number"
                        else -> null
                    }
                    if (localError == null) {
                        onConfirm(name.trim(), phone.trim(), relationship)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AryaaColors.Saffron,
                    disabledContainerColor = AryaaColors.Saffron.copy(alpha = 0.5f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = AryaaColors.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        "Add Contact",
                        color = AryaaColors.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
