package com.example.connect.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class SosBackgroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private val cloudRelayBase = "https://ntfy.sh/connect_family_sos_global_channel_2026"
    private var lastReceivedMsg = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        start4GCloudListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "sos_4g_background_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Connect 4G SOS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "4G/5G Mobil Internetda 24/7 SOS tinglovchi xizmat"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("📡 Connect 4G SOS Network Faol")
            .setContentText("4G/5G Mobil internetda uzluksiz ishlamoqda...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(9999, notification)
    }

    private fun start4GCloudListener() {
        val currentDeviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

        serviceScope.launch {
            while (isActive) {
                try {
                    val url = URL("$cloudRelayBase/json")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 3000
                    conn.readTimeout = 3000
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Connection", "keep-alive")
                    conn.setRequestProperty("Cache-Control", "no-cache")

                    if (conn.responseCode == 200) {
                        val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            if (!line.isNullOrEmpty()) {
                                try {
                                    val jsonObj = JSONObject(line)
                                    val eventType = jsonObj.optString("event", "")
                                    val messageText = jsonObj.optString("message", "")

                                    if (eventType == "message" && messageText.isNotBlank()) {
                                        if (messageText != lastReceivedMsg && !messageText.contains("Kimdan: $currentDeviceModel")) {
                                            lastReceivedMsg = messageText
                                            triggerSirenAndNotification(messageText)
                                        }
                                    }
                                } catch (ex: Exception) {}
                            }
                        }
                        reader.close()
                    }
                    conn.disconnect()
                } catch (e: Exception) {}

                delay(1000)
            }
        }
    }

    private fun triggerSirenAndNotification(alertMsg: String) {
        // 1. Play Loud Alarm Siren Sound on 4G
        try {
            stopSirenSound()

            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                if (Build.VERSION.SDK_INT >= 21) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                }
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {}

        // 2. Full-Screen POPUP Activity to wake screen on 4G
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("alert_msg", alertMsg)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                8888,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = "sos_alarm_high_channel"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Shoshilinch Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setBypassDnd(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("🚨 4G MOBILE SOS SIGNAL!")
                .setContentText(alertMsg)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(8888, notification)
        } catch (e: Exception) {}
    }

    private fun stopSirenSound() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        } catch (e: Exception) {
            // Ignore error
        } finally {
            mediaPlayer = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopSirenSound()
    }
}
