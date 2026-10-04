package com.example.connect.data

enum class GridMode(val count: Int, val label: String) {
    ONE_BY_ONE(1, "1x1"),
    TWO_BY_TWO(4, "2x2"),
    THREE_BY_THREE(9, "3x3"),
    FOUR_BY_FOUR(16, "4x4")
}

enum class StreamQuality {
    HD, SD
}

enum class DeviceStatus {
    ONLINE, OFFLINE, RECORDING, WARNING
}

data class CameraDevice(
    val id: String,
    val name: String,
    val nvrName: String = "Main NVR",
    val ipAddress: String,
    val status: DeviceStatus = DeviceStatus.ONLINE,
    val isPtzSupported: Boolean = true,
    val rtspUrl: String = "rtsp://$ipAddress:554/ch1/main",
    val channelNumber: Int = 1,
    val isFavorite: Boolean = false,
    val lastEvent: String? = null,
    val verificationCode: String = "s.901407735"
)

data class NvrDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val serialNumber: String = "s.901407735",
    val verificationCode: String = "s.901407735",
    val status: DeviceStatus = DeviceStatus.ONLINE,
    val cameras: List<CameraDevice>
)

data class EventLog(
    val id: String,
    val cameraName: String,
    val eventType: String, // Motion Detected, Intrusion, Line Crossing, Disconnected
    val timestamp: String,
    val severity: String = "INFO" // INFO, WARNING, ALARM
)

enum class PtzCommand {
    UP, DOWN, LEFT, RIGHT, ZOOM_IN, ZOOM_OUT, FOCUS_NEAR, FOCUS_FAR
}
