package com.RobPlow.BoschEbikeMonitor.bluetooth

import android.util.Log
import com.RobPlow.BoschEbikeMonitor.models.DataParsingConfig

object BoschDataParser {
    private const val TAG = "BoschDataParser"
    
    fun parseBoschData(data: ByteArray, config: DataParsingConfig?): BikeData {
        val bytes = data.map { it.toInt() and 0xFF }
        val hexString = bytes.joinToString("-") { "%02X".format(it) }
        
        Log.d(TAG, "Parsing data: $hexString")
        
        return when (data.size) {
            6 -> parseShortPacket(data, bytes, config)
            7 -> parseShortPacket(data, bytes, config)
            in 20..35 -> parseLongPacket(data, bytes, config)
            else -> parseGenericPacket(data, bytes)
        }.copy(rawData = hexString)
    }
    
    private fun parseShortPacket(data: ByteArray, bytes: List<Int>, config: DataParsingConfig?): BikeData {
        Log.d(TAG, "Parsing short packet (${data.size} bytes)")
        
        // Look for assist mode in short packets
        val assistMode = config?.let { findAssistMode(bytes, it.assistPattern) }
        
        // Try to identify speed (often 2 bytes combined)
        val possibleSpeed1 = if (bytes.size >= 2) (bytes[0] shl 8) + bytes[1] else 0 // Big endian
        val possibleSpeed2 = if (bytes.size >= 2) (bytes[1] shl 8) + bytes[0] else 0 // Little endian
        val possibleSpeed3 = if (bytes.size >= 4) (bytes[2] shl 8) + bytes[3] else 0
        
        // Use most reasonable speed value (typically under 60 km/h for eBikes)
        val speed = listOf(
            possibleSpeed1 / 10.0,
            possibleSpeed2 / 10.0,
            possibleSpeed3 / 10.0
        ).firstOrNull { it in 0.0..60.0 }
        
        return BikeData(
            assistMode = assistMode,
            speed = speed
        )
    }
    
    private fun parseLongPacket(data: ByteArray, bytes: List<Int>, config: DataParsingConfig?): BikeData {
        Log.d(TAG, "Parsing long packet (${data.size} bytes)")
        
        val batteryLevel = config?.let { findBatteryLevel(bytes, it.batteryPattern) }
        val assistMode = config?.let { findAssistMode(bytes, it.assistPattern) }
        
        return BikeData(
            batteryLevel = batteryLevel,
            assistMode = assistMode
        )
    }
    
    private fun parseGenericPacket(data: ByteArray, bytes: List<Int>): BikeData {
        Log.d(TAG, "Parsing generic packet (${data.size} bytes)")
        return BikeData()
    }
    
    private fun findBatteryLevel(bytes: List<Int>, pattern: List<Int>): Int? {
        // Look for battery pattern in the byte array
        for (i in 0..bytes.size - pattern.size - 1) {
            val segment = bytes.subList(i, i + pattern.size)
            if (segment == pattern) {
                val batteryValue = bytes[i + pattern.size]
                Log.d(TAG, "Found battery pattern at position $i, value: $batteryValue")
                return batteryValue
            }
        }
        
        Log.d(TAG, "Battery pattern not found in: ${bytes.joinToString("-") { "%02X".format(it) }}")
        return null
    }
    
    private fun findAssistMode(bytes: List<Int>, pattern: List<Int>): Int? {
        // Look for assist pattern in the byte array
        for (i in 0..bytes.size - pattern.size - 1) {
            val segment = bytes.subList(i, i + pattern.size)
            if (segment == pattern) {
                val assistValue = bytes[i + pattern.size]
                Log.d(TAG, "Found assist pattern at position $i, value: $assistValue")
                return assistValue
            }
        }
        
        Log.d(TAG, "Assist pattern not found in: ${bytes.joinToString("-") { "%02X".format(it) }}")
        return null
    }
    
    fun formatDataForDisplay(data: BikeData): String {
        return buildString {
            appendLine("🚴 Bike Status:")
            appendLine("⚡ Assist: ${data.getAssistModeText()}")
            appendLine("🔋 Battery: ${data.getBatteryPercentage()}")
            data.speed?.let { appendLine("🏃 Speed: ${data.getSpeedText()}") }
            appendLine("📡 Raw: ${data.rawData}")
            appendLine("⏰ Time: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(data.timestamp))}")
        }
    }
}