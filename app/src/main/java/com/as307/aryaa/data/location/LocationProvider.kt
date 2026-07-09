package com.as307.aryaa.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

interface LocationProvider {
    suspend fun getLastKnownLocation(): Location?
}

@Singleton
class LocationProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationProvider {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override suspend fun getLastKnownLocation(): Location? {
        // Check permissions
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return null
        }

        // Try getting fresh high accuracy location first
        val freshLocation = getFreshHighAccuracyLocation()
        if (freshLocation != null) {
            return freshLocation
        }

        // Fallback to cached lastLocation
        return getCachedLastLocation()
    }

    private suspend fun getFreshHighAccuracyLocation(): Location? {
        return withTimeoutOrNull(5000) {
            suspendCancellableCoroutine { continuation ->
                val cts = com.google.android.gms.tasks.CancellationTokenSource()
                continuation.invokeOnCancellation {
                    cts.cancel()
                }
                try {
                    val request = com.google.android.gms.location.CurrentLocationRequest.Builder()
                        .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                        .setDurationMillis(5000)
                        .build()
                    fusedLocationClient.getCurrentLocation(request, cts.token)
                        .addOnCompleteListener { task ->
                            if (continuation.isActive) {
                                if (task.isSuccessful && task.result != null) {
                                    val location = task.result
                                    val isMock = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                        location?.isMock == true
                                    } else {
                                        @Suppress("DEPRECATION")
                                        location?.isFromMockProvider == true
                                    }
                                    android.util.Log.d("LOCATION_DEBUG", "getCurrentLocation fresh raw: " + 
                                        location?.latitude + ", " + location?.longitude + 
                                        ", accuracy=" + location?.accuracy + "m" +
                                        ", provider=" + location?.provider + 
                                        ", time=" + location?.time + 
                                        ", isFromMockProvider=" + isMock)
                                    continuation.resume(location)
                                } else {
                                    android.util.Log.d("LOCATION_DEBUG", "getCurrentLocation task failed or returned null")
                                    continuation.resume(null)
                                }
                            }
                        }
                } catch (e: SecurityException) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }

    private suspend fun getCachedLastLocation(): Location? {
        return withTimeoutOrNull(5000) {
            suspendCancellableCoroutine { continuation ->
                try {
                    fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                        if (continuation.isActive) {
                            if (task.isSuccessful && task.result != null) {
                                val location = task.result
                                val isMock = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    location?.isMock == true
                                } else {
                                    @Suppress("DEPRECATION")
                                    location?.isFromMockProvider == true
                                }
                                android.util.Log.d("LOCATION_DEBUG", "getCachedLastLocation raw: " + 
                                    location?.latitude + ", " + location?.longitude + 
                                    ", accuracy=" + location?.accuracy + "m" +
                                    ", provider=" + location?.provider + 
                                    ", time=" + location?.time + 
                                    ", isFromMockProvider=" + isMock)
                                continuation.resume(location)
                            } else {
                                android.util.Log.d("LOCATION_DEBUG", "getCachedLastLocation task failed or returned null")
                                continuation.resume(null)
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }
}
