package com.as307.aryaa.ui.screens.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.ui.theme.AryaaColors
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.graphicsLayer
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deletingId by viewModel.deletingId.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    val context = LocalContext.current
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onSmsPermissionResult(isGranted)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddSheet by remember { mutableStateOf(false) }
    var addLoading by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }
    var contactToDelete by remember { mutableStateOf<ContactDto?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        containerColor = AryaaColors.Navy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trusted Contacts",
                        color = AryaaColors.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AryaaColors.Navy
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    addError = null
                    showAddSheet = true
                },
                containerColor = AryaaColors.Saffron,
                contentColor = AryaaColors.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add contact")
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = AryaaColors.NavyCard,
                    contentColor = AryaaColors.Crimson
                )
            }
        }
    ) { paddingValues ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refresh()
                isRefreshing = false
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is ContactsViewModel.UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AryaaColors.Saffron)
                    }
                }

                is ContactsViewModel.UiState.Empty -> EmptyContactsState(
                    onAddClick = {
                        addError = null
                        showAddSheet = true
                    }
                )

                is ContactsViewModel.UiState.Success -> {
                    val nearbyContacts = state.contacts.filter { it.isNearby == "YES" }
                    val familyContacts = state.contacts.filter { it.isNearby != "YES" }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Prominent warning if 0 nearby responders configured
                        if (nearbyContacts.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(androidx.compose.ui.graphics.Color(0xFFFEF3C7)) // Amber 100
                                        .border(1.dp, androidx.compose.ui.graphics.Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Error,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color(0xFFD97706)
                                        )
                                        Text(
                                            text = "Warning: 0 nearby responders configured. Set contacts who live or work nearby to YES for instant local rescue coordination.",
                                            color = androidx.compose.ui.graphics.Color(0xFF92400E),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Nearby Responders section
                        if (nearbyContacts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Nearby Responders",
                                    color = AryaaColors.Slate,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(nearbyContacts, key = { "nearby_" + it.id }) { contact ->
                                val view = LocalView.current
                                var hasTriggeredHaptic by remember { mutableStateOf(false) }

                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                contactToDelete = contact
                                            }
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${contact.phone}")
                                                }
                                                context.startActivity(dialIntent)
                                            }
                                            else -> {}
                                        }
                                        false
                                    },
                                    positionalThreshold = { distance -> distance * 0.3f }
                                )

                                LaunchedEffect(dismissState.progress) {
                                    if (dismissState.progress >= 0.3f && dismissState.targetValue != dismissState.currentValue) {
                                        if (!hasTriggeredHaptic) {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                            hasTriggeredHaptic = true
                                        }
                                    } else if (dismissState.progress < 0.3f) {
                                        hasTriggeredHaptic = false
                                    }
                                }

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val direction = dismissState.dismissDirection
                                        val color = when (direction) {
                                            SwipeToDismissBoxValue.EndToStart -> AryaaColors.Crimson.copy(alpha = 0.85f)
                                            SwipeToDismissBoxValue.StartToEnd -> androidx.compose.ui.graphics.Color(0xFF10B981) // Emerald
                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                        }
                                        val scale = dismissState.progress.coerceIn(0.5f, 1f)
                                        val alpha = dismissState.progress.coerceIn(0f, 1f)

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(color)
                                                .padding(horizontal = 20.dp)
                                        ) {
                                            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                                Row(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterStart)
                                                        .graphicsLayer(
                                                            scaleX = scale,
                                                            scaleY = scale,
                                                            alpha = alpha
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Phone,
                                                        contentDescription = "Call",
                                                        tint = AryaaColors.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Call ${contact.name}",
                                                        color = AryaaColors.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                                                Row(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterEnd)
                                                        .graphicsLayer(
                                                            scaleX = scale,
                                                            scaleY = scale,
                                                            alpha = alpha
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Delete",
                                                        color = AryaaColors.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete,
                                                        contentDescription = "Delete",
                                                        tint = AryaaColors.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    content = {
                                        ContactCard(
                                            contact = contact,
                                            isDeleting = deletingId == contact.id,
                                            onDeleteClick = { contactToDelete = contact }
                                        )
                                    }
                                )
                            }
                        }

                        // Faraway Family & Friends section
                        if (familyContacts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Faraway Family & Friends",
                                    color = AryaaColors.Slate,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(familyContacts, key = { "family_" + it.id }) { contact ->
                                val view = LocalView.current
                                var hasTriggeredHaptic by remember { mutableStateOf(false) }

                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                contactToDelete = contact
                                            }
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${contact.phone}")
                                                }
                                                context.startActivity(dialIntent)
                                            }
                                            else -> {}
                                        }
                                        false
                                    },
                                    positionalThreshold = { distance -> distance * 0.3f }
                                )

                                LaunchedEffect(dismissState.progress) {
                                    if (dismissState.progress >= 0.3f && dismissState.targetValue != dismissState.currentValue) {
                                        if (!hasTriggeredHaptic) {
                                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                            hasTriggeredHaptic = true
                                        }
                                    } else if (dismissState.progress < 0.3f) {
                                        hasTriggeredHaptic = false
                                    }
                                }

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        val direction = dismissState.dismissDirection
                                        val color = when (direction) {
                                            SwipeToDismissBoxValue.EndToStart -> AryaaColors.Crimson.copy(alpha = 0.85f)
                                            SwipeToDismissBoxValue.StartToEnd -> androidx.compose.ui.graphics.Color(0xFF10B981) // Emerald
                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                        }
                                        val scale = dismissState.progress.coerceIn(0.5f, 1f)
                                        val alpha = dismissState.progress.coerceIn(0f, 1f)

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(color)
                                                .padding(horizontal = 20.dp)
                                        ) {
                                            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                                Row(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterStart)
                                                        .graphicsLayer(
                                                            scaleX = scale,
                                                            scaleY = scale,
                                                            alpha = alpha
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Phone,
                                                        contentDescription = "Call",
                                                        tint = AryaaColors.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Call ${contact.name}",
                                                        color = AryaaColors.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                                                Row(
                                                    modifier = Modifier
                                                        .align(Alignment.CenterEnd)
                                                        .graphicsLayer(
                                                            scaleX = scale,
                                                            scaleY = scale,
                                                            alpha = alpha
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Delete",
                                                        color = AryaaColors.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete,
                                                        contentDescription = "Delete",
                                                        tint = AryaaColors.White,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    content = {
                                        ContactCard(
                                            contact = contact,
                                            isDeleting = deletingId == contact.id,
                                            onDeleteClick = { contactToDelete = contact }
                                        )
                                    }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }

                is ContactsViewModel.UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = AryaaColors.Crimson)
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = AryaaColors.NavyCard,
            title = {
                Text("Remove Contact?", color = AryaaColors.White, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "${contact.name} will be removed from your trusted contact list. This cannot be undone.",
                    color = AryaaColors.Slate
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeContact(contact.id)
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AryaaColors.Crimson)
                ) {
                    Text("Remove", color = AryaaColors.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancel", color = AryaaColors.Slate)
                }
            }
        )
    }

    // Add contact bottom sheet
    if (showAddSheet) {
        AddContactBottomSheet(
            sheetState = sheetState,
            onDismiss = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    showAddSheet = false
                    addLoading = false
                    addError = null
                }
            },
            onConfirm = { name, phone, relationship, isNearby ->
                addLoading = true
                addError = null
                viewModel.addContact(name, phone, relationship, isNearby) { success ->
                    addLoading = false
                    if (success) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showAddSheet = false
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.SEND_SMS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPermission) {
                                smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                            }
                        }
                    } else {
                        addError = viewModel.actionMessage.value
                        viewModel.clearActionMessage()
                    }
                }
            },
            isLoading = addLoading,
            errorMessage = addError
        )
    }
}

