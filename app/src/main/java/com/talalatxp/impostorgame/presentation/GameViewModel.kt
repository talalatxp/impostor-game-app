package com.talalatxp.impostorgame.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talalatxp.impostorgame.data.local.DefaultCategories
import com.talalatxp.impostorgame.domain.model.*
import com.talalatxp.impostorgame.domain.repository.GameRepository
import com.talalatxp.impostorgame.domain.usecase.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

enum class Screen { HOME, SETUP, CATEGORIES, ROLES, ROUND, RESULTS }
enum class Winner { INNOCENTS, IMPOSTORS }

data class GameUiState(
    val screen: Screen = Screen.HOME,
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val players: List<Player> = emptyList(),
    val settings: GameSettings = GameSettings(),
    val stats: GameStats = GameStats(),
    val generalRanking: List<PlayerScore> = emptyList(),
    val sessionRanking: List<PlayerScore> = emptyList(),
    val session: GameSession? = null,
    val roleIndex: Int = 0,
    val remainingSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val winner: Winner? = null,
    val error: String? = null,
)

class GameViewModel(repository: GameRepository) : ViewModel() {
    private val load = LoadGameDataUseCase(repository)
    private val saveCategories = SaveCategoriesUseCase(repository)
    private val savePlayers = SavePlayersUseCase(repository)
    private val saveSettings = SaveSettingsUseCase(repository)
    private val saveStats = SaveStatsUseCase(repository)
    private val saveGeneralRanking = SaveGeneralRankingUseCase(repository)
    private val createGame = CreateGameUseCase()
    private var timerJob: Job? = null

    var uiState: GameUiState by androidx.compose.runtime.mutableStateOf(GameUiState())
        private set

    init {
        viewModelScope.launch {
            val data = load()
            val settings = if (data.settings.timerDurationSeconds == 60) {
                data.settings.copy(timerDurationSeconds = 120)
            } else {
                data.settings
            }
            uiState = uiState.copy(
                categories = data.categories,
                players = data.savedPlayers,
                settings = settings,
                stats = data.stats,
                generalRanking = data.generalRanking,
                isLoading = false,
            )
            if (settings != data.settings) saveSettings(settings)
        }
    }

    fun navigate(screen: Screen) { uiState = uiState.copy(screen = screen, error = null) }

    fun goBack() {
        val previousScreen = when (uiState.screen) {
            Screen.SETUP, Screen.CATEGORIES -> Screen.HOME
            Screen.ROLES -> Screen.SETUP
            Screen.HOME, Screen.ROUND, Screen.RESULTS -> return
        }
        uiState = uiState.copy(
            screen = previousScreen,
            isTimerRunning = false,
            error = null,
        )
    }

