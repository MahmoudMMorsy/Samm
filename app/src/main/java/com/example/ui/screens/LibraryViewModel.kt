package com.example.ui.screens

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GameEntity
import com.example.data.GameRepository
import com.example.model.Game
import com.example.model.GameType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID
import java.util.zip.ZipInputStream

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GameRepository
    private val context = application

    private val _isDownloading = MutableStateFlow<String?>(null)
    val isDownloading: StateFlow<String?> = _isDownloading.asStateFlow()

    init {
        val gameDao = AppDatabase.getDatabase(application).gameDao()
        repository = GameRepository(gameDao)
        File(context.filesDir, "games").mkdirs()
    }

    fun downloadDemoGame(name: String, urlString: String, extension: String) {
        viewModelScope.launch {
            _isDownloading.value = "جاري تحميل $name..."
            withContext(Dispatchers.IO) {
                try {
                    val url = URL(urlString)
                    val connection = url.openConnection()
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    val inputStream = connection.getInputStream()
                    val fileName = "${name.replace(" ", "_").lowercase()}.$extension"
                    val destFile = File(File(context.filesDir, "games"), fileName)
                    FileOutputStream(destFile).use { fos ->
                        inputStream.copyTo(fos)
                    }
                    addGame(name, destFile.absolutePath, extension)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _isDownloading.value = null
                }
            }
        }
    }

    val games: StateFlow<List<Game>> = repository.allGames.map { entities ->
        entities.map { entity ->
            Game(
                id = entity.id,
                name = entity.name,
                path = entity.path,
                type = GameType.fromExtension(entity.type),
                thumbnailUri = entity.thumbnailUri,
                boxArtUrl = entity.boxArtUrl,
                description = entity.description,
                genre = entity.genre,
                rating = entity.rating,
                sram = entity.sram,
                saveState = entity.saveState
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateGameSram(gameId: String, sramBase64: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSRAM(gameId, sramBase64)
        }
    }

    fun updateGameSaveState(gameId: String, saveStateJson: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSaveState(gameId, saveStateJson)
        }
    }

    fun processDroppedFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = context.contentResolver
            val displayName = getFileName(uri) ?: "Unknown"
            val ext = displayName.substringAfterLast('.', "").lowercase()

            if (ext == "zip") {
                extractAndAddZip(uri)
            } else if (ext in listOf("gba", "gbc", "gb")) {
                val destFile = copyToLocalDir(uri, displayName)
                if (destFile != null) {
                    addGame(displayName, destFile.absolutePath, ext)
                }
            }
        }
    }

    private suspend fun extractAndAddZip(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.split("/").last()
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext in listOf("gba", "gbc", "gb")) {
                            val destFile = File(File(context.filesDir, "games"), name)
                            FileOutputStream(destFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            addGame(name, destFile.absolutePath, ext)
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun copyToLocalDir(uri: Uri, fileName: String): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val destFile = File(File(context.filesDir, "games"), fileName)
            FileOutputStream(destFile).use { fos ->
                inputStream.copyTo(fos)
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addGame(name: String, path: String, extension: String) {
        viewModelScope.launch {
            // Mock metadata for cinematic feel
            val (boxArt, description, genre, rating) = when {
                name.contains("Tobu", true) -> listOf(
                    "https://mrombout.github.io/tobu-tobu-girl/img/tobu-tobu-girl-box.png",
                    "A challenging arcade platformer for the Game Boy. Help the girl rescue her cat by jumping on enemies!",
                    "Action/Platformer",
                    4.8f
                )
                name.contains("uCity", true) || name.contains("ucity", true) -> listOf(
                    "https://vhelin.github.io/ucity/img/ucity-boxart.png",
                    "A complete city building simulator for the Game Boy Color. Manage taxes, zones, and disasters!",
                    "Simulation",
                    4.6f
                )
                name.contains("Flappy", true) -> listOf(
                    "https://raw.githubusercontent.com/GrizzlyCove/FlappyBoy/master/screenshot.png",
                    "A classic flappy bird clone optimized for original 8-bit hardware. Simple but addictive.",
                    "Arcade",
                    4.2f
                )
                else -> listOf(null, "No description available.", "Unknown", 0f)
            }

            repository.insert(
                GameEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    path = path,
                    type = extension,
                    boxArtUrl = boxArt as String?,
                    description = description as String?,
                    genre = genre as String?,
                    rating = rating as Float?
                )
            )
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        result = cursor.getString(idx)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
