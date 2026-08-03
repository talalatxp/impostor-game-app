package com.talalatxp.impostorgame.domain.usecase

import com.talalatxp.impostorgame.domain.model.Category
import com.talalatxp.impostorgame.domain.model.GameSession
import com.talalatxp.impostorgame.domain.model.GameSettings
import com.talalatxp.impostorgame.domain.model.GameMode
import com.talalatxp.impostorgame.domain.model.GameStats
import com.talalatxp.impostorgame.domain.model.Player
import com.talalatxp.impostorgame.domain.model.PlayerRole
import com.talalatxp.impostorgame.domain.model.PlayerScore
import com.talalatxp.impostorgame.domain.model.WordItem
import com.talalatxp.impostorgame.domain.repository.GameRepository
import kotlin.random.Random

class LoadGameDataUseCase(private val repository: GameRepository) { suspend operator fun invoke() = repository.load() }
class SaveCategoriesUseCase(private val repository: GameRepository) { suspend operator fun invoke(value: List<Category>) = repository.saveCategories(value) }
class SavePlayersUseCase(private val repository: GameRepository) { suspend operator fun invoke(value: List<Player>) = repository.savePlayers(value) }
class SaveSettingsUseCase(private val repository: GameRepository) { suspend operator fun invoke(value: GameSettings) = repository.saveSettings(value) }
class SaveStatsUseCase(private val repository: GameRepository) { suspend operator fun invoke(value: GameStats) = repository.saveStats(value) }
class SaveGeneralRankingUseCase(private val repository: GameRepository) { suspend operator fun invoke(value: List<PlayerScore>) = repository.saveGeneralRanking(value) }

class CreateGameUseCase {
    operator fun invoke(players: List<Player>, settings: GameSettings, categories: List<Category>): GameSession? {
        if (players.size < 3) return null
        val chaosCategory = if (settings.mode == GameMode.CHAOS) categories.filter { category -> category.words.any { !it.isUsed } }.randomOrNull() else null
        val effectiveSettings = if (settings.mode == GameMode.CHAOS) {
            val maxImpostors = (players.size - 2).coerceAtLeast(1)
            settings.copy(impostorsCount = Random.nextInt(1, maxImpostors + 1), selectedCategoryIds = chaosCategory?.let { setOf(it.id) } ?: emptySet())
        } else settings
        if (effectiveSettings.impostorsCount !in 1 until players.size) return null
        val allowed = categories.filter { effectiveSettings.selectedCategoryIds.isEmpty() || it.id in effectiveSettings.selectedCategoryIds }
            .flatMap { it.words.filterNot(WordItem::isUsed) }
        val word = allowed.randomOrNull() ?: return null
        val impostorIds = players.shuffled(Random.Default).take(effectiveSettings.impostorsCount).map { it.id }.toSet()
        return GameSession(word, players.shuffled(Random.Default).map { player ->
            val isImpostor = player.id in impostorIds
            PlayerRole(player, isImpostor, if (isImpostor && effectiveSettings.impostorGetsClue) word.clue else null)
        }, effectiveSettings)
    }
}
