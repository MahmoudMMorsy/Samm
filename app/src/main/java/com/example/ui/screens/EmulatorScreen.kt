package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.emulator.EmulatorViewModel
import com.example.model.Game
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorScreen(
    gameId: String,
    onBack: () -> Unit,
    libraryViewModel: LibraryViewModel = viewModel(),
    emulatorViewModel: EmulatorViewModel = viewModel()
) {
    val gamesList by libraryViewModel.games.collectAsStateWithLifecycle()
    val currentGame = remember(gamesList, gameId) { gamesList.find { it.id == gameId } }
    
    LaunchedEffect(currentGame) {
        currentGame?.let {
            emulatorViewModel.startEmulator(it.path)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = currentGame?.name ?: "EMULATOR",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF39FF14)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF39FF14))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Game Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                emulatorViewModel.gameBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                // Scanline Shader Effect
                                for (i in 0 until size.height.toInt() step 4) {
                                    drawLine(
                                        color = Color.Black.copy(alpha = 0.3f),
                                        start = androidx.compose.ui.geometry.Offset(0f, i.toFloat()),
                                        end = androidx.compose.ui.geometry.Offset(size.width, i.toFloat()),
                                        strokeWidth = 2f
                                    )
                                }
                            },
                        contentScale = ContentScale.Fit
                    )
                } ?: CircularProgressIndicator(color = Color(0xFF39FF14))
            }

            // Controllers
            NativeControllerLayout(emulatorViewModel)
        }
    }
}

@Composable
fun NativeControllerLayout(viewModel: EmulatorViewModel) {
    // Standard Retro Gamepad Layout
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // D-Pad
            Column {
                ControllerButton("▲", { viewModel.onButtonPressed(4) }, { viewModel.onButtonReleased(4) })
                Row {
                    ControllerButton("◀", { viewModel.onButtonPressed(6) }, { viewModel.onButtonReleased(6) })
                    Spacer(modifier = Modifier.size(48.dp))
                    ControllerButton("▶", { viewModel.onButtonPressed(7) }, { viewModel.onButtonReleased(7) })
                }
                ControllerButton("▼", { viewModel.onButtonPressed(5) }, { viewModel.onButtonReleased(5) })
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                ControllerButton("B", { viewModel.onButtonPressed(0) }, { viewModel.onButtonReleased(0) }, color = Color.Red)
                Spacer(modifier = Modifier.width(16.dp))
                ControllerButton("A", { viewModel.onButtonPressed(8) }, { viewModel.onButtonReleased(8) }, color = Color.Green)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            ControllerButton("SELECT", { viewModel.onButtonPressed(2) }, { viewModel.onButtonReleased(2) }, width = 80.dp, height = 30.dp)
            Spacer(modifier = Modifier.width(24.dp))
            ControllerButton("START", { viewModel.onButtonPressed(3) }, { viewModel.onButtonReleased(3) }, width = 80.dp, height = 30.dp)
        }
    }
}

@Composable
fun ControllerButton(
    label: String,
    onDown: () -> Unit,
    onUp: () -> Unit,
    color: Color = Color.LightGray,
    width: androidx.compose.ui.unit.Dp = 56.dp,
    height: androidx.compose.ui.unit.Dp = 56.dp
) {
    Button(
        onClick = {},
        modifier = Modifier.size(width, height).onTouchGesture(onDown, onUp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B36)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, color = color, fontWeight = FontWeight.Bold)
    }
}
