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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class SosBackgroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: DatagramSocket? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        startUdpListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP_SIREN") {
            stopSirenSound()
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "sos_background_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Connect SOS Fon Xizmati",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SOS tarmoq xizmati fonda uzluksiz ishlamoqda"
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
            .setContentTitle("🚨 Connect SOS Network Faol")
            .setContentText("24/7 Fonda shoshilinch signallarni tinglamoqda...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(9999, notification)
    }

    private fun startUdpListener() {
        serviceScope.launch {
            try {
                serverSocket = DatagramSocket(8888)
                val buffer = ByteArray(1024)

                while (serverSocket?.isClosed == false) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    serverSocket?.receive(packet)
                    val receivedMessage = String(packet.data, 0, packet.length)

                    val myDeviceId = "CONNECT_DEVICE_ID"

                    if (receivedMessage.startsWith("SOS_PACKET::")) {
                        val parts = receivedMessage.split("::")
                        if (parts.size >= 3) {
                            val senderId = parts[1]
                            val alertMsg = parts[2]

                            if (senderId != myDeviceId) {
                                triggerSirenAndNotification(alertMsg)
                            }
                        }
                    } else if (receivedMessage.startsWith("SOS_ALERT:")) {
                        val alertMsg = receivedMessage.removePrefix("SOS_ALERT:")
                        if (!packet.address.isLoopbackAddress) {
                            triggerSirenAndNotification(alertMsg)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore socket bind errors if restarting
            }
        }
    }

    private fun triggerSirenAndNotification(alertMsg: String) {
        // 1. Play Loud Alarm Siren Sound
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
        } catch (e: Exception) {
            // Sound fallback
        }

        // 2. Full-Screen POPUP Activity to wake screen
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
                .setContentTitle("🚨 SHOSHILINCH SOS SIGNAL!")
                .setContentText(alertMsg)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(8888, notification)
        } catch (e: Exception) {
            // Ignore popup error
        }
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
        serverSocket?.close()
        stopSirenSound()
    }
}
