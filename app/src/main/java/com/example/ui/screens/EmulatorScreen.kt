package com.example.ui.screens

import android.content.res.Configuration
import android.util.Base64
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Key Index Mappings for Joypad:
// 0: RIGHT, 1: LEFT, 2: UP, 3: DOWN, 4: A, 5: B, 6: SELECT, 7: START, 8: R, 9: L
private const val BTN_RIGHT = 0
private const val BTN_LEFT = 1
private const val BTN_UP = 2
private const val BTN_DOWN = 3
private const val BTN_A = 4
private const val BTN_B = 5
private const val BTN_SELECT = 6
private const val BTN_START = 7
private const val BTN_R = 8
private const val BTN_L = 9

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorScreen(
    gameId: String,
    onBack: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val gamesList by viewModel.games.collectAsStateWithLifecycle()
    val currentGame = remember(gamesList, gameId) {
        gamesList.find { it.id == gameId }
    }

    val sheetState = rememberModalBottomSheetState()
    var showMenu by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isRomLoaded by remember { mutableStateOf(false) }
    var isFastForward by remember { mutableStateOf(false) }
    var consoleMessage by remember { mutableStateOf("BOOTING RETRO CORE...") }

    // Read ROM bytes and convert to Base64 in Dispatcher.IO to avoid blocking the main thread!
    var romBase64 by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentGame) {
        currentGame?.let { game ->
            withContext(Dispatchers.IO) {
                try {
                    val file = File(game.path)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        romBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    } else {
                        consoleMessage = "ROM FILE NOT FOUND AT PATH EXTREMUM"
                    }
                } catch (e: Exception) {
                    consoleMessage = "ROM READ ERROR: ${e.message}"
                }
            }
        }
    }

    // Auto-Save background cycle: Save battery SRAM every 25 seconds
    LaunchedEffect(isRomLoaded, currentGame) {
        if (isRomLoaded && currentGame != null) {
            while (true) {
                delay(25000)
                webViewInstance?.let { webView ->
                    webView.evaluateJavascript("window.getSRAM()") { sramBase64 ->
                        if (!sramBase64.isNullOrEmpty() && sramBase64 != "null" && sramBase64 != "\"\"") {
                            val cleanSram = sramBase64.trim('"')
                            viewModel.updateGameSram(gameId, cleanSram)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = currentGame?.name ?: "RETRO CONSOLE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF39FF14)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Exit",
                            tint = Color(0xFF39FF14)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu, 
                            contentDescription = "Emulator Menu",
                            tint = Color(0xFF39FF14)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0D11),
                    titleContentColor = Color(0xFF39FF14),
                    actionIconContentColor = Color(0xFF39FF14),
                    navigationIconContentColor = Color(0xFF39FF14)
                )
            )
        },
        containerColor = Color(0xFF0D0D11)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Game Screen Viewport Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .padding(12.dp)
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .border(
                            BorderStroke(3.dp, Color(0xFF39FF14)),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        // On HTML load finished, if we already have the ROM base64, load it!
                                        romBase64?.let { base64 ->
                                            scope.launch {
                                                // Small delay to let Javascript core instantiate
                                                delay(500)
                                                view?.evaluateJavascript("window.loadRom('$base64')") { result ->
                                                    if (result != null && result.contains("SUCCESS")) {
                                                        isRomLoaded = true
                                                        // Inject previous SRAM backup if exists
                                                        currentGame?.sram?.let { prevSram ->
                                                            if (prevSram.isNotEmpty()) {
                                                                view.evaluateJavascript("window.injectSRAM('$prevSram')") {}
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    mediaPlaybackRequiresUserGesture = false
                                }
                                loadUrl("file:///android_asset/retro_emulator.html")
                                webViewInstance = this
                            }
                        },
                        update = { webView ->
                            // If base64 loaded after Webview initialized, trigger boot
                            if (!isRomLoaded && romBase64 != null) {
                                webView.evaluateJavascript("window.loadRom('$romBase64')") { result ->
                                    if (result != null && result.contains("SUCCESS")) {
                                        isRomLoaded = true
                                        currentGame?.sram?.let { prevSram ->
                                            if (prevSram.isNotEmpty()) {
                                                webView.evaluateJavascript("window.injectSRAM('$prevSram')") {}
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay Loading Boot-screen if engine not ready
                    if (!isRomLoaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF39FF14))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = consoleMessage,
                                    color = Color(0xFF39FF14),
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Controller Dpad and Action Panel
                if (!isLandscape) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF16161D))
                    ) {
                        ControllerLayoutPortrait(webViewInstance)
                    }
                }
            }

            // Quick Floating overlay in landscape
            if (isLandscape) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PillButton("SELECT", webViewInstance, BTN_SELECT)
                        PillButton("START", webViewInstance, BTN_START)
                    }
                }
            }

            // Menu Bottom Sheet Drawer
            if (showMenu) {
                ModalBottomSheet(
                    onDismissRequest = { showMenu = false },
                    sheetState = sheetState,
                    containerColor = Color(0xFF16161D),
                    tonalElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "EMULATOR UTILITIES",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF00FF)
                        )
                        HorizontalDivider(color = Color(0xFFFF00FF).copy(alpha = 0.3f))
                        
                        MenuItem(
                            icon = Icons.Default.Save, 
                            title = stringResource(R.string.emulator_quick_save),
                            tint = Color(0xFF39FF14)
                        ) {
                            // Fetch save data from JS and write it into local database!
                            webViewInstance?.let { webView ->
                                webView.evaluateJavascript("window.getSRAM()") { sramBase64 ->
                                    if (!sramBase64.isNullOrEmpty() && sramBase64 != "null" && sramBase64 != "\"\"") {
                                        val cleanSram = sramBase64.trim('"')
                                        viewModel.updateGameSram(gameId, cleanSram)
                                    }
                                }
                                webView.evaluateJavascript("window.getSaveState()") { saveStateJson ->
                                    if (!saveStateJson.isNullOrEmpty() && saveStateJson != "null") {
                                        viewModel.updateGameSaveState(gameId, saveStateJson)
                                    }
                                }
                            }
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showMenu = false }
                        }

                        MenuItem(
                            icon = Icons.Default.Save, 
                            title = stringResource(R.string.emulator_quick_load),
                            tint = Color(0xFF00FFFF)
                        ) {
                            // Read state from DB and write inside running instance!
                            webViewInstance?.let { webView ->
                                currentGame?.sram?.let { prevSram ->
                                    if (prevSram.isNotEmpty()) {
                                        webView.evaluateJavascript("window.injectSRAM('$prevSram')") {}
                                    }
                                }
                                currentGame?.saveState?.let { prevSave ->
                                    if (prevSave.isNotEmpty()) {
                                        webView.evaluateJavascript("window.loadSaveState($prevSave)") {}
                                    }
                                }
                            }
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showMenu = false }
                        }

                        MenuItem(
                            icon = Icons.Default.FastForward, 
                            title = if (isFastForward) "NORMAL SPEED (1x)" else "FAST FORWARD (2.5x)",
                            tint = Color(0xFFFF003C)
                        ) {
                            isFastForward = !isFastForward
                            webViewInstance?.evaluateJavascript("window.setFastForward($isFastForward)") {}
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showMenu = false }
                        }

                        Button(
                            onClick = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showMenu = false
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF003C)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.emulator_exit),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ControllerLayoutPortrait(webView: WebView?) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // GBA L Shoulder Trigger Button (Top Left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            ShoulderButton("L", webView, BTN_L)
        }

        // GBA R Shoulder Trigger Button (Top Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            ShoulderButton("R", webView, BTN_R)
        }

        // Dpads Center Left
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            DpadsSection(webView)
        }
        
        // Start Select Center Bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            PillButton("SELECT", webView, BTN_SELECT)
            PillButton("START", webView, BTN_START)
        }

        // Action Buttons Center Right (Styled diagonal A & B layout)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 16.dp, bottom = 16.dp)
        ) {
            ActionButtonsSection(webView)
        }
    }
}

