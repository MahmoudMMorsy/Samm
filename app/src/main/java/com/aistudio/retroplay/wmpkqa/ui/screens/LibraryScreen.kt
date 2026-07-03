package com.aistudio.retroplay.wmpkqa.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aistudio.retroplay.wmpkqa.R
import com.aistudio.retroplay.wmpkqa.model.Game
import com.aistudio.retroplay.wmpkqa.model.GameType

// Retro Synthwave Cyber Colors
val SynthObsidian = Color(0xFF030305)
val NeonGreen = Color(0xFF39FF14)
val LaserMagenta = Color(0xFFFF00FF)
val CyberCyan = Color(0xFF00FFFF)
val DarkNavy = Color(0xFF0D0D15)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToEmulator: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val gamesList by viewModel.games.collectAsStateWithLifecycle()
    val isDownloadingState by viewModel.isDownloading.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedGame by remember { mutableStateOf<Game?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
            }
            viewModel.processDroppedFile(it)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SynthObsidian)) {
        // Dynamic Background Blur based on selected game
        AnimatedContent(
            targetState = selectedGame,
            transitionSpec = { fadeIn() with fadeOut() }
        ) { game ->
            if (game?.boxArtUrl != null) {
                AsyncImage(
                    model = game.boxArtUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().scale(1.2f).blur(50.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(DarkNavy, SynthObsidian)
                        )
                    )
                }
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "RETROPLAY",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = Color.White,
                                letterSpacing = 4.sp
                            )
                            Text(
                                text = " ULTRA",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Light,
                                fontSize = 24.sp,
                                color = NeonGreen,
                                letterSpacing = 2.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                    containerColor = NeonGreen,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Game Details Preview Section (Cinematic)
                AnimatedVisibility(
                    visible = selectedGame != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    selectedGame?.let { game ->
                        GameHeroSection(game = game, onPlay = { onNavigateToEmulator(game.id) })
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Library Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (gamesList.isEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            EmptyLibraryState(onDownload = { name, url, ext ->
                                viewModel.downloadDemoGame(name, url, ext)
                            })
                        }
                    } else {
                        items(gamesList) { game ->
                            GamePosterItem(
                                game = game,
                                isSelected = selectedGame?.id == game.id,
                                onClick = { selectedGame = game }
                            )
                        }
                    }
                }
            }
        }

        // Loading HUD
        isDownloadingState?.let { LoadingOverlay(it) }
    }
}

@Composable
fun GameHeroSection(game: Game, onPlay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonGreen)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = game.type.name,
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (index < (game.rating ?: 0f).toInt()) NeonGreen else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = game.name,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = game.genre ?: "Retro Game",
            color = CyberCyan,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = game.description ?: "Ready to play the classic adventures of the past.",
            color = Color.LightGray,
            fontSize = 14.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("إلعب الآن", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun GamePosterItem(game: Game, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (isSelected) 1.1f else 1.0f)
    val borderAlpha by animateFloatAsState(if (isSelected) 1.0f else 0.0f)

    Column(
        modifier = Modifier
            .scale(scale)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .border(
                    BorderStroke(2.dp, NeonGreen.copy(alpha = borderAlpha)),
                    RoundedCornerShape(8.dp)
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24))
        ) {
            if (game.boxArtUrl != null) {
                AsyncImage(
                    model = game.boxArtUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Gamepad, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyLibraryState(onDownload: (String, String, String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "المكتبة فارغة، ابدأ بتحميل الألعاب التجريبية:",
            color = Color.White,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(24.dp))

        DemoQuickDownload(
            "Tobu Tobu Girl",
            "https://github.com/mrombout/tobu-tobu-girl/releases/download/v1.0.1/tobutobugirl.gb",
            "gb",
            onDownload
        )
        Spacer(modifier = Modifier.height(12.dp))
        DemoQuickDownload(
            "µCity Color",
            "https://github.com/vhelin/ucity/releases/download/v1.1/ucity.gbc",
            "gbc",
            onDownload
        )
    }
}

@Composable
fun DemoQuickDownload(name: String, url: String, ext: String, onDownload: (String, String, String) -> Unit) {
    Button(
        onClick = { onDownload(name, url, ext) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242430)),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
    ) {
        Text("تحميل $name ($ext)", color = CyberCyan)
    }
}

@Composable
fun LoadingOverlay(msg: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NeonGreen)
            Spacer(modifier = Modifier.height(16.dp))
            Text(msg, color = Color.White, fontFamily = FontFamily.Monospace)
        }
    }
}