    fun addPlayer(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank() || uiState.players.any { it.name.equals(cleanName, true) }) return
        updatePlayers(uiState.players + Player(UUID.randomUUID().toString(), cleanName))
    }

    fun removePlayer(player: Player) = updatePlayers(uiState.players - player)
    private fun updatePlayers(players: List<Player>) {
        uiState = uiState.copy(players = players)
        viewModelScope.launch { savePlayers(players) }
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        val next = transform(uiState.settings).let { it.copy(impostorsCount = it.impostorsCount.coerceIn(1, (uiState.players.size - 1).coerceAtLeast(1))) }
        uiState = uiState.copy(settings = next)
        viewModelScope.launch { saveSettings(next) }
    }

    fun toggleCategory(id: String) = updateSettings { settings ->
        val ids = settings.selectedCategoryIds.toMutableSet()
        if (!ids.add(id)) ids.remove(id)
        settings.copy(selectedCategoryIds = ids)
    }

    fun addCategory(name: String, description: String) {
        if (name.isBlank()) return
        val updated = uiState.categories + Category(UUID.randomUUID().toString(), name.trim(), description.trim(), emptyList(), true)
        updateCategories(updated)
    }

    fun addWord(categoryId: String, word: String, clue: String) {
        if (word.isBlank() || clue.isBlank()) return
        updateCategories(uiState.categories.map { category -> if (category.id == categoryId) category.copy(words = category.words + WordItem(UUID.randomUUID().toString(), word.trim(), clue.trim())) else category })
    }

    fun updateWord(categoryId: String, wordId: String, word: String, clue: String) {
        if (word.isBlank() || clue.isBlank()) return
        updateCategories(uiState.categories.map { category ->
            if (category.id == categoryId) category.copy(words = category.words.map { item ->
                if (item.id == wordId) item.copy(word = word.trim(), clue = clue.trim()) else item
            }) else category
        })
    }

    fun deleteWord(categoryId: String, wordId: String) = updateCategories(uiState.categories.map { category ->
        if (category.id == categoryId) category.copy(words = category.words.filterNot { it.id == wordId }) else category
    })

    fun setWordUsed(categoryId: String, wordId: String, isUsed: Boolean) = updateCategories(uiState.categories.map { category ->
        if (category.id == categoryId) category.copy(words = category.words.map { word ->
            if (word.id == wordId) word.copy(isUsed = isUsed) else word
        }) else category
    })

    fun renameCategory(categoryId: String, name: String) {
        if (name.isBlank()) return
        updateCategories(uiState.categories.map { if (it.id == categoryId) it.copy(name = name.trim()) else it })
    }
    fun deleteCategory(categoryId: String) = updateCategories(uiState.categories.filterNot { it.id == categoryId && it.isCustom })
    fun resetCategories() = updateCategories(DefaultCategories.items)
    fun generateTenCategories() = updateCategories(DefaultCategories.items.shuffled(Random.Default).take(10))
    private fun updateCategories(categories: List<Category>) {
        uiState = uiState.copy(categories = categories)
        viewModelScope.launch { saveCategories(categories) }
    }

    fun startGame() {
        val session = createGame(uiState.players, uiState.settings, uiState.categories)
        if (session == null) {
            uiState = uiState.copy(error = if (uiState.players.size < 3) "Añade al menos 3 jugadores." else "No quedan palabras disponibles. Reactiva algunas desde Categorías.")
            return
        }
        val categories = uiState.categories.map { category ->
            if (category.words.any { it.id == session.word.id }) {
                category.copy(words = category.words.map { word -> if (word.id == session.word.id) word.copy(isUsed = true) else word })
            } else category
        }
        updateCategories(categories)
        uiState = uiState.copy(categories = categories, session = session, roleIndex = 0, remainingSeconds = session.settings.timerDurationSeconds, winner = null, screen = Screen.ROLES, error = null)
    }

    fun nextRole() {
        val next = uiState.roleIndex + 1
        if (next < (uiState.session?.roles?.size ?: 0)) uiState = uiState.copy(roleIndex = next)
        else {
            uiState = uiState.copy(screen = Screen.ROUND)
            if (uiState.remainingSeconds > 0) toggleTimer()
        }
    }

    fun toggleTimer() {
        if (uiState.remainingSeconds <= 0 && uiState.session?.settings?.timerDurationSeconds != 0) return
        val start = !uiState.isTimerRunning
        uiState = uiState.copy(isTimerRunning = start)
        timerJob?.cancel()
        if (start) timerJob = viewModelScope.launch {
            while (uiState.remainingSeconds > 0 && uiState.isTimerRunning) {
                delay(1_000)
                uiState = uiState.copy(remainingSeconds = (uiState.remainingSeconds - 1).coerceAtLeast(0))
            }
            if (uiState.remainingSeconds == 0) uiState = uiState.copy(isTimerRunning = false)
        }
    }
    fun addTime(seconds: Int = 30) { uiState = uiState.copy(remainingSeconds = uiState.remainingSeconds + seconds) }
    fun finishRound() { timerJob?.cancel(); uiState = uiState.copy(isTimerRunning = false, screen = Screen.RESULTS) }
    fun recordWinner(winner: Winner) {
        if (uiState.winner != null) return
        val roles = uiState.session?.roles ?: return
        val stats = if (winner == Winner.INNOCENTS) uiState.stats.copy(innocentWins = uiState.stats.innocentWins + 1) else uiState.stats.copy(impostorWins = uiState.stats.impostorWins + 1)
        val generalRanking = addResultToRanking(uiState.generalRanking, roles, winner)
        val sessionRanking = addResultToRanking(uiState.sessionRanking, roles, winner)
        uiState = uiState.copy(
            stats = stats,
            generalRanking = generalRanking,
            sessionRanking = sessionRanking,
            winner = winner,
        )
        viewModelScope.launch {
            saveStats(stats)
            saveGeneralRanking(generalRanking)
        }
    }

    fun clearSessionRanking() {
        uiState = uiState.copy(sessionRanking = emptyList())
    }

    private fun addResultToRanking(
        current: List<PlayerScore>,
        roles: List<PlayerRole>,
        winner: Winner,
    ): List<PlayerScore> {
        val scores = current.associateBy { it.playerId }.toMutableMap()
        roles.forEach { role ->
            val previous = scores[role.player.id]
            val won = when (winner) {
                Winner.INNOCENTS -> !role.isImpostor
                Winner.IMPOSTORS -> role.isImpostor
            }
            val earnedPoints = when {
                !won -> 0
                role.isImpostor -> 2
                else -> 1
            }
            scores[role.player.id] = PlayerScore(
                playerId = role.player.id,
                playerName = role.player.name,
                points = (previous?.points ?: 0) + earnedPoints,
            )
        }
        return scores.values.sortedWith(
            compareByDescending<PlayerScore> { it.points }
                .thenBy { it.playerName.lowercase() },
        )
    }
}