@Composable
private fun ContactCard(
    contact: ContactDto,
    isDeleting: Boolean,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AryaaColors.NavyCard)
            .border(1.dp, AryaaColors.NavyBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar initial
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AryaaColors.Saffron.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.first().uppercaseChar().toString(),
                color = AryaaColors.Saffron,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                color = AryaaColors.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = null,
                    tint = AryaaColors.Slate,
                    modifier = Modifier.size(12.dp)
                )
                Text(text = contact.phone, color = AryaaColors.Slate, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Relationship and Proximity badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AryaaColors.Emerald.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = contact.relationship.lowercase().replaceFirstChar { it.uppercase() },
                        color = AryaaColors.Emerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                val proximityText = when (contact.isNearby) {
                    "YES" -> "Nearby Responder"
                    "NO" -> "Faraway / Family"
                    else -> "Family / Sometimes Nearby"
                }
                val proximityColor = when (contact.isNearby) {
                    "YES" -> AryaaColors.Saffron
                    else -> AryaaColors.Slate
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(proximityColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = proximityText,
                        color = proximityColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (isDeleting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = AryaaColors.Crimson,
                strokeWidth = 2.dp
            )
        } else {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete contact",
                    tint = AryaaColors.Slate
                )
            }
        }
    }
}

@Composable
private fun EmptyContactsState(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            Icon(
                Icons.Filled.People,
                contentDescription = null,
                tint = AryaaColors.Slate,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "No trusted contacts yet",
                color = AryaaColors.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Text(
                "Add your first trusted contact so ARYAA knows who to reach in an emergency.",
                color = AryaaColors.Slate,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = AryaaColors.Saffron),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = AryaaColors.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Add First Contact",
                    color = AryaaColors.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
