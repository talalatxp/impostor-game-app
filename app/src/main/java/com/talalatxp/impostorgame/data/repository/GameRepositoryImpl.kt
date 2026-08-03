package com.talalatxp.impostorgame.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.talalatxp.impostorgame.data.local.DefaultCategories
import com.talalatxp.impostorgame.domain.model.*
import com.talalatxp.impostorgame.domain.repository.GameRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.gameDataStore by preferencesDataStore("impostor_game")

class GameRepositoryImpl(private val context: Context) : GameRepository {
    private companion object {
        const val CURRENT_RANKING_VERSION = 2
        const val CURRENT_CATEGORY_BANK_VERSION = 2
    }

    private object Keys {
        val categories = stringPreferencesKey("categories")
        val players = stringPreferencesKey("players")
        val settings = stringPreferencesKey("settings")
        val stats = stringPreferencesKey("stats")
        val generalRanking = stringPreferencesKey("general_ranking")
        val rankingVersion = intPreferencesKey("ranking_version")
        val categoryBankVersion = intPreferencesKey("category_bank_version")
    }

    override suspend fun load(): AppData {
        val data = context.gameDataStore.data.first()
        val shouldResetRanking = data[Keys.rankingVersion] != CURRENT_RANKING_VERSION
        val shouldReplaceCategoryBank = data[Keys.categoryBankVersion] != CURRENT_CATEGORY_BANK_VERSION
        if (shouldResetRanking) {
            context.gameDataStore.edit {
                it[Keys.generalRanking] = "[]"
                it[Keys.rankingVersion] = CURRENT_RANKING_VERSION
            }
        }
        if (shouldReplaceCategoryBank) {
            context.gameDataStore.edit {
                it[Keys.categories] = encodeCategories(DefaultCategories.items)
                it[Keys.categoryBankVersion] = CURRENT_CATEGORY_BANK_VERSION
            }
        }
        return AppData(
            categories = if (shouldReplaceCategoryBank) DefaultCategories.items else data[Keys.categories]?.let(::decodeCategories) ?: DefaultCategories.items,
            savedPlayers = data[Keys.players]?.let(::decodePlayers) ?: emptyList(),
            settings = data[Keys.settings]?.let(::decodeSettings) ?: GameSettings(),
            stats = data[Keys.stats]?.let(::decodeStats) ?: GameStats(),
            generalRanking = if (shouldResetRanking) emptyList() else data[Keys.generalRanking]?.let(::decodeRanking) ?: emptyList(),
        )
    }

    override suspend fun saveCategories(categories: List<Category>) {
        context.gameDataStore.edit {
            it[Keys.categories] = encodeCategories(categories)
            it[Keys.categoryBankVersion] = CURRENT_CATEGORY_BANK_VERSION
        }
    }
    override suspend fun savePlayers(players: List<Player>) {
        context.gameDataStore.edit { it[Keys.players] = encodePlayers(players) }
    }
    override suspend fun saveSettings(settings: GameSettings) {
        context.gameDataStore.edit { it[Keys.settings] = encodeSettings(settings) }
    }
    override suspend fun saveStats(stats: GameStats) {
        context.gameDataStore.edit { it[Keys.stats] = JSONObject().put("innocent", stats.innocentWins).put("impostor", stats.impostorWins).toString() }
    }
    override suspend fun saveGeneralRanking(ranking: List<PlayerScore>) {
        context.gameDataStore.edit {
            it[Keys.generalRanking] = encodeRanking(ranking)
            it[Keys.rankingVersion] = CURRENT_RANKING_VERSION
        }
    }

    private fun encodeCategories(categories: List<Category>) = JSONArray().apply {
        categories.forEach { category -> put(JSONObject().apply {
            put("id", category.id); put("name", category.name); put("description", category.description); put("custom", category.isCustom)
            put("words", JSONArray().apply { category.words.forEach { word -> put(JSONObject().put("id", word.id).put("word", word.word).put("clue", word.clue).put("used", word.isUsed)) } })
        }) }
    }.toString()
    private fun decodeCategories(raw: String) = JSONArray(raw).let { array -> List(array.length()) { i -> array.getJSONObject(i).let { json ->
        Category(json.getString("id"), json.getString("name"), json.optString("description"), json.getJSONArray("words").let { words -> List(words.length()) { j -> words.getJSONObject(j).let { WordItem(it.getString("id"), it.getString("word"), it.getString("clue"), it.optBoolean("used")) } } }, json.optBoolean("custom"))
    } } }
    private fun encodePlayers(players: List<Player>) = JSONArray().apply { players.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) } }.toString()
    private fun decodePlayers(raw: String) = JSONArray(raw).let { array -> List(array.length()) { i -> array.getJSONObject(i).let { Player(it.getString("id"), it.getString("name")) } } }
    private fun encodeSettings(value: GameSettings) = JSONObject().put("impostors", value.impostorsCount).put("timer", value.timerDurationSeconds).put("clue", value.impostorGetsClue).put("categories", JSONArray(value.selectedCategoryIds.toList())).put("mode", value.mode.name).toString()
    private fun decodeSettings(raw: String) = JSONObject(raw).let { json -> GameSettings(json.optInt("impostors", 1), json.optInt("timer", 120), json.optJSONArray("categories")?.let { array -> List(array.length()) { array.getString(it) }.toSet() } ?: emptySet(), json.optBoolean("clue", true), runCatching { GameMode.valueOf(json.optString("mode", GameMode.STANDARD.name)) }.getOrDefault(GameMode.STANDARD)) }
    private fun decodeStats(raw: String) = JSONObject(raw).let { GameStats(it.optInt("innocent"), it.optInt("impostor")) }
    private fun encodeRanking(ranking: List<PlayerScore>) = JSONArray().apply {
        ranking.forEach { score ->
            put(
                JSONObject()
                    .put("playerId", score.playerId)
                    .put("playerName", score.playerName)
                    .put("points", score.points),
            )
        }
    }.toString()
    private fun decodeRanking(raw: String) = JSONArray(raw).let { array ->
        List(array.length()) { index ->
            array.getJSONObject(index).let {
                PlayerScore(
                    playerId = it.getString("playerId"),
                    playerName = it.getString("playerName"),
                    points = it.optInt("points"),
                )
            }
        }
    }
}
