package com.example.connect.data

object SampleData {
    val sampleCameras = listOf(
        CameraDevice("cam1", "Asosiy Kirish (Main Gate)", "NVR-Markaziy", "192.168.1.101", DeviceStatus.RECORDING, true, channelNumber = 1),
        CameraDevice("cam2", "Kassa va Qabulxona", "NVR-Markaziy", "192.168.1.102", DeviceStatus.RECORDING, false, channelNumber = 2),
        CameraDevice("cam3", "Avtoturargoh (Parking Area)", "NVR-Markaziy", "192.168.1.103", DeviceStatus.ONLINE, true, channelNumber = 3),
        CameraDevice("cam4", "Omborxona (Warehouse)", "NVR-Markaziy", "192.168.1.104", DeviceStatus.WARNING, false, channelNumber = 4, lastEvent = "Harakat aniqlandi (Motion)"),
        CameraDevice("cam5", "2-Qavat Koridor", "NVR-Ikkinchi-Bino", "192.168.1.105", DeviceStatus.ONLINE, false, channelNumber = 1),
        CameraDevice("cam6", "Server Xonasi (Server Room)", "NVR-Ikkinchi-Bino", "192.168.1.106", DeviceStatus.RECORDING, true, channelNumber = 2),
        CameraDevice("cam7", "Perimetr Shimol", "NVR-Ikkinchi-Bino", "192.168.1.107", DeviceStatus.OFFLINE, true, channelNumber = 3),
        CameraDevice("cam8", "Bosh Offis (Main Office)", "NVR-Ikkinchi-Bino", "192.168.1.108", DeviceStatus.ONLINE, false, channelNumber = 4)
    )

    val sampleNvrs = listOf(
        NvrDevice(
            id = "nvr1",
            name = "Hikvision NVR (s.901407735)",
            ipAddress = "192.168.1.200",
            serialNumber = "s.901407735",
            verificationCode = "s.901407735",
            status = DeviceStatus.ONLINE,
            cameras = sampleCameras.subList(0, 4)
        ),
        NvrDevice(
            id = "nvr2",
            name = "NVR-Ikkinchi-Bino",
            ipAddress = "192.168.1.201",
            serialNumber = "DS-7604NI-K1/4P",
            verificationCode = "s.901407735",
            status = DeviceStatus.ONLINE,
            cameras = sampleCameras.subList(4, 8)
        )
    )

    val sampleLogs = listOf(
        EventLog("log1", "Omborxona (Warehouse)", "Harakat aniqlandi (Motion Detection)", "22:42:10", "WARNING"),
        EventLog("log2", "Avtoturargoh (Parking Area)", "Avtomobil raqami o'qildi (ANPR)", "22:38:05", "INFO"),
        EventLog("log3", "Perimetr Shimol", "Aloqa uzildi (Device Offline)", "22:15:00", "ALARM"),
        EventLog("log4", "Asosiy Kirish (Main Gate)", "Yuz tanindi (Face Recognition: Jamshid)", "21:50:30", "INFO"),
        EventLog("log5", "Server Xonasi (Server Room)", "Eshik ochildi (Access Control)", "21:30:12", "INFO")
    )
}
