package com.RobPlow.BoschEbikeMonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.RobPlow.BoschEbikeMonitor.bluetooth.BoschBluetoothManager
import com.RobPlow.BoschEbikeMonitor.ui.MainScreen
import com.RobPlow.BoschEbikeMonitor.ui.theme.BoschEBikeMonitorTheme
import com.RobPlow.BoschEbikeMonitor.utils.PermissionHelper

class MainActivity : ComponentActivity() {
    
    private lateinit var bluetoothManager: BoschBluetoothManager
    private lateinit var permissionHelper: PermissionHelper
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permissions granted - BluetoothManager will handle the rest
        } else {
            // Handle permission denial
            // You might want to show a dialog explaining why permissions are needed
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionHelper = PermissionHelper(this)
        bluetoothManager = BoschBluetoothManager(this)
        
        // Request permissions if not already granted
        if (!permissionHelper.hasAllPermissions()) {
            requestPermissionLauncher.launch(permissionHelper.getRequiredPermissions())
        }
        
        setContent {
            BoschEBikeMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(bluetoothManager)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnect()
    }
}