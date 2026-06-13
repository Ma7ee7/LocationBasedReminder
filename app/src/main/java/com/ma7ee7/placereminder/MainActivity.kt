package com.ma7ee7.placereminder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.createChannels(this)

        setContent {
            PlaceReminderApp()
        }
    }
}

@Composable
fun PlaceReminderApp() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            ReminderScreen()
        }
    }
}

@Composable
private fun ReminderScreen() {
    val context = LocalContext.current
    val store = remember { ReminderStore(context) }

    var reminders by remember { mutableStateOf(store.getAll()) }
    var title by remember { mutableStateOf("") }
    var latitudeText by remember { mutableStateOf("") }
    var longitudeText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Checker stopped. Add a place, then start the checker.") }

    fun refresh() {
        reminders = store.getAll()
    }

    fun startChecker() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, LocationCheckerService::class.java)
        )
        statusText = "Checker running. It checks at :00, :10, :20, :30, :40, and :50."
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            hasLocationPermission(context)

        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            grants[Manifest.permission.POST_NOTIFICATIONS] == true || hasNotificationPermission(context)
        } else {
            true
        }

        if (locationGranted && notificationGranted) {
            startChecker()
        } else {
            statusText = "Permission missing. Location and notifications are needed for this app."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Place Reminder",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text("Each reminder uses a fixed 100 meter zone around the coordinates you enter.")

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Reminder text") },
            placeholder = { Text("Buy thermal paste") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = latitudeText,
            onValueChange = { latitudeText = it.trim() },
            label = { Text("Latitude") },
            placeholder = { Text("40.712776") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = longitudeText,
            onValueChange = { longitudeText = it.trim() },
            label = { Text("Longitude") },
            placeholder = { Text("-74.005974") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                val lat = latitudeText.toDoubleOrNull()
                val lon = longitudeText.toDoubleOrNull()

                when {
                    lat == null || lon == null -> {
                        statusText = "Bad coordinates. Paste decimal latitude and longitude."
                    }
                    lat !in -90.0..90.0 -> {
                        statusText = "Latitude must be between -90 and 90."
                    }
                    lon !in -180.0..180.0 -> {
                        statusText = "Longitude must be between -180 and 180."
                    }
                    else -> {
                        store.add(title = title, latitude = lat, longitude = lon)
                        title = ""
                        latitudeText = ""
                        longitudeText = ""
                        statusText = "Saved. This place has a 100 meter trigger zone."
                        refresh()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add 100m Place Reminder")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (hasLocationPermission(context) && hasNotificationPermission(context)) {
                        startChecker()
                    } else {
                        permissionLauncher.launch(requiredPermissions())
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Start")
            }

            Button(
                onClick = {
                    context.stopService(Intent(context, LocationCheckerService::class.java))
                    statusText = "Checker stopped."
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop")
            }
        }

        Text(statusText)

        HorizontalDivider()

        Text(
            text = "Saved reminders",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onDelete = {
                        store.delete(reminder.id)
                        refresh()
                    },
                    onToggle = {
                        store.toggleEnabled(reminder.id)
                        refresh()
                    },
                    onReset = {
                        store.resetTriggered(reminder.id)
                        refresh()
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(reminder.title, fontWeight = FontWeight.Bold)
            Text("Lat: ${reminder.latitude}, Lon: ${reminder.longitude}")
            Text("Zone: 100 meters")
            Text(
                when {
                    reminder.triggered -> "Status: triggered once"
                    reminder.enabled -> "Status: enabled"
                    else -> "Status: disabled"
                }
            )

            Spacer(Modifier.height(2.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onToggle) {
                    Text(if (reminder.enabled) "Disable" else "Enable")
                }
                TextButton(onClick = onReset) {
                    Text("Reset")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

private fun requiredPermissions(): Array<String> {
    val permissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions += Manifest.permission.POST_NOTIFICATIONS
    }

    return permissions.toTypedArray()
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
