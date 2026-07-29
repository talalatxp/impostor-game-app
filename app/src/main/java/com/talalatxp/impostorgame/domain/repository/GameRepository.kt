package com.talalatxp.impostorgame.domain.repository

import com.talalatxp.impostorgame.domain.model.AppData
import com.talalatxp.impostorgame.domain.model.Category
import com.talalatxp.impostorgame.domain.model.GameSettings
import com.talalatxp.impostorgame.domain.model.GameStats
import com.talalatxp.impostorgame.domain.model.Player

interface GameRepository {
    suspend fun load(): AppData
    suspend fun saveCategories(categories: List<Category>)
    suspend fun savePlayers(players: List<Player>)
    suspend fun saveSettings(settings: GameSettings)
    suspend fun saveStats(stats: GameStats)
}

