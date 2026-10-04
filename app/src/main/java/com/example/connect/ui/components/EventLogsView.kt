package com.example.connect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.connect.data.EventLog

@Composable
fun EventLogsView(
    logs: List<EventLog>,
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
                    text = "Hodisalar va Xabarlar (Event Logs & Alarms)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Kameralarda aniqlangan harakatlar, trevogalar va tizim bildirishnomalari",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { /* Clear or Export */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282F3E)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("🗑 Loglarni Tozalash", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(logs) { log ->
                EventLogCard(log = log)
            }
        }
    }
}

@Composable
fun EventLogCard(log: EventLog) {
    val severityColor = when (log.severity) {
        "ALARM" -> Color(0xFFE53935)
        "WARNING" -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1B1E28))
            .border(1.dp, Color(0xFF262C3A), RoundedCornerShape(6.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(severityColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = log.severity,
                    color = severityColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Column {
                Text(
                    text = "${log.cameraName} — ${log.eventType}",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Holat muvaffaqiyatli saqlandi",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = log.timestamp,
            color = Color(0xFFFFB74D),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
