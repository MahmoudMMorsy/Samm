package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.Game
import com.example.model.GameType

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

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                // Ignore if we can't take persistable permission
            }
            viewModel.processDroppedFile(it)
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
                        Icon(
                            imageVector = Icons.Default.VideogameAsset,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "RETROPLAY GBA",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = LaserMagenta
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = "Settings",
                            tint = CyberCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkNavy,
                    titleContentColor = NeonGreen
                ),
                modifier = Modifier.border(
                    BorderStroke(1.dp, LaserMagenta.copy(alpha = 0.3f))
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                containerColor = LaserMagenta,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .border(BorderStroke(2.dp, CyberCyan), RoundedCornerShape(12.dp))
                    .padding(bottom = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إضافة لُعبة (ROM)", 
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = SynthObsidian
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Clean background layout


            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (gamesList.isNotEmpty()) {
                    // Title for Added ROMs
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "الألعاب المثبّتة فِي المكتبة",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NeonGreen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    items(gamesList) { game ->
                        GameCartridgeCard(game = game, onClick = { onNavigateToEmulator(game.id) })
                    }
                }

                // Header for built-in downloader always visible to ensure users don't get stuck!
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp)
                    ) {
                        Text(
                            text = "تَحميل الألعاب التجريبية مباشرة (Homebrew)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CyberCyan,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "ألعاب كلاسيكية مفتوحة المصدر قانونية للعب الفوري دون تعقيد",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Showcase homebrew list
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DemoDownloadRow(
                            title = "Tobu Tobu Girl (Action-Puzzle)",
                            description = "لعبة مغامرات ممتعة حازت على جوائز لـ GameBoy كلاسيك",
                            systemType = "GB (Retro)",
                            url = "https://github.com/mrombout/tobu-tobu-girl/releases/download/v1.0.1/tobutobugirl.gb",
                            ext = "gb",
                            onDownload = { name, url, ext ->
                                viewModel.downloadDemoGame(name, url, ext)
                            }
                        )

                        DemoDownloadRow(
                            title = "µCity Color (City-Builder)",
                            description = "لعبة محاكاة بناء مدينة تفاعلية كاملة لـ GameBoy Color",
                            systemType = "GBC (Color)",
                            url = "https://github.com/vhelin/ucity/releases/download/v1.1/ucity.gbc",
                            ext = "gbc",
                            onDownload = { name, url, ext ->
                                viewModel.downloadDemoGame(name, url, ext)
                            }
                        )

                        DemoDownloadRow(
                            title = "Flappy Boy (Retro Flappy)",
                            description = "محاكاة طائر في الأنبوب كلاسيكية 8 بت سريعة وبسيطة",
                            systemType = "GB (Retro)",
                            url = "https://github.com/GrizzlyCove/FlappyBoy/raw/master/FlappyBoy.gb",
                            ext = "gb",
                            onDownload = { name, url, ext ->
                                viewModel.downloadDemoGame(name, url, ext)
                            }
                        )
                    }
                }

                // If completely empty, show extra welcoming layout
                if (gamesList.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkNavy)
                                .border(BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "مكتبتك فارغة حالياً!\nاضغط على (تحميل) بالأسفل للبدء فوراً بجولة لعب تجريبية.",
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Real-time downloading HUD notifier overlay
            isDownloadingState?.let { statusMsg ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(enabled = false) {}, // consume clicks
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkNavy),
                        border = BorderStroke(2.dp, NeonGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .width(280.dp)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = NeonGreen)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = statusMsg,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "يرجى الانتظار، يتم الاتصال بالنواة...",
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameCartridgeCard(game: Game, onClick: () -> Unit) {
    // Dynamic border depending on GBC vs GB 
    val accentColor = if (game.type == GameType.GBC) CyberCyan else NeonGreen

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Realistic gameboy cartridge shape design with grip lines at the top
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF242430)),
            border = BorderStroke(2.dp, accentColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Ribbed cartridge top grip bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(Color(0xFF16161F))
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(8) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF242430))
                        )
                    }
                }

                // Inner label artwork section
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F0F16), Color(0xFF030305))
                            )
                        )
                        .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = accentColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = game.name,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Rom info badge tag
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = game.type.name,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = accentColor
            )
        }
    }
}

@Composable
fun DemoDownloadRow(
    title: String,
    description: String,
    systemType: String,
    url: String,
    ext: String,
    onDownload: (String, String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101018)),
        border = BorderStroke(1.dp, LaserMagenta.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = systemType,
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { onDownload(title, url, ext) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تَحميل", 
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
