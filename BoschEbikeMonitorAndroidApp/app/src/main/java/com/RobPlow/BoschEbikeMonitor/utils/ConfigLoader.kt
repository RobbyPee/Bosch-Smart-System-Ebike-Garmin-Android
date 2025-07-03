package com.RobPlow.BoschEbikeMonitor.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.RobPlow.BoschEbikeMonitor.models.BikeConfig
import java.io.File
import java.io.IOException

class ConfigLoader(private val context: Context) {
    private val gson = Gson()
    
    companion object {
        private const val TAG = "ConfigLoader"
        private const val CONFIG_FILENAME = "bike_config.json"
        private const val EXAMPLE_CONFIG_FILENAME = "config/bike_config.example.json"
        private const val CONFIG_PATH = "config/$CONFIG_FILENAME"
    }
    
    fun loadConfig(): BikeConfig? {
        // First try to load from internal storage (user saved config)
        loadConfigFromInternalStorage()?.let { return it }
        
        // Then try to load actual config from assets
        return try {
            loadConfigFromAssets(CONFIG_PATH)
        } catch (e: Exception) {
            Log.w(TAG, "Could not load $CONFIG_PATH, trying example config", e)
            try {
                // Fallback to example config
                loadConfigFromAssets(EXAMPLE_CONFIG_FILENAME)
            } catch (e: Exception) {
                Log.e(TAG, "Could not load any config file", e)
                null
            }
        }
    }
    
    private fun loadConfigFromAssets(filename: String): BikeConfig {
        val inputStream = context.assets.open(filename)
        val json = inputStream.bufferedReader().use { it.readText() }
        return gson.fromJson(json, BikeConfig::class.java)
            ?: throw JsonSyntaxException("Config file is empty or invalid")
    }
    
    fun saveConfig(config: BikeConfig): Boolean {
        return try {
            val file = File(context.filesDir, CONFIG_FILENAME)
            val json = gson.toJson(config)
            file.writeText(json)
            Log.d(TAG, "Config saved to internal storage")
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error saving config to internal storage", e)
            false
        }
    }
    
    private fun loadConfigFromInternalStorage(): BikeConfig? {
        return try {
            val file = File(context.filesDir, CONFIG_FILENAME)
            if (file.exists()) {
                val json = file.readText()
                gson.fromJson(json, BikeConfig::class.java)
            } else {
                Log.d(TAG, "No config file found in internal storage")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading config from internal storage", e)
            null
        }
    }
    
    fun validateConfig(config: BikeConfig): List<String> {
        val errors = mutableListOf<String>()
        
        // Validate MAC address format
        val macRegex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
        if (!macRegex.matches(config.bike.macAddress)) {
            errors.add("Invalid MAC address format: ${config.bike.macAddress}")
        }
        
        // Validate UUIDs
        try {
            java.util.UUID.fromString(config.bluetooth.services.statusServiceUuid)
        } catch (e: IllegalArgumentException) {
            errors.add("Invalid service UUID format")
        }
        
        try {
            java.util.UUID.fromString(config.bluetooth.services.statusCharacteristicUuid)
        } catch (e: IllegalArgumentException) {
            errors.add("Invalid characteristic UUID format")
        }
        
        // Validate patterns
        if (config.dataParsing.batteryPattern.isEmpty()) {
            errors.add("Battery pattern cannot be empty")
        }
        
        if (config.dataParsing.assistPattern.isEmpty()) {
            errors.add("Assist pattern cannot be empty")
        }
        
        return errors
    }
}