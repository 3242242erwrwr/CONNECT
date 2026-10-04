package com.example.connect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.connect.data.CameraDevice

@Composable
fun PlaybackView(
    selectedCamera: CameraDevice?,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var playSpeed by remember { mutableStateOf("1x") }
    var sliderPosition by remember { mutableFloatStateOf(14f) } // 14:00 (2 PM)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1116))
            .padding(8.dp)
    ) {
        // Video Player Placeholder Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF151821))
                .border(1.dp, Color(0xFF232836)),
            contentAlignment = Alignment.Center
        ) {
            if (selectedCamera == null) {
                Text(
                    text = "Arxivni ko'rish uchun chap paneldan kamerani tanlang",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📼 PLAYBACK ARCHIVE: ${selectedCamera.name}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sana: 2026-10-01  |  Vaqt: ${String.format("%02d:00:00", sliderPosition.toInt())}",
                        color = Color(0xFFFFB74D),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isPlaying) "▶ IJRO ETILMOQDA ($playSpeed)" else "⏸ PAUZA",
                        color = if (isPlaying) Color.Green else Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Playback Timeline & Controls Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1D212A))
                .padding(12.dp)
        ) {
            // Timeline 24h Slider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "00:00", color = Color.Gray, fontSize = 11.sp)
                Text(
                    text = "Tanlangan vaqt: ${String.format("%02d:00:00", sliderPosition.toInt())}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(text = "23:59", color = Color.Gray, fontSize = 11.sp)
            }

            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                valueRange = 0f..23f,
                steps = 23,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFE53935),
                    activeTrackColor = Color(0xFFE53935),
                    inactiveTrackColor = Color(0xFF333B4D)
                )
            )

            // Timeline Legend (Continuous vs Motion Event)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF4CAF50)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Uzluksiz Yozuv (Continuous)", color = Color.LightGray, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF9800)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Harakat (Motion Event)", color = Color.LightGray, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFE53935)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Signal/Alarm", color = Color.LightGray, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isPlaying = !isPlaying },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(text = if (isPlaying) "⏸ Pauza" else "▶ Ijro etish", color = Color.White)
                    }

                    listOf("1x", "2x", "4x", "8x").forEach { speed ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (playSpeed == speed) Color(0xFF3A4458) else Color(0xFF282E3D))
                                .clickable { playSpeed = speed }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = speed,
                                color = if (playSpeed == speed) Color.Red else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Button(
                    onClick = { /* Export */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3242)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(text = "💾 Videoni yuklab olish (Export)", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}
