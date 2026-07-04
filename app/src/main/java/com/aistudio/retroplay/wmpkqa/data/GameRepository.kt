package com.aistudio.retroplay.wmpkqa.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()

    suspend fun getGameById(id: String): GameEntity? {
        return gameDao.getGameById(id)
    }

    suspend fun updateSRAM(id: String, sram: String?) {
        gameDao.updateSRAM(id, sram)
    }

    suspend fun updateSaveState(id: String, saveState: String?) {
        gameDao.updateSaveState(id, saveState)
    }

    suspend fun insert(game: GameEntity) {
        gameDao.insertGame(game)
    }

    suspend fun deleteById(id: String) {
        gameDao.deleteGameById(id)
    }
}
