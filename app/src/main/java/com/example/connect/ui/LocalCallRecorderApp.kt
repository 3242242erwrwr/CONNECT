package com.example.connect.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LocalCallRecorderApp() {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentOutputFile by remember { mutableStateOf<File?>(null) }

    val recordingsList = remember { mutableStateListOf<File>() }

    fun refreshRecordings() {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        recordingsList.clear()
        storageDir?.listFiles()?.forEach { file ->
            if (file.isFile && (file.extension.equals("m4a", true) || file.extension.equals("mp3", true) || file.extension.equals("3gp", true))) {
                recordingsList.add(file)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshRecordings()
    }

    fun startRecording() {
        try {
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "REC_${sdf.format(Date())}.m4a"
            val outputFile = File(storageDir, fileName)
            currentOutputFile = outputFile

            @Suppress("DEPRECATION")
            val recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
            Toast.makeText(context, "Ovoz yozish boshlandi...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Xatolik: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            isRecording = false
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            Toast.makeText(context, "Yozib olindi va saqlandi!", Toast.LENGTH_SHORT).show()
            refreshRecordings()
        } catch (e: Exception) {
            isRecording = false
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎙 Local Call & Voice Recorder",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Standard Android AudioRecorder (Mahalliy xotirada saqlash)",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) Color.Red else Color.Green)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Control Box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2E3544), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRecording) "🔴 OVOZ YOZILMOQDA..." else "⚪ Yozishga tayyor",
                        color = if (isRecording) Color.Red else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (isRecording) stopRecording() else startRecording()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFE53935) else Color(0xFF2E7D32)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (isRecording) "⏹ STOP (To'xtatish va Saqlash)" else "🎙 START RECORDING (Yozishni Boshlash)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Mahalliy Saqlangan Audio Fayllar (${recordingsList.size} ta):",
                color = Color.LightGray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // List of Local Recordings
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(recordingsList) { file ->
                    LocalAudioItemCard(file = file, context = context)
                }
            }
        }
    }
}

@Composable
fun LocalAudioItemCard(file: File, context: Context) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun playAudio() {
        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    isPlaying = false
                }
            }
            mediaPlayer = player
            isPlaying = true
        } catch (e: Exception) {
            Toast.makeText(context, "Eshitishda xatolik!", Toast.LENGTH_SHORT).show()
            isPlaying = false
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        } catch (e: Exception) {
            isPlaying = false
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D26)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF282E3D), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (isPlaying) stopAudio() else playAudio()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaying) Color(0xFFE53935) else Color(0xFF2A3242)
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Text(text = if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 16.sp)
                }

                Column {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Hajmi: ${file.length() / 1024} KB",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            Text("Mahalliy", color = Color(0xFF81C784), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
