package com.example.model

data class Game(
    val id: String,
    val name: String,
    val path: String,
    val type: GameType,
    val thumbnailUri: String? = null,
    val boxArtUrl: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val rating: Float? = null,
    val sram: String? = null,
    val saveState: String? = null
)

enum class GameType {
    GBA, GBC, GB, ZIP, UNKNOWN;
    
    companion object {
        fun fromExtension(ext: String): GameType = when (ext.lowercase()) {
            "gba" -> GBA
            "gbc" -> GBC
            "gb" -> GB
            "zip" -> ZIP
            else -> UNKNOWN
        }
    }
}
