package com.RobPlow.BoschEbikeMonitor.bluetooth

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.RobPlow.BoschEbikeMonitor.models.BikeConfig
import com.RobPlow.BoschEbikeMonitor.notifications.BikeNotificationManager
import com.RobPlow.BoschEbikeMonitor.utils.ConfigLoader
import com.RobPlow.BoschEbikeMonitor.utils.PermissionHelper
import java.util.*

class BoschBluetoothManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BoschBluetoothManager"
    }
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private val permissionHelper = PermissionHelper(context)
    private val configLoader = ConfigLoader(context)
    private val notificationManager = BikeNotificationManager(context)
    private val handler = Handler(Looper.getMainLooper())
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var config: BikeConfig? = null
    
    // State flows for UI
    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _bikeData = MutableStateFlow<BikeData?>(null)
    val bikeData: StateFlow<BikeData?> = _bikeData.asStateFlow()
    
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ScanResult>> = _scanResults.asStateFlow()
    
    private val _dataLog = MutableStateFlow<List<String>>(emptyList())
    val dataLog: StateFlow<List<String>> = _dataLog.asStateFlow()
    
    // Previous values for change detection
    private var previousBattery: Int? = null
    private var previousAssistMode: Int? = null
    
    init {
        loadConfiguration()
    }
    
    private fun loadConfiguration() {
        config = configLoader.loadConfig()
        if (config == null) {
            Log.e(TAG, "Failed to load configuration")
            _connectionState.value = ConnectionState.Error
        } else {
            Log.d(TAG, "Configuration loaded successfully")
        }
    }
    
    fun startScan() {
        if (!permissionHelper.hasAllPermissions()) {
            Log.e(TAG, "Missing required permissions")
            return
        }
        
        if (_connectionState.value == ConnectionState.Connected) {
            Log.w(TAG, "Already connected, ignoring scan request")
            return
        }
        
        _connectionState.value = ConnectionState.Scanning
        _scanResults.value = emptyList()
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0)
            .build()
        
        try {
            bluetoothLeScanner.startScan(emptyList(), scanSettings, scanCallback)
            Log.d(TAG, "BLE scan started")
            
            // Stop scanning after timeout
            val timeout = config?.bluetooth?.scanTimeoutMs ?: 15000L
            handler.postDelayed({
                stopScan()
            }, timeout)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan: ${e.message}")
            _connectionState.value = ConnectionState.Error
        }
    }
    
    fun stopScan() {
        try {
            bluetoothLeScanner.stopScan(scanCallback)
            Log.d(TAG, "BLE scan stopped")
            if (_connectionState.value == ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}")
        }
    }
    
    fun connectToDevice(deviceAddress: String) {
        if (!permissionHelper.hasAllPermissions()) {
            Log.e(TAG, "Missing required permissions")
            return
        }
        
        try {
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            _connectionState.value = ConnectionState.Connecting
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            Log.d(TAG, "Attempting to connect to: $deviceAddress")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to device: ${e.message}")
            _connectionState.value = ConnectionState.Error
        }
    }
    
    fun connectToConfiguredBike() {
        config?.bike?.macAddress?.let { macAddress ->
            connectToDevice(macAddress)
        } ?: run {
            Log.e(TAG, "No configured bike MAC address found")
        }
    }
    
    fun disconnect() {
        bluetoothGatt?.let { gatt ->
            if (permissionHelper.hasBluetoothPermissions()) {
                gatt.disconnect()
                gatt.close()
            }
            bluetoothGatt = null
        }
        _connectionState.value = ConnectionState.Disconnected
        _bikeData.value = null
        Log.d(TAG, "Disconnected from device")
    }
    
    fun clearDataLog() {
        _dataLog.value = emptyList()
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
            if (!permissionHelper.hasAllPermissions()) return
            
            val device = result.device
            val deviceName = device.name ?: "Unknown Device"
            val deviceAddress = device.address
            
            val scanResult = ScanResult(deviceName, deviceAddress, result.rssi)
            
            val currentResults = _scanResults.value.toMutableList()
            if (!currentResults.any { it.deviceAddress == deviceAddress }) {
                currentResults.add(scanResult)
                _scanResults.value = currentResults
                Log.d(TAG, "Found device: $deviceName ($deviceAddress) RSSI: ${result.rssi}")
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            _connectionState.value = ConnectionState.Error
        }
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Notifications enabled for characteristic")
                } else {
                    Log.e(TAG, "Required service or characteristic not found")
                    _connectionState.value = ConnectionState.Error
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
                _connectionState.value = ConnectionState.Error
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.let { char ->
                val data = char.value
                if (data != null && config != null) {
                    val bikeData = BoschDataParser.parseBoschData(data, config!!.dataParsing)
                    _bikeData.value = bikeData
                    
                    // Add to log
                    val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    val logEntry = "$timestamp [${data.size}b]: ${bikeData.rawData}"
                    
                    val currentLog = _dataLog.value.toMutableList()
                    currentLog.add(0, logEntry) // Add to beginning
                    
                    // Keep only last 50 entries
                    if (currentLog.size > 50) {
                        currentLog.removeAt(currentLog.size - 1)
                    }
                    _dataLog.value = currentLog
                    
                    // Send notifications for changes
                    bikeData.batteryLevel?.let { battery ->
                        if (previousBattery != battery && previousBattery != null) {
                            notificationManager.sendBatteryNotification(battery)
                        }
                        previousBattery = battery
                    }
                    
                    bikeData.assistMode?.let { assist ->
                        if (previousAssistMode != assist && previousAssistMode != null) {
                            notificationManager.sendAssistModeNotification(bikeData.getAssistModeText())
                        }
                        previousAssistMode = assist
                    }
                    
                    Log.d(TAG, "Data received: ${BoschDataParser.formatDataForDisplay(bikeData)}")
                }
            }
        }
        
        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Descriptor write successful - notifications should now work")
            } else {
                Log.e(TAG, "Descriptor write failed with status: $status")
            }
        }
    }
    
    fun getConfiguredBikeName(): String {
        return config?.bike?.name ?: "Unknown Bike"
    }
    
    fun getConfiguredBikeMac(): String {
        return config?.bike?.macAddress ?: "XX:XX:XX:XX:XX:XX"
    }
    
    fun isConfigurationValid(): Boolean {
        return config != null && configLoader.validateConfig(config!!).isEmpty()
    }
    
    fun getConfigurationErrors(): List<String> {
        return config?.let { configLoader.validateConfig(it) } ?: listOf("No configuration loaded")
    }
} "Connected to GATT server")
                    _connectionState.value = ConnectionState.Connected
                    notificationManager.sendConnectionNotification(true)
                    if (permissionHelper.hasBluetoothPermissions()) {
                        gatt?.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    _connectionState.value = ConnectionState.Disconnected
                    _bikeData.value = null
                    notificationManager.sendConnectionNotification(false)
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    Log.d(TAG, "Connecting to GATT server")
                    _connectionState.value = ConnectionState.Connecting
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && config != null) {
                val serviceUuid = UUID.fromString(config!!.bluetooth.services.statusServiceUuid)
                val characteristicUuid = UUID.fromString(config!!.bluetooth.services.statusCharacteristicUuid)
                
                val service = gatt?.getService(serviceUuid)
                val characteristic = service?.getCharacteristic(characteristicUuid)
                
                if (characteristic != null && permissionHelper.hasBluetoothPermissions()) {
                    // Enable notifications
                    gatt.setCharacteristicNotification(characteristic, true)
                    
                    val descriptor = characteristic.getDescriptor(
                        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                    )
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                    
                    Log.d(TAG,