package com.as307.aryaa.data.remote

import com.as307.aryaa.data.remote.dto.AuthResponse
import com.as307.aryaa.data.remote.dto.LoginRequest
import com.as307.aryaa.data.remote.dto.SignupRequest
import com.as307.aryaa.data.remote.dto.ContactDto
import com.as307.aryaa.data.remote.dto.AddContactRequest
import com.as307.aryaa.data.remote.dto.SosTriggerRequest
import com.as307.aryaa.data.remote.dto.SosResponse
import com.as307.aryaa.data.remote.dto.SosCancelRequest
import com.as307.aryaa.data.remote.dto.SosCancelResponse
import com.as307.aryaa.data.remote.dto.SosHistoryItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query
import com.as307.aryaa.data.remote.dto.NearbyServiceResponse
import com.as307.aryaa.data.remote.dto.ProfileAddressRequest
import com.as307.aryaa.data.remote.dto.SafetyReportRequest
import com.as307.aryaa.data.remote.dto.SafetyMapPin

interface AryaaApi {

    @POST("api/auth/register")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @GET("api/contacts")
    suspend fun getContacts(): Response<List<ContactDto>>

    @POST("api/contacts")
    suspend fun addContact(
        @Body request: AddContactRequest
    ): Response<ContactDto>

    @DELETE("api/contacts/{id}")
    suspend fun deleteContact(
        @Path("id") id: String
    ): Response<Unit>

    @POST("api/sos/trigger")
    suspend fun triggerSos(
        @Body request: SosTriggerRequest
    ): Response<SosResponse>

    @POST("api/sos/cancel")
    suspend fun cancelSos(
        @Body request: SosCancelRequest
    ): Response<SosCancelResponse>

    @POST("api/sos/duress-cancel")
    suspend fun duressCancel(
        @Body request: SosCancelRequest
    ): Response<SosCancelResponse>

    @GET("api/sos/history")
    suspend fun getSosHistory(): Response<List<SosHistoryItem>>

    @POST("api/sos/location-update")
    suspend fun updateLocation(
        @Body request: com.as307.aryaa.data.remote.dto.SosLocationUpdateRequest
    ): Response<Unit>

    @POST("api/users/fcm-token")
    suspend fun registerFcmToken(
        @Body request: com.as307.aryaa.data.remote.dto.FcmTokenRequest
    ): Response<Unit>

    @POST("api/deadzone/start")
    suspend fun startDeadZone(
        @Body request: com.as307.aryaa.data.remote.dto.DeadZoneStartRequest
    ): Response<com.as307.aryaa.data.remote.dto.DeadZoneResponse>

    @POST("api/deadzone/checkin")
    suspend fun checkInDeadZone(
        @Body request: com.as307.aryaa.data.remote.dto.DeadZoneCheckInRequest
    ): Response<com.as307.aryaa.data.remote.dto.DeadZoneResponse>

    @POST("api/deadzone/cancel")
    suspend fun cancelDeadZone(
        @Body request: com.as307.aryaa.data.remote.dto.DeadZoneCheckInRequest
    ): Response<com.as307.aryaa.data.remote.dto.DeadZoneResponse>

    @GET("api/deadzone/status")
    suspend fun getDeadZoneStatus(): Response<com.as307.aryaa.data.remote.dto.DeadZoneStatusContainer>

    @POST("api/sos/{eventId}/respond")
    suspend fun respondToSos(
        @Path("eventId") eventId: String
    ): Response<Unit>

    @GET("api/sos/{eventId}/playbook")
    suspend fun getPlaybook(
        @Path("eventId") eventId: String
    ): Response<com.as307.aryaa.data.remote.dto.PlaybookDto>

    @GET("api/sos/active-incoming")
    suspend fun getActiveIncoming(): Response<com.as307.aryaa.data.remote.dto.ActiveIncomingSosResponse>

    @POST("api/location-share/start")
    suspend fun startLocationShare(
        @Body request: com.as307.aryaa.data.remote.dto.LocationShareStartRequest
    ): Response<com.as307.aryaa.data.remote.dto.LocationShareStartResponse>

    @POST("api/location-share/{sessionId}/update")
    suspend fun updateLocationShare(
        @Path("sessionId") sessionId: String,
        @Body request: com.as307.aryaa.data.remote.dto.LocationShareUpdateRequest
    ): Response<Unit>

    @POST("api/location-share/{sessionId}/stop")
    suspend fun stopLocationShare(
        @Path("sessionId") sessionId: String
    ): Response<Unit>

    @GET("api/nearby-services")
    suspend fun getNearbyServices(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("type") type: String
    ): Response<List<NearbyServiceResponse>>

    @POST("api/users/profile-address")
    suspend fun updateProfileAddress(
        @Body request: ProfileAddressRequest
    ): Response<Unit>

    @POST("api/safety-reports")
    suspend fun submitSafetyReport(
        @Body request: SafetyReportRequest
    ): Response<Unit>

    @GET("api/safety-map/pins")
    suspend fun getSafetyMapPins(): Response<List<SafetyMapPin>>

    @POST("api/safety-reports/{id}/dispute")
    suspend fun disputeSafetyReport(
        @Path("id") reportId: String
    ): Response<Unit>

    @GET("api/safety-reports/me")
    suspend fun getMySafetyReports(): Response<List<com.as307.aryaa.data.remote.dto.SafetyReport>>

    @DELETE("api/safety-reports/{id}")
    suspend fun deleteSafetyReport(
        @Path("id") reportId: String
    ): Response<Unit>
}
