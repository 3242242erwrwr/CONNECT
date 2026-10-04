package com.example.connect.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.connect.data.CameraDevice
import com.example.connect.data.DeviceStatus
import com.example.connect.data.NvrDevice
import com.example.connect.data.PtzCommand

enum class SidebarTab(val title: String) {
    LIVE_VIEW("Jonli Ko'rish"),
    PLAYBACK("Arxiv / Playback"),
    DEVICES("Qurilmalar"),
    LOGS("Xabarlar")
}

@Composable
fun Sidebar(
    activeTab: SidebarTab,
    onTabSelected: (SidebarTab) -> Unit,
    nvrs: List<NvrDevice>,
    selectedCamera: CameraDevice?,
    onCameraSelect: (CameraDevice) -> Unit,
    onPtzCommand: (PtzCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color(0xFF181B22))
            .border(1.dp, Color(0xFF262B36))
    ) {
        // Navigation Tabs (Horizontal icons / buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF20242D))
        ) {
            SidebarTab.entries.forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) }
                        .background(if (isSelected) Color(0xFF181B22) else Color.Transparent)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (tab) {
                            SidebarTab.LIVE_VIEW -> "🎥"
                            SidebarTab.PLAYBACK -> "⏱️"
                            SidebarTab.DEVICES -> "📟"
                            SidebarTab.LOGS -> "🔔"
                        },
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Active Tab Label Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222733))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = activeTab.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        if (activeTab == SidebarTab.LIVE_VIEW || activeTab == SidebarTab.PLAYBACK) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Kamerani izlash...", color = Color.Gray, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    unfocusedBorderColor = Color(0xFF333A48),
                    focusedContainerColor = Color(0xFF20242D),
                    unfocusedContainerColor = Color(0xFF20242D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(48.dp)
            )

            // Camera Tree View
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                items(nvrs) { nvr ->
                    NvrTreeItem(
                        nvr = nvr,
                        searchQuery = searchQuery,
                        selectedCamera = selectedCamera,
                        onCameraSelect = onCameraSelect
                    )
                }
            }

            // PTZ Controller Section
            if (selectedCamera?.isPtzSupported == true) {
                PtzControlSection(
                    selectedCamera = selectedCamera,
                    onPtzCommand = onPtzCommand
                )
            }
        }
    }
}

@Composable
fun NvrTreeItem(
    nvr: NvrDevice,
    searchQuery: String,
    selectedCamera: CameraDevice?,
    onCameraSelect: (CameraDevice) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val filteredCameras = nvr.cameras.filter {
        searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true)
    }

    if (filteredCameras.isNotEmpty() || searchQuery.isEmpty()) {
        Column(modifier = Modifier.padding(bottom = 6.dp)) {
            // NVR Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF242A36))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (expanded) "▼" else "▶",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = "📁 ${nvr.name}",
                    color = Color.LightGray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${nvr.cameras.count { it.status != DeviceStatus.OFFLINE }}/${nvr.cameras.size}",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }

            // Camera Items list
            if (expanded) {
                filteredCameras.forEach { camera ->
                    val isSelected = selectedCamera?.id == camera.id
                    val statusColor = when (camera.status) {
                        DeviceStatus.RECORDING -> Color(0xFFE53935)
                        DeviceStatus.ONLINE -> Color(0xFF4CAF50)
                        DeviceStatus.WARNING -> Color(0xFFFF9800)
                        DeviceStatus.OFFLINE -> Color.Gray
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) Color(0xFF3B4456) else Color.Transparent)
                            .clickable { onCameraSelect(camera) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                                .padding(end = 6.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📹 ${camera.name}",
                            color = if (isSelected) Color.White else Color(0xFFB0B8C6),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (camera.isPtzSupported) {
                            Text(text = "🎮", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PtzControlSection(
    selectedCamera: CameraDevice,
    onPtzCommand: (PtzCommand) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF202530))
            .border(1.dp, Color(0xFF2A3140))
            .padding(10.dp)
    ) {
        Text(
            text = "PTZ Boshqaruv Panel (${selectedCamera.name})",
            color = Color(0xFFFF8A80),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // D-Pad Arrow Buttons Layout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // UP
            PtzButton(text = "▲", onClick = { onPtzCommand(PtzCommand.UP) })

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT
                PtzButton(text = "◀", onClick = { onPtzCommand(PtzCommand.LEFT) })
                // Center Status
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161920)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "PTZ", color = Color.Gray, fontSize = 9.sp)
                }
                // RIGHT
                PtzButton(text = "▶", onClick = { onPtzCommand(PtzCommand.RIGHT) })
            }

            // DOWN
            PtzButton(text = "▼", onClick = { onPtzCommand(PtzCommand.DOWN) })
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Zoom Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onPtzCommand(PtzCommand.ZOOM_IN) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333B4A)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("🔍 Zoom +", fontSize = 11.sp, color = Color.White)
            }

            Button(
                onClick = { onPtzCommand(PtzCommand.ZOOM_OUT) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333B4A)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("🔍 Zoom -", fontSize = 11.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Gate / Barrier Quick Access Control Button
        var gateStatusText by remember { mutableStateOf<String?>(null) }
        Button(
            onClick = {
                gateStatusText = "🚪 Darvoza Ochildi! (Gate Unlocked)"
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        ) {
            Text(
                text = "🚪 Darvozani Ochish (Gate Control)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        gateStatusText?.let { text ->
            LaunchedEffect(text) {
                kotlinx.coroutines.delay(2000)
                gateStatusText = null
            }
            Text(
                text = text,
                color = Color.Green,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun PtzButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF2C3444))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
