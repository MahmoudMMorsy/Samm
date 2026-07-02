package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val name: String,
    val path: String,
    val type: String,
    val thumbnailUri: String? = null,
    val boxArtUrl: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val rating: Float? = null,
    val sram: String? = null,
    val saveState: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
