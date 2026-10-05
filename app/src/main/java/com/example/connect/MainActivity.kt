package com.example.connect

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.connect.service.SosBackgroundService
import com.example.connect.ui.FamilySosAlertApp
import com.example.connect.ui.theme.ConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start 24/7 4G Persistent Service for 100% 4G/5G Mobile Data Communication
        try {
            val serviceIntent = Intent(this, SosBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Ignore error
        }

        setContent {
            ConnectTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FamilySosAlertApp()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FamilySosAlertAppPreview() {
    ConnectTheme {
        FamilySosAlertApp()
    }
}
