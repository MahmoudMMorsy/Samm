package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY timestamp DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: String): GameEntity?

    @Query("UPDATE games SET sram = :sram WHERE id = :id")
    suspend fun updateSRAM(id: String, sram: String?)

    @Query("UPDATE games SET saveState = :saveState WHERE id = :id")
    suspend fun updateSaveState(id: String, saveState: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: String)
}
