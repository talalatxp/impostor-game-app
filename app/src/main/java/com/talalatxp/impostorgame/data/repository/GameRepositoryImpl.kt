package com.talalatxp.impostorgame.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
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
    private object Keys {
        val categories = stringPreferencesKey("categories")
        val players = stringPreferencesKey("players")
        val settings = stringPreferencesKey("settings")
        val stats = stringPreferencesKey("stats")
    }

    override suspend fun load(): AppData {
        val data = context.gameDataStore.data.first()
        return AppData(
            categories = data[Keys.categories]?.let(::decodeCategories) ?: DefaultCategories.items,
            savedPlayers = data[Keys.players]?.let(::decodePlayers) ?: emptyList(),
            settings = data[Keys.settings]?.let(::decodeSettings) ?: GameSettings(),
            stats = data[Keys.stats]?.let(::decodeStats) ?: GameStats(),
        )
    }

    override suspend fun saveCategories(categories: List<Category>) = context.gameDataStore.edit { it[Keys.categories] = encodeCategories(categories) }
    override suspend fun savePlayers(players: List<Player>) = context.gameDataStore.edit { it[Keys.players] = encodePlayers(players) }
    override suspend fun saveSettings(settings: GameSettings) = context.gameDataStore.edit { it[Keys.settings] = encodeSettings(settings) }
    override suspend fun saveStats(stats: GameStats) = context.gameDataStore.edit { it[Keys.stats] = JSONObject().put("innocent", stats.innocentWins).put("impostor", stats.impostorWins).toString() }

    private fun encodeCategories(categories: List<Category>) = JSONArray().apply {
        categories.forEach { category -> put(JSONObject().apply {
            put("id", category.id); put("name", category.name); put("description", category.description); put("custom", category.isCustom)
            put("words", JSONArray().apply { category.words.forEach { word -> put(JSONObject().put("id", word.id).put("word", word.word).put("clue", word.clue)) } })
        }) }
    }.toString()
    private fun decodeCategories(raw: String) = JSONArray(raw).let { array -> List(array.length()) { i -> array.getJSONObject(i).let { json ->
        Category(json.getString("id"), json.getString("name"), json.optString("description"), json.getJSONArray("words").let { words -> List(words.length()) { j -> words.getJSONObject(j).let { WordItem(it.getString("id"), it.getString("word"), it.getString("clue")) } } }, json.optBoolean("custom"))
    } } }
    private fun encodePlayers(players: List<Player>) = JSONArray().apply { players.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) } }.toString()
    private fun decodePlayers(raw: String) = JSONArray(raw).let { array -> List(array.length()) { i -> array.getJSONObject(i).let { Player(it.getString("id"), it.getString("name")) } } }
    private fun encodeSettings(value: GameSettings) = JSONObject().put("impostors", value.impostorsCount).put("timer", value.timerDurationSeconds).put("clue", value.impostorGetsClue).put("categories", JSONArray(value.selectedCategoryIds.toList())).toString()
    private fun decodeSettings(raw: String) = JSONObject(raw).let { json -> GameSettings(json.optInt("impostors", 1), json.optInt("timer", 120), json.optJSONArray("categories")?.let { array -> List(array.length()) { array.getString(it) }.toSet() } ?: emptySet(), json.optBoolean("clue", true)) }
    private fun decodeStats(raw: String) = JSONObject(raw).let { GameStats(it.optInt("innocent"), it.optInt("impostor")) }
}
