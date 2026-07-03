package com.aistudio.retroplay.wmpkqa.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    // Cyberpunk Retro Colors
    val neonGreen = Color(0xFF39FF14)
    val obsidian = Color(0xFF030305)

    val bootLogs = remember {
        mutableStateListOf<String>()
    }
    
    val fullBootLogs = listOf(
        "RETRO SYSTEM V2.6 BOOT BIOS...",
        "CPU: 8-BIT SHARP LR35902 FAMILY @ 4.19 MHz...",
        "VRAM: DETECTED 16KB HIGH-SPEED STAT...",
        "AUDIO: COAXIAL STEREO FM SYNTH ENGINE...",
        "STORAGE: SQLITE ROOM MOUNTED AT /data...",
        "NUCLEUS ENGINE: GameBoyCore.JS ... READY",
        "SYNC: PLATFORM METADATA SYNC ... COMPLETED",
        "STATUS: BOOT SUCCESS! STARTING CONSOLE..."
    )

    var currentBuildProgress by remember { mutableStateOf(0) }

    // Sequential diagnostic type-writer print simulation
    LaunchedEffect(Unit) {
        delay(100)
        for (i in fullBootLogs.indices) {
            bootLogs.add(fullBootLogs[i])
            delay(280) // Fast terminal type offset
        }
        
        // Progress tick up
        while (currentBuildProgress < 100) {
            currentBuildProgress += (5..15).random()
            if (currentBuildProgress > 100) currentBuildProgress = 100
            delay(120)
        }
        delay(400) // Aesthetic delay for complete status display
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(obsidian),
        contentAlignment = Alignment.Center
    ) {
        // Clean backgrounds programmatically 


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Cyber Logo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RETROPLAY GBA",
                    color = Color(0xFFFF00FF), // Cyber-Magenta Glow Accent
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "ULTIMATE RETRO EMULATION CORE",
                    color = neonGreen.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Diagnostic Console (Center Area)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 40.dp)
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "--- SYSTEM DIAGNOSTIC RUN ---",
                    color = Color(0xFF39FF14).copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                bootLogs.forEach { log ->
                    Text(
                        text = ">>> $log",
                        color = neonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // GameBoy Console Loading Bar (Bottom)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Retro visual progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(Color.Black)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(currentBuildProgress / 100f)
                            .background(neonGreen)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "LOADING GRAPHICAL UI SYSTEM... $currentBuildProgress%",
                    color = neonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
