package com.example.connect.ui

import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YouTubeBrowserApp() {
    var currentUrl by remember { mutableStateOf("https://yewtu.be") }
    var inputUrl by remember { mutableStateOf("https://yewtu.be") }
    var searchQuery by remember { mutableStateOf("") }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1116))
    ) {
        // Top Custom App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFF1E222B))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFE53935), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            // URL & Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("YouTube'dan video izlang...", color = Color.Gray, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE53935),
                    unfocusedBorderColor = Color(0xFF3B4456),
                    focusedContainerColor = Color(0xFF14171F),
                    unfocusedContainerColor = Color(0xFF14171F),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            // Search Button
            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        val formattedSearch = searchQuery.replace(" ", "+")
                        currentUrl = "https://yewtu.be/search?q=$formattedSearch"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text("🔍 Izlash", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Quick Preset Shortcuts Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161922))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tezkor:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            ShortcutChip(
                label = "⚡ Yewtu.be (Light YouTube)",
                isSelected = currentUrl.contains("yewtu.be"),
                onClick = { currentUrl = "https://yewtu.be" }
            )

            ShortcutChip(
                label = "🎬 YouTube Mobile",
                isSelected = currentUrl.contains("m.youtube.com"),
                onClick = { currentUrl = "https://m.youtube.com" }
            )

            ShortcutChip(
                label = "🎵 YouTube Music",
                isSelected = currentUrl.contains("music.youtube.com"),
                onClick = { currentUrl = "https://music.youtube.com" }
            )

            Button(
                onClick = { webViewRef?.reload() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B3242)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("🔄 Yangilash", color = Color.LightGray, fontSize = 10.sp)
            }
        }

        // WebView Video Display Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Mobile Safari/537.36"
                        }

                        loadUrl(currentUrl)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    if (webView.url != currentUrl) {
                        webView.loadUrl(currentUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun ShortcutChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFFE53935) else Color(0xFF262C3A)
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.LightGray,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
