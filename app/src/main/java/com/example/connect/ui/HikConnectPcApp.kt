package com.example.connect.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.connect.data.*
import com.example.connect.ui.components.*

@Composable
fun HikConnectPcApp() {
    var activeTab by remember { mutableStateOf(SidebarTab.LIVE_VIEW) }
    var currentGridMode by remember { mutableStateOf(GridMode.TWO_BY_TWO) }
    var streamQuality by remember { mutableStateOf(StreamQuality.HD) }

    var camerasState by remember { mutableStateOf(SampleData.sampleCameras) }
    var nvrsState by remember { mutableStateOf(SampleData.sampleNvrs) }
    var selectedCamera by remember { mutableStateOf<CameraDevice?>(SampleData.sampleCameras.firstOrNull()) }

    var showAddDeviceModal by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F1116)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top PC Bar
            TopHeader(
                currentGridMode = currentGridMode,
                onGridModeSelected = { currentGridMode = it },
                streamQuality = streamQuality,
                onQualityToggle = {
                    streamQuality = if (streamQuality == StreamQuality.HD) StreamQuality.SD else StreamQuality.HD
                },
                onAddDeviceClick = { showAddDeviceModal = true },
                onFullScreenClick = { /* Fullscreen */ }
            )

            // Main Desktop Content Layout (Sidebar + Main View Area)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Left Sidebar (Navigation & Camera Tree & PTZ)
                Sidebar(
                    activeTab = activeTab,
                    onTabSelected = { activeTab = it },
                    nvrs = nvrsState,
                    selectedCamera = selectedCamera,
                    onCameraSelect = { selectedCamera = it },
                    onPtzCommand = { cmd ->
                        // Handle PTZ command feedback
                    }
                )

                // Right Main Display View Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF0B0C10))
                ) {
                    when (activeTab) {
                        SidebarTab.LIVE_VIEW -> {
                            CameraGrid(
                                gridMode = currentGridMode,
                                cameras = camerasState,
                                selectedCamera = selectedCamera,
                                onCameraSelect = { selectedCamera = it }
                            )
                        }

                        SidebarTab.PLAYBACK -> {
                            PlaybackView(
                                selectedCamera = selectedCamera
                            )
                        }

                        SidebarTab.DEVICES -> {
                            DeviceManagementView(
                                nvrs = nvrsState,
                                onAddDeviceClick = { showAddDeviceModal = true }
                            )
                        }

                        SidebarTab.LOGS -> {
                            EventLogsView(
                                logs = SampleData.sampleLogs
                            )
                        }
                    }
                }
            }
        }

        // Add Device Modal
        if (showAddDeviceModal) {
            AddDeviceModal(
                onDismiss = { showAddDeviceModal = false },
                onAddDevice = { name, ip, port, user, pass ->
                    val newCam = CameraDevice(
                        id = "cam_${System.currentTimeMillis()}",
                        name = name,
                        nvrName = "Yangi NVR/Kamera",
                        ipAddress = ip,
                        status = DeviceStatus.ONLINE,
                        isPtzSupported = true,
                        channelNumber = camerasState.size + 1
                    )
                    camerasState = camerasState + newCam
                    selectedCamera = newCam
                }
            )
        }
    }
}
