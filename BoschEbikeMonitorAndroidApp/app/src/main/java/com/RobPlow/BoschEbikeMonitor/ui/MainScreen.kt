package com.RobPlow.BoschEbikeMonitor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.RobPlow.BoschEbikeMonitor.bluetooth.BoschBluetoothManager
import com.RobPlow.BoschEbikeMonitor.bluetooth.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(bluetoothManager: BoschBluetoothManager) {
    val connectionState by bluetoothManager.connectionState.collectAsStateWithLifecycle()
    val bikeData by bluetoothManager.bikeData.collectAsStateWithLifecycle()
    val scanResults by bluetoothManager.scanResults.collectAsStateWithLifecycle()
    val dataLog by bluetoothManager.dataLog.collectAsStateWithLifecycle()
    
    var manualMacAddress by remember { mutableStateOf(bluetoothManager.getConfiguredBikeMac()) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bosch eBike Monitor",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Connection Status Card
        ConnectionStatusCard(
            connectionState = connectionState,
            configuredBikeName = bluetoothManager.getConfiguredBikeName(),
            configuredBikeMac = bluetoothManager.getConfiguredBikeMac()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control Buttons
        ControlButtons(
            bluetoothManager = bluetoothManager,
            connectionState = connectionState,
            manualMacAddress = manualMacAddress,
            onMacAddressChange = { manualMacAddress = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Live Bike Data (only when connected)
        if (connectionState == ConnectionState.Connected && bikeData != null) {
            LiveBikeDataCard(bikeData = bikeData!!)
            Spacer(modifier = Modifier.height(16.dp))
            
            DataLogCard(
                dataLog = dataLog,
                onClearLog = { bluetoothManager.clearDataLog() }
            )
        }
        
        // Scan Results (when scanning or scan complete)
        if (scanResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            ScanResultsCard(
                scanResults = scanResults,
                onDeviceClick = { deviceAddress ->
                    if (connectionState != ConnectionState.Connected) {
                        bluetoothManager.connectToDevice(deviceAddress)
                    }
                }
            )
        }
    }
}

@Composable
fun ConnectionStatusCard(
    connectionState: ConnectionState,
    configuredBikeName: String,
    configuredBikeMac: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                ConnectionState.Connecting, ConnectionState.Scanning -> MaterialTheme.colorScheme.secondaryContainer
                ConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceVariant
                ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Status: ${connectionState.name}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Bike: $configuredBikeName",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "MAC: $configuredBikeMac",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ControlButtons(
    bluetoothManager: BoschBluetoothManager,
    connectionState: ConnectionState,
    manualMacAddress: String,
    onMacAddressChange: (String) -> Unit
) {
    // Main control buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { bluetoothManager.connectToConfiguredBike() },
            enabled = connectionState == ConnectionState.Disconnected
        ) {
            Text("Connect to Bike")
        }
        
        Button(
            onClick = { bluetoothManager.startScan() },
            enabled = connectionState == ConnectionState.Disconnected
        ) {
            Text("Scan Devices")
        }
        
        Button(
            onClick = { bluetoothManager.disconnect() },
            enabled = connectionState == ConnectionState.Connected
        ) {
            Text("Disconnect")
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Manual MAC Address Input
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Direct Connection:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = manualMacAddress,
                onValueChange = onMacAddressChange,
                label = { Text("MAC Address") },
                placeholder = { Text("XX:XX:XX:XX:XX:XX") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { bluetoothManager.connectToDevice(manualMacAddress) },
                enabled = connectionState == ConnectionState.Disconnected && manualMacAddress.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect to This MAC")
            }
        }
    }
}

@Composable
fun LiveBikeDataCard(bikeData: com.RobPlow.BoschEbikeMonitor.bluetooth.BikeData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🚴 Live Bike Status",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "⚡ Assist Mode: ${bikeData.getAssistModeText()}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "🔋 Battery: ${bikeData.getBatteryPercentage()}",
                style = MaterialTheme.typography.titleMedium
            )
            
            bikeData.speed?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🏃 Speed: ${bikeData.getSpeedText()}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "📡 Raw Data: ${bikeData.rawData}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DataLogCard(
    dataLog: List<String>,
    onClearLog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 Data Log:",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(
                    onClick = onClearLog,
                    modifier = Modifier.size(80.dp, 32.dp)
                ) {
                    Text("Clear", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.height(200.dp)
            ) {
                items(dataLog) { logEntry ->
                    Text(
                        text = logEntry,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScanResultsCard(
    scanResults: List<com.RobPlow.BoschEbikeMonitor.bluetooth.ScanResult>,
    onDeviceClick: (String) -> Unit
) {
    Text(
        text = "Found Devices:",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        items(scanResults) { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { onDeviceClick(result.deviceAddress) }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = result.deviceName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = result.deviceAddress,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "RSSI: ${result.rssi} dBm",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}