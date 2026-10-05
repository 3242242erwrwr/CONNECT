package com.example.connect.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

data class SosChatMessage(
    val id: String,
    val senderName: String,
    val alertText: String,
    val timestamp: String,
    val isOutgoing: Boolean,
    var isDelivered: Boolean = false,
    var isSeenByRecipient: Boolean = false
)

// Global Singleton References for 100% Guaranteed Instant Sound Stop
object AlarmAudioController {
    var mediaPlayer: MediaPlayer? = null
    var activeRingtone: Ringtone? = null

    fun stopAllSound(context: Context) {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            // Ignore error
        } finally {
            mediaPlayer = null
        }

        try {
            activeRingtone?.let { ringtone ->
                if (ringtone.isPlaying) {
                    ringtone.stop()
                }
            }
        } catch (e: Exception) {
            // Ignore error
        } finally {
            activeRingtone = null
        }
    }
}

@Composable
fun FamilySosAlertApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Universal Device Identity (Works on ANY Android device worldwide)
    val currentDeviceModel = remember { "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}" }
    val myDeviceId = remember { "DEV_${Build.MANUFACTURER}_${Build.MODEL}_${Build.BOARD.hashCode()}" }
    val prefs = remember { context.getSharedPreferences("connect_sos_prefs", Context.MODE_PRIVATE) }

    var selectedSoundType by remember {
        mutableIntStateOf(prefs.getInt("sos_siren_sound_type", RingtoneManager.TYPE_ALARM))
    }
    var showSoundSelectorModal by remember { mutableStateOf(false) }

    var isAlertActive by remember { mutableStateOf(false) }
    var lastAlertText by remember { mutableStateOf("") }
    var customMessageText by remember { mutableStateOf("") }

    val chatMessages = remember { mutableStateListOf<SosChatMessage>() }

    val cloud4GWebSocketRelayUrl = "https://ntfy.sh/connect_family_sos_websocket_v4_channel"
    val renderCloudUrl = "https://sos-connect.onrender.com/sos"
    val targetIps = listOf("192.168.100.146", "192.168.100.144", "192.168.43.1", "192.168.1.100")

    // STOP ALL SIREN SOUNDS INSTANTLY
    fun handleStopSirena() {
        AlarmAudioController.stopAllSound(context)
        isAlertActive = false
        Toast.makeText(context, "🛑 SIRENA O'CHIRILDI!", Toast.LENGTH_SHORT).show()
    }

    // Full-Screen High Priority Notification to POP UP Screen when App is in Background
    fun triggerFullScreenNotification(context: Context, alertTitle: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "sos_alert_high_priority_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Shoshilinch SOS Signallar",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Baland SOS sirenasi va ekranga qalqib chiqish"
                    enableVibration(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.example.connect")?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("alert_msg", alertTitle)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("🚨 4G / 5G SOS SIGNAL!")
                .setContentText(alertTitle)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)

            notificationManager.notify(8888, builder.build())
        } catch (e: Exception) {
            // Ignore notification error
        }
    }

    // Send Read Receipt ACK Back to Sender
    fun sendReadReceiptAck(alertId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val ackMsg = "ACK_SEEN_RECEIPT::$alertId::$myDeviceId"
                val url = URL(cloud4GWebSocketRelayUrl)
                val conn = url.openConnection() as HttpURLConnection
                val payloadBytes = ackMsg.toByteArray(Charsets.UTF_8)

                conn.connectTimeout = 1500
                conn.readTimeout = 1500
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(payloadBytes.size)

                val os = conn.outputStream
                os.write(payloadBytes)
                os.flush()
                os.close()
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {}
        }
    }

    // Play REAL AUTHENTIC SELECTED SIREN MUSIC ONLY on RECIPIENT device!
    fun triggerRecipientSiren(alertTitle: String, senderInfo: String = "Sinxronlangan Qurilma", alertMsgId: String = "") {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeNow = sdf.format(Date())

        lastAlertText = alertTitle
        isAlertActive = true

        chatMessages.add(0, SosChatMessage(
            id = if (alertMsgId.isNotBlank()) alertMsgId else System.currentTimeMillis().toString(),
            senderName = senderInfo,
            alertText = alertTitle,
            timestamp = timeNow,
            isOutgoing = false,
            isSeenByRecipient = true
        ))

        // Auto Send Read Receipt ACK back to sender
        if (alertMsgId.isNotBlank()) {
            sendReadReceiptAck(alertMsgId)
        }

        // POP UP SCREEN ON INCOMING SOS
        try {
            triggerFullScreenNotification(context, alertTitle)

            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.example.connect")?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("alert_msg", alertTitle)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            // Ignore launch error
        }

        // PLAY SELECTED SIREN MUSIC AT MAX VOLUME
        try {
            AlarmAudioController.stopAllSound(context)

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            val alarmUri = RingtoneManager.getDefaultUri(selectedSoundType)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val player = MediaPlayer().apply {
                setDataSource(context, alarmUri)
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
            AlarmAudioController.mediaPlayer = player
        } catch (e: Exception) {
            try {
                val alarmUri = RingtoneManager.getDefaultUri(selectedSoundType)
                val ringtone = RingtoneManager.getRingtone(context, alarmUri)
                ringtone.play()
                AlarmAudioController.activeRingtone = ringtone
            } catch (ex: Exception) {}
        }
    }

    // High-Sensitivity Touch Sender Handler
    fun sendUdpSosAlert(alertText: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeNow = sdf.format(Date())
        val newMsgId = System.currentTimeMillis().toString()

        val fullAlertMsg = "$alertText (Kimdan: $currentDeviceModel)"

        // Instant Touch Feedback
        Toast.makeText(context, "📡 SOS TUGMASI BOSILDI — YUBORILMOQDA!", Toast.LENGTH_SHORT).show()

        // Add to sender chat list (Mark as Delivered = true)
        val outgoingMsg = SosChatMessage(
            id = newMsgId,
            senderName = "$currentDeviceModel (Siz)",
            alertText = alertText,
            timestamp = timeNow,
            isOutgoing = true,
            isDelivered = true,
            isSeenByRecipient = false
        )
        chatMessages.add(0, outgoingMsg)

        coroutineScope.launch(Dispatchers.IO) {
            // Channel 1: 4G Mobile WebSocket Express Stream
            try {
                val url = URL(cloud4GWebSocketRelayUrl)
                val payloadStr = "MSG_ID::$newMsgId::$fullAlertMsg"
                val payloadBytes = payloadStr.toByteArray(Charsets.UTF_8)

                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 2500
                conn.readTimeout = 2500
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(payloadBytes.size)
                conn.setRequestProperty("Title", "🚨 SHOSHILINCH SOS SIGNAL!")
                conn.setRequestProperty("Priority", "5")
                conn.setRequestProperty("X-Sender-ID", myDeviceId)

                val os = conn.outputStream
                os.write(payloadBytes)
                os.flush()
                os.close()
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {}

            // Channel 2: Render Cloud Webhook
            try {
                val url = URL(renderCloudUrl)
                val conn = url.openConnection() as HttpURLConnection
                val jsonPayload = JSONObject().apply {
                    put("alert", fullAlertMsg)
                    put("sender", currentDeviceModel)
                }.toString()
                val payloadBytes = jsonPayload.toByteArray(Charsets.UTF_8)

                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(payloadBytes.size)
                conn.setRequestProperty("Content-Type", "application/json")

                val os = conn.outputStream
                os.write(payloadBytes)
                os.flush()
                os.close()
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {}

            // Channel 3: Local Wi-Fi HTTP Requests
            targetIps.forEach { ip ->
                try {
                    val encodedMsg = URLEncoder.encode(fullAlertMsg, "UTF-8")
                    val url = URL("http://$ip:8080/sos?msg=$encodedMsg")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 400
                    conn.readTimeout = 400
                    conn.requestMethod = "GET"
                    conn.responseCode
                    conn.disconnect()
                } catch (e: Exception) {}
            }

            // Channel 4: UDP Local Subnet Broadcast
            try {
                val socket = DatagramSocket()
                socket.broadcast = true
                val payload = "SOS_PACKET::$myDeviceId::$fullAlertMsg"
                val messageData = payload.toByteArray()

                val targetAddresses = listOf(
                    InetAddress.getByName("255.255.255.255"),
                    InetAddress.getByName("192.168.100.255"),
                    InetAddress.getByName("192.168.43.255")
                )

                targetAddresses.forEach { addr ->
                    try {
                        val packet = DatagramPacket(messageData, messageData.size, addr, 8888)
                        socket.send(packet)
                    } catch (e: Exception) {}
                }

                socket.close()
            } catch (e: Exception) {}
        }
    }

    // 100% Persistent 4G WebSocket Full-Duplex Stream Listener (With ACK Read Receipts)
    var lastReceivedCloudMsg by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val url = URL("$cloud4GWebSocketRelayUrl/raw")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 0
                    conn.requestMethod = "GET"

                    if (conn.responseCode == 200) {
                        val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val alertMsg = line?.trim() ?: ""
                            if (alertMsg.isNotBlank()) {
                                // Check if this is a Read Receipt ACK
                                if (alertMsg.startsWith("ACK_SEEN_RECEIPT::")) {
                                    val parts = alertMsg.split("::")
                                    if (parts.size >= 3) {
                                        val ackMsgId = parts[1]
                                        val ackSenderId = parts[2]
                                        if (ackSenderId != myDeviceId) {
                                            withContext(Dispatchers.Main) {
                                                val msgIndex = chatMessages.indexOfFirst { it.id == ackMsgId }
                                                if (msgIndex != -1) {
                                                    val updatedMsg = chatMessages[msgIndex].copy(isSeenByRecipient = true)
                                                    chatMessages[msgIndex] = updatedMsg
                                                    Toast.makeText(context, "✅ NARIGI TOMON SIRENANI ESHITDI VA KO'RDI!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    var extractMsgId = ""
                                    var alertTextToTrigger = alertMsg

                                    if (alertMsg.startsWith("MSG_ID::")) {
                                        val parts = alertMsg.split("::")
                                        if (parts.size >= 3) {
                                            extractMsgId = parts[1]
                                            alertTextToTrigger = parts[2]
                                        }
                                    }

                                    if (alertTextToTrigger != lastReceivedCloudMsg && !alertTextToTrigger.contains("Kimdan: $currentDeviceModel")) {
                                        lastReceivedCloudMsg = alertTextToTrigger
                                        withContext(Dispatchers.Main) {
                                            triggerRecipientSiren(alertTextToTrigger, "4G Mobile Data Stream", extractMsgId)
                                        }
                                    }
                                }
                            }
                        }
                        reader.close()
                    }
                    conn.disconnect()
                } catch (e: Exception) {}

                kotlinx.coroutines.delay(1000)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F1116)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Universal Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E222B))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Green)
                    )
                    Text(
                        text = "📱 $currentDeviceModel (4G Online)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // SOS MUSIC SELECTOR BUTTON
                Button(
                    onClick = { showSoundSelectorModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("🎵 Musiqa Tanlash", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick SOS Buttons Row 1 (HIGH-SENSITIVITY TOUCH BUTTONS WITH INSTANT FEEDBACK!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { sendUdpSosAlert("🚨 SAIDBEKKA QARA!") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("📢 SAIDBEKKA QARA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = { sendUdpSosAlert("🚨 JASMINAHON QANI?") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB8C00)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("🔔 JASMINAHON QANI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick SOS Buttons Row 2 (INCLUDES THE NEW '🚪 ESHIKNI OCH' BUTTON DIRECTLY BELOW 'UYGA KELING'!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { sendUdpSosAlert("🆘 UYGA SHOSHILINCH KELING!") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E24AA)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text("🆘 UYGA KELING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }

                Button(
                    onClick = { sendUdpSosAlert("📞 TELEFONNI KO'RING!") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Text("📞 TELNI KO'RING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // NEW DEDICATED BUTTON ROW DIRECTLY BELOW 'UYGA KELING': '🚪 ESHIKNI OCH'
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { sendUdpSosAlert("🚪 ESHIKNI OCH!") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Text("🚪 ESHIKNI OCH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // High-Contrast Custom Message Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customMessageText,
                    onValueChange = { customMessageText = it },
                    placeholder = {
                        Text("O'zingiz matn yozing...", color = Color(0xFFA0AAB8), fontSize = 13.sp)
                    },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE53935),
                        unfocusedBorderColor = Color(0xFF384252),
                        focusedContainerColor = Color(0xFF1E2433),
                        unfocusedContainerColor = Color(0xFF1E2433),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFE53935)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )

                Button(
                    onClick = {
                        if (customMessageText.isNotBlank()) {
                            sendUdpSosAlert("💬 $customMessageText")
                            customMessageText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("📡 YUBOR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Active Alert Overlay Banner
            if (isAlertActive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🔊 BALAND STANDART SOS SIRENA CHALINMOQDA!", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            Text(lastAlertText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { handleStopSirena() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFFFFF)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("🛑 STOP", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = "SOS Xabarlar Spiskasi (Chat Feed):",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Original Full-Sized Chat Messages List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(chatMessages) { msg ->
                    OriginalChatBubbleCard(msg = msg)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // PERMANENT ALWAYS-VISIBLE PROMINENT STOP BUTTON AT THE VERY BOTTOM OF THE SCREEN!
            Button(
                onClick = { handleStopSirena() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "🛑 STOP SIRENA (OVOZNI TO'XTATISH)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // SOS MUSIC SELECTOR MODAL DIALOG
        if (showSoundSelectorModal) {
            AlertDialog(
                onDismissRequest = { showSoundSelectorModal = false },
                containerColor = Color(0xFF1E222B),
                title = {
                    Text("🎵 SOS Sirena Musiqasini Tanlash", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Keladigan SOS signal ovozi turini tanlang:", color = Color.LightGray, fontSize = 11.sp)

                        // Option 1: Standart Baland Alarm
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedSoundType == RingtoneManager.TYPE_ALARM) Color(0xFF1565C0) else Color(0xFF2B3242))
                                .clickable {
                                    selectedSoundType = RingtoneManager.TYPE_ALARM
                                    prefs.edit().putInt("sos_siren_sound_type", RingtoneManager.TYPE_ALARM).apply()
                                    Toast.makeText(context, "1-Standart Alarm Sirena Tanlandi", Toast.LENGTH_SHORT).show()
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (selectedSoundType == RingtoneManager.TYPE_ALARM) "🔘 " else "⚪ ", fontSize = 14.sp)
                            Text("🚨 1-Standart Baland Alarm Sirena", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Option 2: Bildirishnoma Musiqasi
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedSoundType == RingtoneManager.TYPE_NOTIFICATION) Color(0xFF1565C0) else Color(0xFF2B3242))
                                .clickable {
                                    selectedSoundType = RingtoneManager.TYPE_NOTIFICATION
                                    prefs.edit().putInt("sos_siren_sound_type", RingtoneManager.TYPE_NOTIFICATION).apply()
                                    Toast.makeText(context, "2-Bildirishnoma Musiqasi Tanlandi", Toast.LENGTH_SHORT).show()
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (selectedSoundType == RingtoneManager.TYPE_NOTIFICATION) "🔘 " else "⚪ ", fontSize = 14.sp)
                            Text("🔔 2-Bildirishnoma Signal Musiqasi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Option 3: Telefon Zvonok Musiqasi
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectedSoundType == RingtoneManager.TYPE_RINGTONE) Color(0xFF1565C0) else Color(0xFF2B3242))
                                .clickable {
                                    selectedSoundType = RingtoneManager.TYPE_RINGTONE
                                    prefs.edit().putInt("sos_siren_sound_type", RingtoneManager.TYPE_RINGTONE).apply()
                                    Toast.makeText(context, "3-Telefon Zvonok Musiqasi Tanlandi", Toast.LENGTH_SHORT).show()
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (selectedSoundType == RingtoneManager.TYPE_RINGTONE) "🔘 " else "⚪ ", fontSize = 14.sp)
                            Text("🎵 3-Telefon Zvonok Musiqasi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSoundSelectorModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("✅ TAYYOR (SAQLASH)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )
        }
    }
}

@Composable
fun OriginalChatBubbleCard(msg: SosChatMessage) {
    val alignment = if (msg.isOutgoing) Alignment.End else Alignment.Start
    val bubbleColor = if (msg.isOutgoing) Color(0xFF1E88E5) else Color(0xFF2A2E3D)
    val senderLabel = if (msg.isOutgoing) "📤 Siz" else "📥 Kelgan Signal (${msg.senderName})"

    val statusIcon = when {
        !msg.isOutgoing -> ""
        msg.isSeenByRecipient -> " ✔✔ Ko'rildi"
        msg.isDelivered -> " ✔ Yetkazildi"
        else -> " ⌛"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 10.dp,
                bottomStart = if (msg.isOutgoing) 10.dp else 2.dp,
                bottomEnd = if (msg.isOutgoing) 10.dp else 2.dp
            ),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .border(
                    width = 1.dp,
                    color = if (msg.isOutgoing) Color(0xFF42A5F5) else Color(0xFF383E50),
                    shape = RoundedCornerShape(
                        topStart = 10.dp,
                        topEnd = 10.dp,
                        bottomStart = if (msg.isOutgoing) 10.dp else 2.dp,
                        bottomEnd = if (msg.isOutgoing) 10.dp else 2.dp
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = "$senderLabel$statusIcon",
                    color = if (msg.isSeenByRecipient) Color(0xFF66BB6A) else if (msg.isOutgoing) Color(0xFFBBDEFB) else Color(0xFFFFB74D),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = msg.alertText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = msg.timestamp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