@Composable
fun ShoulderButton(label: String, webView: WebView?, keyIndex: Int) {
    Box(
        modifier = Modifier
            .width(76.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF2B2B36))
            .border(BorderStroke(1.5.dp, Color(0xFFFFCC00).copy(alpha = 0.6f)), RoundedCornerShape(6.dp))
            .onTouchGesture(
                onDown = { webView?.evaluateJavascript("window.pressButton($keyIndex)") {} },
                onUp = { webView?.evaluateJavascript("window.releaseButton($keyIndex)") {} }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label, 
            color = Color(0xFFFFCC00), 
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun DpadsSection(webView: WebView?, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DpadButton("▲", webView, BTN_UP)
            Row {
                DpadButton("◀", webView, BTN_LEFT)
                Spacer(modifier = Modifier.size(52.dp)) // Crisp center gap
                DpadButton("▶", webView, BTN_RIGHT)
            }
            DpadButton("▼", webView, BTN_DOWN)
        }
    }
}

@Composable
fun ActionButtonsSection(webView: WebView?, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ActionButton("B", Color(0xFFFF003C), webView, BTN_B, Modifier.offset(y = 20.dp))
            ActionButton("A", Color(0xFF39FF14), webView, BTN_A, Modifier.offset(y = (-20).dp))
        }
    }
}

@Composable
fun DpadButton(label: String, webView: WebView?, keyIndex: Int) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(Color(0xFF2B2B36), RoundedCornerShape(8.dp))
            .border(BorderStroke(1.5.dp, Color(0xFF39FF14).copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
            .onTouchGesture(
                onDown = { webView?.evaluateJavascript("window.pressButton($keyIndex)") {} },
                onUp = { webView?.evaluateJavascript("window.releaseButton($keyIndex)") {} }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label, 
            color = Color(0xFF39FF14), 
            fontSize = 20.sp, 
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ActionButton(label: String, col: Color, webView: WebView?, keyIndex: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color(0xFF1A1A24))
            .border(BorderStroke(2.5.dp, col), CircleShape)
            .onTouchGesture(
                onDown = { webView?.evaluateJavascript("window.pressButton($keyIndex)") {} },
                onUp = { webView?.evaluateJavascript("window.releaseButton($keyIndex)") {} }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label, 
            color = col, 
            fontSize = 22.sp, 
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun PillButton(label: String, webView: WebView?, keyIndex: Int) {
    Box(
        modifier = Modifier
            .width(72.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2B2B36))
            .border(BorderStroke(1.5.dp, Color(0xFF00FFFF).copy(alpha = 0.6f)), RoundedCornerShape(14.dp))
            .onTouchGesture(
                onDown = { webView?.evaluateJavascript("window.pressButton($keyIndex)") {} },
                onUp = { webView?.evaluateJavascript("window.releaseButton($keyIndex)") {} }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label, 
            color = Color(0xFF00FFFF), 
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

@Composable
fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    title: String, 
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2B2B36))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = tint)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title, 
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}

// Canonical extension to handle touch gestures down/up in Jetpack Compose
fun Modifier.onTouchGesture(
    onDown: () -> Unit,
    onUp: () -> Unit
): Modifier = this.pointerInput(Unit) {
    forEachGesture {
        awaitPointerEventScope {
            awaitFirstDown(requireUnconsumed = false)
            onDown()
            waitForUpOrCancellation()
            onUp()
        }
    }
}
