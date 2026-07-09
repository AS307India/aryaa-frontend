package com.as307.aryaa.service

import com.google.firebase.messaging.RemoteMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

class AryaaFirebaseMessagingServiceTest {

    @Test
    fun onMessageReceived_withSosAlert_triggersNotification() {
        val service = spyk(AryaaFirebaseMessagingService(), recordPrivateCalls = true)
        service.emergencyStateHolder = mockk(relaxed = true)
        val mockMessage = mockk<RemoteMessage>()
        val mockData = mapOf(
            "type" to "SOS_ALERT",
            "userName" to "Alice",
            "userPhone" to "+919000000001",
            "sosEventId" to "event-123",
            "latitude" to "18.5203",
            "longitude" to "73.8567",
            "w3wAddress" to "",
            "triggeredAt" to "2026-07-08T10:00:00Z",
            "accuracy" to "12.5"
        )
        every { mockMessage.data } returns mockData
        every { mockMessage.notification } returns null
        every { mockMessage.from } returns "test-sender"

        // Stub out Android framework call to showIncomingSosNotification to avoid Context crash on JVM
        every { 
            service["showIncomingSosNotification"](
                any<String>(), 
                any<String>(), 
                any<String>(), 
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>(),
                any<String>()
            ) 
        } returns Unit

        service.onMessageReceived(mockMessage)

        // Verify that the notification helper was called with correct extracted parameters from data payload
        verify(exactly = 1) {
            service["showIncomingSosNotification"](
                "🆘 Alice needs help!",
                "Location: 18.5203, 73.8567 at 15:30 IST",
                "+919000000001",
                "event-123",
                "Alice",
                "18.5203",
                "73.8567",
                "",
                "2026-07-08T10:00:00Z",
                "12.5"
            )
        }
    }

    @Test
    fun onMessageReceived_withSosCancel_clearsEmergencyState() {
        val service = spyk(AryaaFirebaseMessagingService(), recordPrivateCalls = true)
        val stateHolder = mockk<com.as307.aryaa.ui.screens.emergency.EmergencyStateHolder>(relaxed = true)
        service.emergencyStateHolder = stateHolder

        val mockMessage = mockk<RemoteMessage>()
        val mockData = mapOf(
            "type" to "SOS_CANCEL",
            "sosEventId" to "event-123"
        )
        every { mockMessage.data } returns mockData
        every { mockMessage.notification } returns null
        every { mockMessage.from } returns "test-sender"

        // Mock activeEmergency value check
        val activeData = com.as307.aryaa.ui.screens.emergency.EmergencySosData(
            sosEventId = "event-123",
            userName = "Alice",
            userPhone = "+919000000001",
            latitude = 18.5203,
            longitude = 73.8567,
            w3wAddress = "",
            triggeredAt = "2026-07-08T10:00:00Z"
        )
        every { stateHolder.activeEmergency.value } returns activeData

        // Stub getSystemService to return mocked notification manager
        val mockNotificationManager = mockk<android.app.NotificationManager>(relaxed = true)
        every { service.getSystemService(android.content.Context.NOTIFICATION_SERVICE) } returns mockNotificationManager

        service.onMessageReceived(mockMessage)

        // Verify that notificationManager.cancel was called with matching eventId hash
        verify(exactly = 1) { mockNotificationManager.cancel("event-123".hashCode()) }

        // Verify state holder was cleared
        verify(exactly = 1) { stateHolder.clear() }
    }
}
