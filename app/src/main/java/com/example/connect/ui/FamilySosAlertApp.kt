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
    val isOutgoing: Boolean
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

    // Instant Zero-Cold-Start 4G/5G Cloud Relay Endpoints
    val openUzbekistanCloudUrl = "https://api.restful-api.dev/objects"
    val backupHttpbinUrl = "https://httpbin.org/post"
    val renderCloudUrl = "https://connect-sos-cloud.onrender.com/sos"
    val targetIps = listOf("192.168.100.146", "192.168.100.144", "192.168.43.1", "192.168.1.100")

    // Save Chat Messages to SharedPreferences
    fun saveChatMessagesToPrefs() {
        try {
            val jsonArray = JSONArray()
            chatMessages.forEach { msg ->
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("senderName", msg.senderName)
                    put("alertText", msg.alertText)
                    put("timestamp", msg.timestamp)
                    put("isOutgoing", msg.isOutgoing)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("saved_chat_messages", jsonArray.toString()).apply()
        } catch (e: Exception) {
            // Ignore JSON error
        }
    }

    // Load Saved Chat Messages on App Start
    fun loadChatMessagesFromPrefs() {
        try {
            val savedJson = prefs.getString("saved_chat_messages", null)
            if (!savedJson.isNullOrEmpty()) {
                val jsonArray = JSONArray(savedJson)
                val loadedList = mutableListOf<SosChatMessage>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    loadedList.add(
                        SosChatMessage(
                            id = obj.getString("id"),
                            senderName = obj.getString("senderName"),
                            alertText = obj.getString("alertText"),
                            timestamp = obj.getString("timestamp"),
                            isOutgoing = obj.getBoolean("isOutgoing")
                        )
                    )
                }
                chatMessages.clear()
                chatMessages.addAll(loadedList)
            } else {
                if (chatMessages.isEmpty()) {
                    chatMessages.add(SosChatMessage("1", "Oila A'zosi", "🚨 SAIDBEKKA QARA!", "02:18:12", isOutgoing = true))
                    chatMessages.add(SosChatMessage("2", "Oila A'zosi", "🔔 JASMINAHON QANI?", "02:10:10", isOutgoing = false))
                }
            }
        } catch (e: Exception) {
            // Ignore error
        }
    }

    LaunchedEffect(Unit) {
        loadChatMessagesFromPrefs()
    }

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
                .setContentTitle("🚨 4G / 5G INSTANT SOS SIGNAL!")
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

    // Play REAL AUTHENTIC SELECTED SIREN MUSIC ONLY on RECIPIENT device!
    fun triggerRecipientSiren(alertTitle: String, senderInfo: String = "Sinxronlangan Qurilma") {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeNow = sdf.format(Date())

        lastAlertText = alertTitle
        isAlertActive = true

        chatMessages.add(0, SosChatMessage(
            id = System.currentTimeMillis().toString(),
            senderName = senderInfo,
            alertText = alertTitle,
            timestamp = timeNow,
            isOutgoing = false
        ))
        saveChatMessagesToPrefs()

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

    // Universal 4G Mobile Data & Wi-Fi Multi-Carrier SOS & SMS Text Sender Engine (Instant Zero Cold Start)
    fun sendUdpSosAlert(alertText: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeNow = sdf.format(Date())

        val fullAlertMsg = "$alertText (Kimdan: $currentDeviceModel)"

        // Instant Touch Feedback
        Toast.makeText(context, "📡 4G SERVERGA YUBORILDI!", Toast.LENGTH_SHORT).show()

        // Add to sender chat list (NO sound played locally on sender!)
        chatMessages.add(0, SosChatMessage(
            id = System.currentTimeMillis().toString(),
            senderName = "$currentDeviceModel (Siz)",
            alertText = alertText,
            timestamp = timeNow,
            isOutgoing = true
        ))
        saveChatMessagesToPrefs()

        coroutineScope.launch(Dispatchers.IO) {
            // Channel 1: Open Instant 4G Cloud API (api.restful-api.dev)
            try {
                val url = URL(openUzbekistanCloudUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 1500
                conn.readTimeout = 1500
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Connection", "close")

                val jsonPayload = JSONObject().apply {
                    put("name", "CONNECT_SOS_FAMILY_CHANNEL")
                    put("data", JSONObject().apply {
                        put("alert", fullAlertMsg)
                        put("sender_id", myDeviceId)
                        put("sender_model", currentDeviceModel)
                        put("time", timeNow)
                    })
                }.toString()

                val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
                writer.write(jsonPayload)
                writer.flush()
                writer.close()
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {}

            // Channel 2: Backup Httpbin 4G Cloud Endpoint
            try {
                val url = URL(backupHttpbinUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 1200
                conn.readTimeout = 1200
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Connection", "close")

                val jsonPayload = JSONObject().apply {
                    put("alert", fullAlertMsg)
                    put("sender_id", myDeviceId)
                }.toString()

                val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
                writer.write(jsonPayload)
                writer.flush()
                writer.close()
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {}

            // Channel 3: Render Cloud Webhook
            try {
                val url = URL(renderCloudUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 1500
                conn.readTimeout = 1500
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Connection", "close")

                val jsonPayload = JSONObject().apply {
                    put("alert", fullAlertMsg)
                    put("sender", currentDeviceModel)
                }.toString()

                val writer = OutputStreamWriter(conn.outputStream, "UTF-8")
                writer.write(jsonPayload)
                writer.flush()
                writer.close()
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {}

            // Channel 4: Local Wi-Fi HTTP Requests
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

            // Channel 5: UDP Local Subnet Broadcast
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

    // 100% Instant 4G/5G Uzbekistan Cloud Poller (Polls Open 4G Cloud API every 1000ms)
    var lastReceivedCloudMsgId by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val url = URL(openUzbekistanCloudUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 2000
                    conn.readTimeout = 2000
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    conn.setRequestProperty("Cache-Control", "no-cache")

                    if (conn.responseCode == 200) {
                        val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                        val responseText = reader.readText()
                        reader.close()

                        val jsonArray = JSONArray(responseText)
                        if (jsonArray.length() > 0) {
                            val latestObj = jsonArray.getJSONObject(jsonArray.length() - 1)
                            val objId = latestObj.optString("id", "")
                            val dataObj = latestObj.optJSONObject("data")

                            if (dataObj != null) {
                                val alertText = dataObj.optString("alert", "")
                                val senderId = dataObj.optString("sender_id", "")
                                val senderModel = dataObj.optString("sender_model", "4G Qurilma")

                                if (objId != lastReceivedCloudMsgId && senderId != myDeviceId && alertText.isNotBlank()) {
                                    lastReceivedCloudMsgId = objId
                                    withContext(Dispatchers.Main) {
                                        triggerRecipientSiren(alertText, "4G Cloud Server ($senderModel)")
                                    }
                                }
                            }
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {}

                kotlinx.coroutines.delay(1000)
            }
        }
    }

    // Embedded HTTP Direct Server (Port 8080)
    DisposableEffect(Unit) {
        var isServerRunning = true
        var serverSocket: ServerSocket? = null

        coroutineScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(8080)
                while (isServerRunning && serverSocket?.isClosed == false) {
                    val client = serverSocket?.accept() ?: break
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val line = reader.readLine() ?: ""

                    if (line.contains("GET /sos")) {
                        val msgParam = line.substringAfter("msg=").substringBefore(" ").substringBefore("&")
                        val alertText = java.net.URLDecoder.decode(msgParam, "UTF-8")
                        val clientIp = client.inetAddress.hostAddress ?: "Qurilma"

                        withContext(Dispatchers.Main) {
                            triggerRecipientSiren(alertText, "Tarmoq ($clientIp)")
                        }

                        val out = client.getOutputStream()
                        out.write("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nOK".toByteArray())
                        out.flush()
                    }
                    client.close()
                }
            } catch (e: Exception) {}
        }

        onDispose {
            isServerRunning = false
            try { serverSocket?.close() } catch (e: Exception) {}
        }
    }

    // Persistent UDP Listener Service
    DisposableEffect(Unit) {
        var isListening = true
        var socket: DatagramSocket? = null
        var multicastLock: WifiManager.MulticastLock? = null

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            multicastLock = wifiManager.createMulticastLock("connect_sos_lock").apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {}

        coroutineScope.launch(Dispatchers.IO) {
            try {
                socket = DatagramSocket(8888)
                val buffer = ByteArray(1024)

                while (isListening && socket?.isClosed == false) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    val receivedMessage = String(packet.data, 0, packet.length)

                    if (receivedMessage.startsWith("SOS_PACKET::")) {
                        val parts = receivedMessage.split("::")
                        if (parts.size >= 3) {
                            val senderId = parts[1]
                            val alertMsg = parts[2]

                            if (senderId != myDeviceId) {
                                val senderIp = packet.address.hostAddress ?: "Qurilma"
                                withContext(Dispatchers.Main) {
                                    triggerRecipientSiren(alertMsg, "Tarmoq ($senderIp)")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }

        onDispose {
            isListening = false
            try { socket?.close() } catch (e: Exception) {}
            try { if (multicastLock?.isHeld == true) multicastLock.release() } catch (e: Exception) {}
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
                        text = "📱 $currentDeviceModel (4G/5G Server)",
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

            // Quick SOS Buttons Row 1 (HIGH-SENSITIVITY LARGE TOUCH TARGETS!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { sendUdpSosAlert("🚨 SAIDBEKKA QARA!") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("📢 SAIDBEKKA QARA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = { sendUdpSosAlert("🚨 JASMINAHON QANI?") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB8C00)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("🔔 JASMINAHON QANI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick SOS Buttons Row 2
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
                        .height(34.dp)
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
                        .height(34.dp)
                ) {
                    Text("📞 TELNI KO'RING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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

            // Compact List-Style Chat Messages List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(chatMessages) { msg ->
                    CompactChatBubbleCard(msg = msg)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // PERMANENT ALWAYS-VISIBLE PROMINENT STOP BUTTON AT THE VERY BOTTOM OF THE SCREEN!
            Button(
                onClick = { handleStopSirena() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "🛑 STOP SIRENA (OVOZNI TO'XTATISH)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
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
                            Text(if (selectedSoundType == RingtoneManager.TYPE_NOTIFICATION) "🔘 " else "⚪ ", fontSize = 13.sp)
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
fun CompactChatBubbleCard(msg: SosChatMessage) {
    val alignment = if (msg.isOutgoing) Alignment.End else Alignment.Start
    val bubbleColor = if (msg.isOutgoing) Color(0xFF1565C0) else Color(0xFF262A38)
    val senderLabel = if (msg.isOutgoing) "📤 Siz" else "📥 ${msg.senderName}"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = RoundedCornerShape(
                topStart = 8.dp,
                topEnd = 8.dp,
                bottomStart = if (msg.isOutgoing) 8.dp else 2.dp,
                bottomEnd = if (msg.isOutgoing) 2.dp else 8.dp
            ),
            modifier = Modifier
                .widthIn(max = 260.dp)
                .border(
                    width = 1.dp,
                    color = if (msg.isOutgoing) Color(0xFF1E88E5) else Color(0xFF383E50),
                    shape = RoundedCornerShape(
                        topStart = 8.dp,
                        topEnd = 8.dp,
                        bottomStart = if (msg.isOutgoing) 8.dp else 2.dp,
                        bottomEnd = if (msg.isOutgoing) 2.dp else 8.dp
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = senderLabel,
                        color = if (msg.isOutgoing) Color(0xFF90CAF9) else Color(0xFFFFB74D),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = msg.alertText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = msg.timestamp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp
                )
            }
        }
    }
}
