package com.cutetool.mestyachkovoe.game

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.cutisStore by preferencesDataStore("cutis_game")

class CutisRepository(private val context: Context) {
    private object Keys {
        val STATE = stringPreferencesKey("cutis_state_v1")
    }

    val state: Flow<CutisState> = context.cutisStore.data.map { prefs ->
        runCatching { decode(prefs[Keys.STATE]) }.getOrNull() ?: CutisState()
    }

    suspend fun save(state: CutisState) {
        context.cutisStore.edit { prefs ->
            prefs[Keys.STATE] = encode(state)
        }
    }

    private fun encode(state: CutisState): String {
        val pets = JSONArray()
        state.pets.forEach { pet ->
            pets.put(
                JSONObject()
                    .put("id", pet.id)
                    .put("name", pet.name)
                    .put("home", pet.atHome)
                    .put("hunger", pet.stats.hunger.toDouble())
                    .put("sleep", pet.stats.sleep.toDouble())
                    .put("mood", pet.stats.mood.toDouble())
                    .put("hygiene", pet.stats.hygiene.toDouble())
            )
        }
        return JSONObject()
            .put("coins", state.coins)
            .put("room", state.room.name)
            .put("affection", state.affection)
            .put("lastTick", state.lastTick)
            .put("pets", pets)
            .toString()
    }

    private fun decode(raw: String?): CutisState? {
        if (raw.isNullOrBlank()) return null
        val obj = JSONObject(raw)
        val petsJson = obj.optJSONArray("pets")
        val pets = buildList {
            if (petsJson != null) {
                for (i in 0 until petsJson.length()) {
                    val p = petsJson.optJSONObject(i) ?: continue
                    val id = p.optString("id").ifBlank { "koti" }
                    add(
                        PetState(
                            id = id,
                            name = p.optString("name").ifBlank { "Кути" },
                            atHome = p.optBoolean("home", true),
                            stats = PetStats(
                                hunger = p.optDouble("hunger", 78.0).toFloat(),
                                sleep = p.optDouble("sleep", 82.0).toFloat(),
                                mood = p.optDouble("mood", 74.0).toFloat(),
                                hygiene = p.optDouble("hygiene", 88.0).toFloat(),
                            ).clamp(),
                        )
                    )
                }
            }
        }
        val now = System.currentTimeMillis()
        return CutisState(
            coins = obj.optInt("coins", 25),
            pets = pets.ifEmpty { listOf(PetState(id = "koti", name = "Кути")) },
            room = runCatching { RoomType.valueOf(obj.optString("room")) }
                .getOrDefault(RoomType.LIVING),
            affection = obj.optInt("affection", 0),
            lastTick = obj.optLong("lastTick", now),
        )
    }
}
