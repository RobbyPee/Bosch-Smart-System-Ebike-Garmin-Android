package com.RobPlow.BoschEbikeMonitor.bluetooth

data class BikeData(
    val batteryLevel: Int? = null,
    val assistMode: Int? = null,
    val speed: Double? = null,
    val rawData: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getBatteryPercentage(): String {
        return batteryLevel?.toString() ?: "Unknown"
    }
    
    fun getAssistModeText(): String {
        return when (assistMode) {
            0 -> "Off"
            1 -> "Eco"
            2 -> "Tour"
            3 -> "Sport"
            4 -> "Turbo"
            else -> assistMode?.toString() ?: "Unknown"
        }
    }
    
    fun getSpeedText(): String {
        return speed?.let { "%.1f km/h".format(it) } ?: "Unknown"
    }
}

enum class ConnectionState {
    Disconnected,
    Scanning,
    Connecting,
    Connected,
    Error
}

data class ScanResult(
    val deviceName: String,
    val deviceAddress: String,
    val rssi: Int
)