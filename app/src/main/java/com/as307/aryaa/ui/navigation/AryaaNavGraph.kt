package com.as307.aryaa.ui.navigation

import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.as307.aryaa.data.local.TokenStorage
import com.as307.aryaa.data.local.FakeCallPreferences
import com.as307.aryaa.data.remote.SessionEvent
import com.as307.aryaa.data.remote.SessionManager
import com.as307.aryaa.data.repository.ContactsRepository
import com.as307.aryaa.ui.screens.auth.login.LoginScreen
import com.as307.aryaa.ui.screens.auth.login.LoginViewModel
import com.as307.aryaa.ui.screens.auth.signup.SignupScreen
import com.as307.aryaa.ui.screens.auth.signup.SignupViewModel
import com.as307.aryaa.ui.screens.contacts.ContactsScreen
import com.as307.aryaa.ui.screens.home.HomeScreen
import com.as307.aryaa.ui.screens.sos.SosScreen
import com.as307.aryaa.ui.screens.splash.SplashScreen
import com.as307.aryaa.ui.screens.fakecall.FakeCallViewModel
import com.as307.aryaa.ui.screens.fakecall.FakeCallCountdownScreen
import com.as307.aryaa.ui.screens.fakecall.FakeCallRingingScreen
import com.as307.aryaa.ui.screens.fakecall.FakeCallInCallScreen
import com.as307.aryaa.ui.screens.profile.ProfileViewModel
import com.as307.aryaa.ui.screens.profile.ProfileScreen
import com.as307.aryaa.ui.screens.medicalid.MedicalIdViewModel
import com.as307.aryaa.ui.screens.medicalid.MedicalIdEditScreen
import com.as307.aryaa.ui.screens.practice.PracticeSosScreen
import com.as307.aryaa.ui.screens.practice.PracticeSummaryScreen
import com.as307.aryaa.ui.screens.deadzone.DeadZoneScreen
import com.as307.aryaa.ui.theme.AryaaColors

// Routes that belong to the authenticated shell (show bottom nav)
private val AUTHENTICATED_ROUTES = setOf(
    Destination.Home.route,
    Destination.Contacts.route,
    Destination.Sos.route,
    Destination.Profile.route
)

