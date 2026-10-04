package com.example.connect.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.connect.data.CameraDevice
import com.example.connect.data.DeviceStatus
import com.example.connect.data.GridMode

@Composable
fun CameraGrid(
    gridMode: GridMode,
    cameras: List<CameraDevice>,
    selectedCamera: CameraDevice?,
    onCameraSelect: (CameraDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxCount = gridMode.count
    val displayCameras = remember(gridMode, cameras) {
        val list = mutableListOf<CameraDevice?>()
        for (i in 0 until maxCount) {
            list.add(cameras.getOrNull(i))
        }
        list
    }

    val columns = when (gridMode) {
        GridMode.ONE_BY_ONE -> 1
        GridMode.TWO_BY_TWO -> 2
        GridMode.THREE_BY_THREE -> 3
        GridMode.FOUR_BY_FOUR -> 4
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1116))
            .padding(4.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(displayCameras.size) { index ->
                val camera = displayCameras[index]
                val isSelected = selectedCamera?.id == camera?.id && camera != null

                CameraCell(
                    camera = camera,
                    cellIndex = index + 1,
                    isSelected = isSelected,
                    onSelect = {
                        if (camera != null) {
                            onCameraSelect(camera)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CameraCell(
    camera: CameraDevice?,
    cellIndex: Int,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    var isRecording by remember { mutableStateOf(camera?.status == DeviceStatus.RECORDING) }
    var isMuted by remember { mutableStateOf(true) }
    var snapshotMessage by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val gridLineColor = Color(0xFF1E2330)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF14171F))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFFF3D00) else Color(0xFF232836)
            )
            .clickable { onSelect() }
    ) {
        if (camera == null) {
            // Empty Camera Slot
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "📷", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Kanal $cellIndex - Bo'sh",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else if (camera.status == DeviceStatus.OFFLINE) {
            // Offline Camera with Gateway Reconnect Option
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "⚠️", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = camera.name,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Gateway aloqasi uzilgan (${camera.ipAddress})",
                    color = Color(0xFFE53935),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        snapshotMessage = "Gateway qayta ulandi (s.901407735)!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E3B52)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("⚡ Gateway-ga Qayta Kirish", fontSize = 11.sp, color = Color.White)
                }
            }
        } else {
            // Live Video Feed Simulation Area
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 1.dp.toPx()
                // Draw CCTV grid lines effect
                drawRect(Color(0xFF0C0E13))
                val dashWidth = 8.dp.toPx()
                val dashGap = 8.dp.toPx()
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)

                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = strokeWidth,
                    pathEffect = pathEffect
                )
                drawLine(
                    color = gridLineColor,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = strokeWidth,
                    pathEffect = pathEffect
                )
            }

            // CCTV Watermark / Simulated Live Feed Graphics
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LIVE STREAM [RTSP/H.265]",
                        color = Color(0xFF2A3344),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "1920x1080 @ 30fps | 4096 Kbps",
                        color = Color(0xFF222B38),
                        fontSize = 10.sp
                    )
                }
            }

            // Top Header Overlay inside Camera Cell
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x99000000))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.Red.copy(alpha = pulseAlpha) else Color.Green)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${camera.channelNumber}. ${camera.name}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "2026-10-01  22:44:06",
                    color = Color.Yellow,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            // Bottom Overlay Controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xBB000000))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Record Toggle
                    Text(
                        text = if (isRecording) "🔴 REC" else "⚪ REC",
                        color = if (isRecording) Color.Red else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { isRecording = !isRecording }
                    )

                    // Audio Mute/Unmute
                    Text(
                        text = if (isMuted) "🔇 Mute" else "🔊 Live Audio",
                        color = if (isMuted) Color.Gray else Color.Green,
                        fontSize = 10.sp,
                        modifier = Modifier.clickable { isMuted = !isMuted }
                    )

                    // Snapshot Button
                    Text(
                        text = "📸 Rasm",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        modifier = Modifier.clickable {
                            snapshotMessage = "Skrinshot saqlandi!"
                        }
                    )

                    // Gate / Barrier Open Button for Gate cameras
                    if (camera.name.contains("Kirish", ignoreCase = true) || camera.name.contains("Gate", ignoreCase = true) || camera.channelNumber == 1) {
                        Text(
                            text = "🚪 Darvoza Ochish",
                            color = Color(0xFFFFB74D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.clickable {
                                snapshotMessage = "🚪 Darvoza Ochildi! (Gate Unlocked: s.901407735)"
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (camera.isPtzSupported) {
                        Text(text = "🎮 PTZ", color = Color(0xFFFF8A80), fontSize = 10.sp)
                    }
                    Text(text = "HD", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(text = "⛶", color = Color.White, fontSize = 11.sp)
                }
            }

            // Snapshot Notification Toast Overlay
            snapshotMessage?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(1500)
                    snapshotMessage = null
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xDD000000))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = msg, color = Color.Green, fontSize = 12.sp)
                }
            }
        }
    }
}
