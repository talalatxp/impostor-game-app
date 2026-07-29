package com.talalatxp.impostorgame.domain.model

data class WordItem(val id: String, val word: String, val clue: String)

data class Category(
    val id: String,
    val name: String,
    val description: String,
    val words: List<WordItem>,
    val isCustom: Boolean = false,
)

data class Player(val id: String, val name: String)

data class GameSettings(
    val impostorsCount: Int = 1,
    val timerDurationSeconds: Int = 120,
    val selectedCategoryIds: Set<String> = emptySet(),
    val impostorGetsClue: Boolean = true,
)

data class PlayerRole(val player: Player, val isImpostor: Boolean, val clue: String?)

data class GameSession(
    val word: WordItem,
    val roles: List<PlayerRole>,
    val settings: GameSettings,
)

data class GameStats(val innocentWins: Int = 0, val impostorWins: Int = 0)

data class AppData(
    val categories: List<Category>,
    val savedPlayers: List<Player>,
    val settings: GameSettings,
    val stats: GameStats,
)