@Composable
fun AryaaNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destination.Splash.route,
    tokenStorage: TokenStorage,
    contactsRepository: ContactsRepository,
    sosRepository: com.as307.aryaa.data.repository.SosRepository,
    deadZoneRepository: com.as307.aryaa.data.repository.DeadZoneRepository,
    sosServiceManager: com.as307.aryaa.service.SosServiceManager,
    fakeCallPreferences: FakeCallPreferences,
    emergencyStateHolder: com.as307.aryaa.ui.screens.emergency.EmergencyStateHolder
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val fakeCallViewModel: FakeCallViewModel = hiltViewModel()

    // Observe hardware SOS trigger events and navigate to SOS screen immediately
    LaunchedEffect(Unit) {
        sosServiceManager.sosTriggerEvents.collect {
            navController.navigate(Destination.Sos.route) {
                popUpTo(Destination.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // Observe session expiry events from AuthInterceptor and force-navigate to Login
    LaunchedEffect(Unit) {
        SessionManager.events.collect { event ->
            if (event is SessionEvent.SessionExpired) {
                navController.navigate(Destination.Login.createRoute()) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // Observe active emergency notifications and deep link immediately
    val activeEmergency by emergencyStateHolder.activeEmergency.collectAsState()
    LaunchedEffect(activeEmergency) {
        activeEmergency?.let {
            // Add a short delay to prevent navigation race condition during initial graph setup
            delay(500L)
            navController.navigate(Destination.EmergencyResponse.route) {
                popUpTo(Destination.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        android.util.Log.i("TIMING_DATA", "AryaaNavGraph: DisposableEffect registered")
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            android.util.Log.i("TIMING_DATA", "AryaaNavGraph: Lifecycle event occurred: $event")
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    try {
                        val token = tokenStorage.getToken()
                        android.util.Log.i("TIMING_DATA", "AryaaNavGraph: ON_RESUME checked, token present: ${token != null}")
                        if (token != null) {
                            sosRepository.getActiveIncoming()
                                .onSuccess { response ->
                                    android.util.Log.i("TIMING_DATA", "active-incoming poll response: $response")
                                    if (response.hasActiveIncoming && response.eventId != null) {
                                        val current = emergencyStateHolder.activeEmergency.value
                                        if (current == null || current.sosEventId != response.eventId) {
                                            val data = com.as307.aryaa.ui.screens.emergency.EmergencySosData(
                                                sosEventId = response.eventId,
                                                userName = response.victimName ?: "A contact",
                                                userPhone = "",
                                                latitude = response.lat,
                                                longitude = response.lng,
                                                w3wAddress = response.w3w,
                                                triggeredAt = response.triggeredAt ?: "",
                                                accuracy = response.accuracy,
                                                tier = response.tier ?: "FAMILY"
                                            )
                                            emergencyStateHolder.setActive(data)
                                        }
                                    } else {
                                        if (emergencyStateHolder.activeEmergency.value != null) {
                                            emergencyStateHolder.clear()
                                        }
                                    }
                                }
                                .onFailure { error ->
                                    android.util.Log.e("TIMING_DATA", "active-incoming poll failed: $error")
                                }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TIMING_DATA", "Failed to fetch active incoming alert: ${e.message}")
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = AryaaColors.Navy,
        bottomBar = {
            // Show bottom nav only on authenticated screens
            if (currentRoute in AUTHENTICATED_ROUTES) {
                BottomNavBar(currentRoute = currentRoute) { tab ->
                    when (tab) {
                        BottomNavTab.HOME -> navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                        BottomNavTab.CONTACTS -> navController.navigate(Destination.Contacts.route) {
                            launchSingleTop = true
                        }
                        BottomNavTab.SOS -> navController.navigate(Destination.Sos.route) {
                            launchSingleTop = true
                        }
                        BottomNavTab.PROFILE -> navController.navigate(Destination.Profile.route) {
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(Destination.Splash.route) {
                SplashScreen(
                    onNavigate = { targetRoute ->
                        navController.navigate(targetRoute) {
                            popUpTo(Destination.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Destination.Login.route,
                arguments = listOf(navArgument("email") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStack ->
                val email = backStack.arguments?.getString("email")
                val loginViewModel: LoginViewModel = hiltViewModel()
                LoginScreen(
                    initialEmail = email,
                    onNavigateToSignup = { typedEmail ->
                        navController.navigate(Destination.Signup.createRoute(typedEmail)) {
                            popUpTo(Destination.Login.route) { inclusive = true }
                        }
                    },
                    onLoginSuccess = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Login.route) { inclusive = true }
                        }
                    },
                    viewModel = loginViewModel
                )
            }

            composable(
                route = Destination.Signup.route,
                arguments = listOf(navArgument("email") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStack ->
                val email = backStack.arguments?.getString("email")
                val signupViewModel: SignupViewModel = hiltViewModel()
                SignupScreen(
                    initialEmail = email,
                    onNavigateToLogin = { typedEmail ->
                        navController.navigate(Destination.Login.createRoute(typedEmail)) {
                            popUpTo(Destination.Signup.route) { inclusive = true }
                        }
                    },
                    onSignupSuccess = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Signup.route) { inclusive = true }
                        }
                    },
                    viewModel = signupViewModel
                )
            }

            composable(Destination.Home.route) {
                HomeScreen(
                    tokenStorage = tokenStorage,
                    contactsRepository = contactsRepository,
                    sosRepository = sosRepository,
                    deadZoneRepository = deadZoneRepository,
                    fakeCallPreferences = fakeCallPreferences,
                    sosServiceManager = sosServiceManager,
                    onNavigateToContacts = {
                        navController.navigate(Destination.Contacts.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSos = {
                        navController.navigate(Destination.Sos.route) {
                            launchSingleTop = true
                        }
                    },
                    onTriggerFakeCall = { delaySeconds ->
                        fakeCallViewModel.scheduleFakeCall()
                        if (delaySeconds > 0) {
                            navController.navigate(Destination.FakeCallCountdown.route)
                        } else {
                            navController.navigate(Destination.FakeCallRinging.route)
                        }
                    },
                    onNavigateToPracticeMode = {
                        navController.navigate(Destination.PracticeMode.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToDeadZone = {
                        navController.navigate(Destination.DeadZone.route) {
                            launchSingleTop = true
                        }
                    },
                    activeEmergency = activeEmergency,
                    onNavigateToEmergencyResponse = {
                        navController.navigate(Destination.EmergencyResponse.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Destination.Contacts.route) {
                ContactsScreen()
            }

            composable(Destination.Sos.route) {
                SosScreen(
                    onNavigateToContacts = {
                        navController.navigate(Destination.Contacts.route) {
                            popUpTo(Destination.Sos.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Destination.FakeCallCountdown.route) {
                FakeCallCountdownScreen(
                    viewModel = fakeCallViewModel,
                    onCancel = {
                        navController.popBackStack()
                    },
                    onCountdownFinished = {
                        navController.navigate(Destination.FakeCallRinging.route) {
                            popUpTo(Destination.FakeCallCountdown.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destination.FakeCallRinging.route) {
                FakeCallRingingScreen(
                    viewModel = fakeCallViewModel,
                    onDecline = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Home.route) { inclusive = false }
                        }
                    },
                    onAnswer = {
                        navController.navigate(Destination.FakeCallInCall.route) {
                            popUpTo(Destination.FakeCallRinging.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destination.FakeCallInCall.route) {
                FakeCallInCallScreen(
                    viewModel = fakeCallViewModel,
                    onCallEnded = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Home.route) { inclusive = false }
                        }
                    }
                )
            }

            composable(Destination.Profile.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateToContacts = {
                        navController.navigate(Destination.Contacts.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToMedicalId = {
                        navController.navigate(Destination.MedicalIdEdit.route) {
                            launchSingleTop = true
                        }
                    },
                    onSignOutComplete = {
                        navController.navigate(Destination.Login.createRoute()) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destination.MedicalIdEdit.route) {
                val medicalIdViewModel: MedicalIdViewModel = hiltViewModel()
                MedicalIdEditScreen(
                    viewModel = medicalIdViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Destination.EmergencyResponse.route) {
                com.as307.aryaa.ui.screens.emergency.EmergencyResponseScreen(
                    onDismiss = {
                        emergencyStateHolder.clear()
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Home.route) { inclusive = false }
                        }
                    }
                )
            }

            composable(Destination.PracticeMode.route) {
                PracticeSosScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSummary = { duration, contacts, accuracy, duress ->
                        navController.navigate(
                            Destination.PracticeSummary.createRoute(duration, contacts, accuracy, duress)
                        ) {
                            popUpTo(Destination.PracticeMode.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Destination.PracticeSummary.route,
                arguments = listOf(
                    navArgument("duration") { type = NavType.IntType },
                    navArgument("contacts") { type = NavType.IntType },
                    navArgument("accuracy") { type = NavType.IntType },
                    navArgument("duress") { type = NavType.BoolType }
                )
            ) { backStack ->
                val duration = backStack.arguments?.getInt("duration") ?: 0
                val contacts = backStack.arguments?.getInt("contacts") ?: 0
                val accuracy = backStack.arguments?.getInt("accuracy") ?: 0
                val duress = backStack.arguments?.getBoolean("duress") ?: false

                PracticeSummaryScreen(
                    duration = duration,
                    contacts = contacts,
                    accuracy = accuracy,
                    duress = duress,
                    onDone = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Destination.DeadZone.route) {
                DeadZoneScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
