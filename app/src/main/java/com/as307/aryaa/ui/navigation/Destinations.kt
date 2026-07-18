package com.as307.aryaa.ui.navigation

sealed class Destination(val route: String) {
    object Splash : Destination("splash")
    object SafetyLimits : Destination("safety_limits")
    object Login : Destination("login?email={email}") {
        fun createRoute(email: String = ""): String = "login?email=$email"
    }
    object Signup : Destination("signup?email={email}") {
        fun createRoute(email: String = ""): String = "signup?email=$email"
    }
    object Home : Destination("home")
    object Contacts : Destination("contacts")
    object AddContact : Destination("add_contact")
    object Sos : Destination("sos")
    object FakeCallCountdown : Destination("fake_call_countdown")
    object FakeCallRinging : Destination("fake_call_ringing")
    object FakeCallInCall : Destination("fake_call_in_call")
    object Profile : Destination("profile")
    object MedicalIdEdit : Destination("medical_id_edit")
    object EmergencyResponse : Destination("emergency_response")
    object PracticeMode : Destination("practice_mode")
    object PracticeSummary : Destination("practice_summary?duration={duration}&contacts={contacts}&accuracy={accuracy}&duress={duress}") {
        fun createRoute(duration: Int, contacts: Int, accuracy: Int, duress: Boolean): String =
            "practice_summary?duration=$duration&contacts=$contacts&accuracy=$accuracy&duress=$duress"
    }
    object DeadZone : Destination("dead_zone")
    object LocationShare : Destination("location_share")
}
