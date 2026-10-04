package com.example.connect.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun AddDeviceModal(
    onDismiss: () -> Unit,
    onAddDevice: (name: String, ip: String, port: String, user: String, pass: String) -> Unit
) {
    var deviceName by remember { mutableStateOf("Hikvision Kamera") }
    var ipAddress by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("8000") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("s.901407735") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E222B),
            modifier = Modifier.width(420.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "➕ Hikvision Qurilma Qo'shish (NVR/IP Cam)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = "IP Manzil, Port hamda Hikvision kirish ma'lumotlarini kiriting:",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Qurilma Nomi (Masalan: Kirish Kamera)") },
                    singleLine = true,
                    colors = customTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("IP Manzil / Domain (Masalan: 192.168.1.100)") },
                    singleLine = true,
                    colors = customTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("SDK/Server Port") },
                        singleLine = true,
                        colors = customTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Login") },
                        singleLine = true,
                        colors = customTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Parol") },
                    singleLine = true,
                    colors = customTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { verificationCode = it },
                    label = { Text("Kirish Kodi / Verification Code (Serial Number)") },
                    singleLine = true,
                    colors = customTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Bekor qilish", color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (deviceName.isNotBlank()) {
                                onAddDevice(deviceName, ipAddress, port, username, password)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Qo'shish va Saqlash", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFFE53935),
    unfocusedBorderColor = Color(0xFF3B4456),
    focusedLabelColor = Color(0xFFE53935),
    unfocusedLabelColor = Color.Gray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF14171F),
    unfocusedContainerColor = Color(0xFF14171F)
)
