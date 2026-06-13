package com.ma7ee7.placereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationCheckerService : Service() {
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private lateinit var store: ReminderStore
    private var checkerStarted = false

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        store = ReminderStore(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(SERVICE_NOTIFICATION_ID, Notifications.serviceNotification(this))

        if (!checkerStarted) {
            checkerStarted = true
            startCheckerLoop()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCheckerLoop() {
        scope.launch {
            while (isActive) {
                delayUntilNextTenMinuteMark()
                checkLocationOnce()
            }
        }
    }

    private suspend fun delayUntilNextTenMinuteMark() {
        val now = System.currentTimeMillis()
        val tenMinutes = 10 * 60 * 1000L
        val nextMark = ((now / tenMinutes) + 1) * tenMinutes
        val waitMs = (nextMark - now).coerceAtLeast(1_000L)
        delay(waitMs)
    }

    private suspend fun checkLocationOnce() {
        if (!hasLocationPermission()) return

        val location = getSingleLocation() ?: return
        val reminders = store.getAll().filter { it.enabled && !it.triggered }

        for (reminder in reminders) {
            val distance = distanceMeters(
                fromLat = location.latitude,
                fromLon = location.longitude,
                toLat = reminder.latitude,
                toLon = reminder.longitude
            )

            if (distance <= Reminder.RADIUS_METERS) {
                try {
                    Notifications.showReminder(this, reminder, distance)
                    store.markTriggered(reminder.id)
                } catch (_: SecurityException) {
                    // Notification permission may have been revoked. Do not crash the service.
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getSingleLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cancellationToken = CancellationTokenSource()
        continuation.invokeOnCancellation { cancellationToken.cancel() }

        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationToken.token)
            .addOnSuccessListener { currentLocation ->
                if (currentLocation != null) {
                    if (continuation.isActive) continuation.resume(currentLocation)
                } else {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { lastLocation ->
                            if (continuation.isActive) continuation.resume(lastLocation)
                        }
                        .addOnFailureListener {
                            if (continuation.isActive) continuation.resume(null)
                        }
                }
            }
            .addOnFailureListener {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastLocation ->
                        if (continuation.isActive) continuation.resume(lastLocation)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun distanceMeters(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(fromLat, fromLon, toLat, toLon, results)
        return results[0]
    }

    private companion object {
        const val SERVICE_NOTIFICATION_ID = 1001
    }
}
