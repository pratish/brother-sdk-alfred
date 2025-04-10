package com.pratishk.sdkalfred

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothPermissionScreen() {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
    } else {
        listOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        multiplePermissionsState.launchMultiplePermissionRequest()
    }

    when {
        multiplePermissionsState.allPermissionsGranted -> {
            Text("All required Bluetooth permissions granted")
        }

        multiplePermissionsState.shouldShowRationale -> {
            Column {
                Text("Bluetooth permissions are needed to scan/connect to Bluetooth Printers")
                Button(
                    onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }
                ) {
                    Text("Request Permission Again")
                }
            }
        }

        else -> {
            Text("Bluetooth permissions denied. Please enable them in settings.")
        }
    }
}