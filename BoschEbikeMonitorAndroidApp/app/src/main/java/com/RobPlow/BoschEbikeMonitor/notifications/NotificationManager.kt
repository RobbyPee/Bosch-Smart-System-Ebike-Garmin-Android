package com.RobPlow.BoschEbikeMonitor.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.RobPlow.BoschEbikeMonitor.bluetooth.BikeData

class BikeNotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BikeNotificationManager"
        private const val CHANNEL_ID = "bosch_ebike_notifications"
        private const val NOTIFICATION_ID = 1
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Bosch eBike Status"
                val descriptionText = "Battery and assist mode updates from your eBike"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }

                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                
                Log.d(TAG, "Notification channel created successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification channel: ${e.message}")
        }
    }
    
    fun sendBatteryNotification(batteryLevel: Int) {
        val message = "🔋 Battery: $batteryLevel%"
        sendNotification("Battery Update", message)
    }
    
    fun sendAssistModeNotification(assistMode: String) {
        val message = "⚡ Assist Mode: $assistMode"
        sendNotification("Assist Mode Changed", message)
    }
    
    fun sendConnectionNotification(isConnected: Boolean) {
        val title = if (isConnected) "🚴 Connected" else "🔌 Disconnected"
        val message = if (isConnected) "Successfully connected to your Bosch eBike" else "Disconnected from your Bosch eBike"
        sendNotification(title, message)
    }
    
    fun sendDataNotification(data: BikeData) {
        val message = buildString {
            data.batteryLevel?.let { append("🔋 $it% ") }
            data.assistMode?.let { append("⚡ ${data.getAssistModeText()} ") }
            data.speed?.let { append("🏃 ${data.getSpeedText()}") }
        }.trim()
        
        if (message.isNotEmpty()) {
            sendNotification("🚴 Bike Status", message)
        }
    }
    
    private fun sendNotification(title: String, message: String) {
        try {
            if (!hasNotificationPermission()) {
                Log.w(TAG, "Notification permission not granted")
                return
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
            
            Log.d(TAG, "Notification sent: $title - $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}")
        }
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not needed for older versions
        }
    }
    
    fun cancelAllNotifications() {
        try {
            NotificationManagerCompat.from(context).cancelAll()
            Log.d(TAG, "All notifications cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notifications: ${e.message}")
        }
    }
}