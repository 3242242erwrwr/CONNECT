package com.example.connect

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.connect.ui.FamilySosAlertApp
import com.example.connect.ui.theme.ConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // TURN ON SCREEN & SHOW OVER LOCKSCREEN WHEN SCREEN IS OFF!
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )

            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                keyguardManager.requestDismissKeyguard(this, null)
            }
        } catch (e: Exception) {
            // Ignore flags error
        }

        enableEdgeToEdge()

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
