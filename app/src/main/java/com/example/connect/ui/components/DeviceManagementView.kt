package com.example.connect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.connect.data.DeviceStatus
import com.example.connect.data.NvrDevice

@Composable
fun DeviceManagementView(
    nvrs: List<NvrDevice>,
    onAddDeviceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1116))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Qurilmalar Boshqaruvi (Device Management)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Ulangan NVR, DVR va IP Kameralar ro'yxati hamda holati",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onAddDeviceClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("+ Yangi NVR / Kamera Qo'shish", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(nvrs) { nvr ->
                NvrCard(nvr = nvr)
            }
        }
    }
}

@Composable
fun NvrCard(nvr: NvrDevice) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1E28)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF282E3D), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (nvr.status == DeviceStatus.ONLINE) Color.Green else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "💾 ${nvr.name}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Text(
                    text = "IP: ${nvr.ipAddress}  |  S/N: ${nvr.serialNumber}",
                    color = Color(0xFF8C98AC),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF262B38))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Kanal va Kameralar (${nvr.cameras.size} ta):",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                nvr.cameras.forEach { camera ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF222735))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📹", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kanal ${camera.channelNumber}: ${camera.name}",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = camera.ipAddress,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (camera.status) {
                                            DeviceStatus.RECORDING -> Color(0xFFE53935).copy(alpha = 0.2f)
                                            DeviceStatus.ONLINE -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                            DeviceStatus.WARNING -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                            DeviceStatus.OFFLINE -> Color.Gray.copy(alpha = 0.2f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = camera.status.name,
                                    color = when (camera.status) {
                                        DeviceStatus.RECORDING -> Color(0xFFFF5252)
                                        DeviceStatus.ONLINE -> Color(0xFF81C784)
                                        DeviceStatus.WARNING -> Color(0xFFFFB74D)
                                        DeviceStatus.OFFLINE -> Color.LightGray
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
