package com.example.connect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.connect.data.GridMode
import com.example.connect.data.StreamQuality

@Composable
fun TopHeader(
    currentGridMode: GridMode,
    onGridModeSelected: (GridMode) -> Unit,
    streamQuality: StreamQuality,
    onQualityToggle: () -> Unit,
    onAddDeviceClick: () -> Unit,
    onFullScreenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF1E222B))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Logo & App Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE53935)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "HK",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Column {
                Text(
                    text = "Hik-Connect PC",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "iVMS-4200 Surveillance Center",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        // Center - Grid Layout Mode Switches
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2E39))
                .padding(4.dp)
        ) {
            GridMode.entries.forEach { mode ->
                val isSelected = currentGridMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) Color(0xFF3F4656) else Color.Transparent)
                        .clickable { onGridModeSelected(mode) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = mode.label,
                        color = if (isSelected) Color(0xFFFF5252) else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Right Action Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quality Toggle (HD / SD)
            Button(
                onClick = onQualityToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2A2E39),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Stream: ${streamQuality.name}",
                    fontSize = 12.sp,
                    color = if (streamQuality == StreamQuality.HD) Color(0xFF4CAF50) else Color.Yellow
                )
            }

            // Add Device Button
            Button(
                onClick = onAddDeviceClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "+ Qurilma Qo'shish",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Connection & Verification Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .border(1.dp, Color(0xFF333A48), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF4CAF50))
                )
                Text(
                    text = "Kod: s.901407735",
                    color = Color.LightGray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
