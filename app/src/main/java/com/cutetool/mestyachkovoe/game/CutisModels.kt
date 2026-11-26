package com.cutetool.mestyachkovoe.game

import kotlin.math.roundToInt

/** Комнаты дома. Пятую (ванную) тоже можно открыть, интерьер — Шаг 2. */
enum class RoomType(val title: String, val emoji: String) {
    BEDROOM("Спальня", "🛏"),
    LIVING("Гостиная", "🛋"),
    PLAYROOM("Игровая", "🕹"),
    KITCHEN("Кухня", "🍳"),
    BATHROOM("Ванная", "🛁"),
}

data class PetStats(
    val hunger: Float = 78f,
    val sleep: Float = 82f,
    val mood: Float = 74f,
    val hygiene: Float = 88f,
) {
    val average: Float get() = (hunger + sleep + mood + hygiene) / 4f
    val lowest: Float get() = minOf(hunger, sleep, mood, hygiene)

    fun clamp() = PetStats(
        hunger = hunger.coerceIn(0f, 100f),
        sleep = sleep.coerceIn(0f, 100f),
        mood = mood.coerceIn(0f, 100f),
        hygiene = hygiene.coerceIn(0f, 100f),
    )

    fun adjusted(
        hunger: Float = 0f,
        sleep: Float = 0f,
        mood: Float = 0f,
        hygiene: Float = 0f,
    ) = PetStats(
        hunger = this.hunger + hunger,
        sleep = this.sleep + sleep,
        mood = this.mood + mood,
        hygiene = this.hygiene + hygiene,
    ).clamp()

    fun rounded() = PetStats(
        hunger = hunger.roundToInt().toFloat(),
        sleep = sleep.roundToInt().toFloat(),
        mood = mood.roundToInt().toFloat(),
        hygiene = hygiene.roundToInt().toFloat(),
    )
}

data class PetState(
    val id: String,
    val name: String,
    val stats: PetStats = PetStats(),
    val atHome: Boolean = true,
)

data class CutisState(
    val coins: Int = 25,
    val pets: List<PetState> = listOf(PetState(id = "koti", name = "Кути")),
    val room: RoomType = RoomType.LIVING,
    val affection: Int = 0,
    val lastTick: Long = System.currentTimeMillis(),
)

enum class Mood { LOVE, HAPPY, NEUTRAL, SAD, HUNGRY }

object Kaomoji {
    val love = listOf(
        "(⁠*⁠´⁠ω⁠｀⁠*⁠)",
        "(⁠つ⁠≧⁠▽⁠≦⁠)⁠つ",
        "(⁠⁄⁠⁄⁠•⁠⁄⁠-⁠⁄⁠•⁠⁄⁠⁄⁠)",
        "(⁠♡⁠ω⁠♡⁠)",
        "ヽ(⁠>⁠▽⁠<⁠)⁠ﾉ",
    )
    val happy = listOf(
        "(⁠=⁠^⁠･⁠ｪ⁠･⁠^⁠=⁠)",
        "(⁠๑⁠•⁠ө⁠•⁠๑⁠)",
        "(⁠≧⁠▽⁠≦⁠)",
        "(⁠•⁠‿⁠•⁠)✧",
        "(⁠*⁠꒳⁠*⁠)",
    )
    val neutral = listOf(
        "(⁠・⁠ω⁠・⁠)",
        "(⁠￣⁠ω⁠￣⁠)",
        "(⁠•⁠_⁠•⁠)",
        "(⁠´⁠-⁠ω⁠-⁠`)",
    )
    val sad = listOf(
        "(⁠╥⁠﹏⁠╥⁠)",
        "(⁠；⁠ω⁠；⁠)",
        "(⁠◞⁠‸⁠◟⁠)",
        "(⁠´⁠；⁠ω⁠；⁠`)",
    )
    val hungry = listOf(
        "(⁠´⁠⊙⁠ω⁠⊙⁠`)",
        "(⁠￣﹃￣⁠)",
        "(⁠・⁠∀⁠・⁠)…🍙",
    )
    val sleepy = listOf("(⁠￣⁠o⁠￣⁠)… zzz", "(⁠-⁠_⁠-⁠)… zzz", "(⁠´⁠-⁠ω⁠-⁠`) zzz")
    val broke = listOf("(⁠；⁠ω⁠；⁠) нет у.е.…", "(⁠´⁠；⁠ω⁠；⁠`) пусто в кармане")
    val pat = listOf("(⁠*⁠´⁠ω⁠｀⁠*⁠)", "(⁠≧⁠ω⁠≦⁠)", "(⁠♡⁠ω⁠♡⁠)")

    fun forMood(mood: Mood): String = when (mood) {
        Mood.LOVE -> love.random()
        Mood.HAPPY -> happy.random()
        Mood.NEUTRAL -> neutral.random()
        Mood.SAD -> sad.random()
        Mood.HUNGRY -> hungry.random()
    }
}

object CutisEngine {
    /** Медленное падение: за сутки статы теряют примерно 20–27%. */
    const val HUNGER_PER_HOUR = 1.15f
    const val SLEEP_PER_HOUR = 0.95f
    const val MOOD_PER_HOUR = 1.05f
    const val HYGIENE_PER_HOUR = 0.85f

    const val MAX_OFFLINE_HOURS = 72f
    const val PASSIVE_INCOME_CAP = 12

    fun advance(state: CutisState, now: Long = System.currentTimeMillis()): CutisState {
        val elapsedMs = (now - state.lastTick).coerceAtLeast(0L)
        val hours = (elapsedMs / 3_600_000.0).toFloat()
        if (hours < 0.001f) return state.copy(lastTick = now)

        val capped = hours.coerceAtMost(MAX_OFFLINE_HOURS)
        var income = 0f

        val pets = state.pets.map { pet ->
            val before = pet.stats
            val after = before.adjusted(
                hunger = -HUNGER_PER_HOUR * capped,
                sleep = -SLEEP_PER_HOUR * capped,
                mood = -MOOD_PER_HOUR * capped,
                hygiene = -HYGIENE_PER_HOUR * capped,
            )
            if (pet.atHome && before.lowest >= 10f && after.lowest >= 10f) {
                income += capped
            }
            pet.copy(stats = after)
        }

        val earned = income.toInt().coerceIn(0, PASSIVE_INCOME_CAP)

        return state.copy(
            pets = pets,
            coins = state.coins + earned,
            lastTick = now,
        )
    }

    fun moodOf(state: CutisState, pet: PetState = state.pets.first()): Mood = when {
        pet.stats.hunger < 18f -> Mood.HUNGRY
        pet.stats.lowest < 22f -> Mood.SAD
        pet.stats.average < 45f -> Mood.NEUTRAL
        state.affection >= 25 -> Mood.LOVE
        pet.stats.average > 68f -> Mood.HAPPY
        else -> Mood.NEUTRAL
    }

    fun isEarningIncome(pet: PetState): Boolean = pet.atHome && pet.stats.lowest >= 10f
}
