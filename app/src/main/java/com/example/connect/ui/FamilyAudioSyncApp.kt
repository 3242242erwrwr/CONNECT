package com.example.connect.ui

import android.os.Environment
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class AudioRecordItem(
    val id: String,
    val senderName: String,
    val phoneModel: String,
    val duration: String,
    val timestamp: String,
    val date: String = "2026-10-05",
    val isSynced: Boolean = true
)

@Composable
fun FamilyAudioSyncApp() {
    var activeAccount by remember { mutableStateOf("0087abbos@gmail.com") }
    var isSyncing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val audioList = remember {
        mutableStateListOf(
            AudioRecordItem("1", "HUAWEI nova 13i", "Test_Call_Record_2.m4a (Suhbat Yozuvi #2)", "01:15", "23:20"),
            AudioRecordItem("2", "HUAWEI nova 13i", "Test_Call_Record_1.m4a (Suhbat Yozuvi #1)", "00:45", "23:11"),
            AudioRecordItem("3", "HUAWEI nova 13i", "Call_Record_20261005_221400.m4a", "02:14", "22:14"),
            AudioRecordItem("4", "HUAWEI nova 13i", "Call_Record_20261005_213000.m4a", "01:30", "21:30")
        )
    }

    // Function to scan local storage
    fun scanLocalCallRecordings() {
        val foundItems = mutableListOf<AudioRecordItem>()

        val directoriesToScan = listOf(
            File(Environment.getExternalStorageDirectory(), "Sounds/CallRecord"),
            File(Environment.getExternalStorageDirectory(), "Recordings/Call"),
            File(Environment.getExternalStorageDirectory(), "CallRecord"),
            File(Environment.getExternalStorageDirectory(), "Recordings"),
            File("/sdcard/Sounds/CallRecord"),
            File("/sdcard/Recordings")
        )

        var idCounter = 1
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        directoriesToScan.forEach { dir ->
            try {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && (file.extension.equals("m4a", true) || file.extension.equals("mp3", true) || file.extension.equals("3gp", true) || file.extension.equals("amr", true))) {
                            val lastMod = Date(file.lastModified())
                            val sizeKb = file.length() / 1024
                            foundItems.add(
                                AudioRecordItem(
                                    id = (idCounter++).toString(),
                                    senderName = "HUAWEI nova 13i",
                                    phoneModel = file.name,
                                    duration = "${if (sizeKb > 0) sizeKb else 48} KB",
                                    timestamp = sdfTime.format(lastMod),
                                    date = sdfDate.format(lastMod)
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore permission error during fallback
            }
        }

        if (foundItems.isNotEmpty()) {
            audioList.clear()
            audioList.addAll(foundItems)
        }
    }

    LaunchedEffect(Unit) {
        scanLocalCallRecordings()
    }

    val filteredList = remember(searchQuery, audioList.toList()) {
        audioList.filter {
            searchQuery.isEmpty() ||
                    it.phoneModel.contains(searchQuery, ignoreCase = true) ||
                    it.senderName.contains(searchQuery, ignoreCase = true)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F1116)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎙 Connect — Call Sync v3.0",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "HUAWEI nova 13i va Honor X8a o'rtasida masofaviy audio sinxronlash",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .border(1.dp, Color(0xFF2A3242), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Text("Avto-Sync: Faol", color = Color.LightGray, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Account Status & Refresh Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2E3544), RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sinxronlangan Bulut Akkaunt:", color = Color.Gray, fontSize = 11.sp)
                        Text(activeAccount, color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            isSyncing = true
                            scanLocalCallRecordings()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isSyncing) "⌛ Izlanmoqda..." else "🔄 Yangilash / Sync",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isSyncing) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1000)
                    isSyncing = false
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Audio yozuvni nomi bo'yicha izlash...", color = Color.Gray, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    unfocusedBorderColor = Color(0xFF2A3142),
                    focusedContainerColor = Color(0xFF14171F),
                    unfocusedContainerColor = Color(0xFF14171F),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Muloqot Audio Yozuvlari (${filteredList.size} ta):",
                color = Color.LightGray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Audio Records List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList) { item ->
                    AudioCard(item = item)
                }
            }
        }
    }
}

@Composable
fun AudioCard(item: AudioRecordItem) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    if (isPlaying) {
        LaunchedEffect(isPlaying) {
            while (progress < 1f && isPlaying) {
                kotlinx.coroutines.delay(200)
                progress += 0.05f
            }
            if (progress >= 1f) {
                isPlaying = false
                progress = 0f
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D26)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isPlaying) 1.5.dp else 1.dp,
                color = if (isPlaying) Color(0xFFE53935) else Color(0xFF282E3D)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color(0xFFE53935) else Color(0xFF2A3242))
                            .clickable {
                                isPlaying = !isPlaying
                                if (!isPlaying) progress = 0f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }

                    Column {
                        Text(
                            text = "${item.senderName} — ${item.phoneModel}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Sana: ${item.date}  |  Vaqti: ${item.timestamp}  |  Hajmi: ${item.duration}",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Sinxronlandi", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFFE53935),
                    trackColor = Color(0xFF2B3242)
                )
            }
        }
    }
}
