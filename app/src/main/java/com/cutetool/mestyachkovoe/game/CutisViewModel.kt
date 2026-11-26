package com.cutetool.mestyachkovoe.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class Bubble(val id: Long, val text: String)

class CutisViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CutisRepository(app)

    private val _state = MutableStateFlow<CutisState?>(null)
    val state: StateFlow<CutisState?> = _state.asStateFlow()

    private val _bubble = MutableStateFlow<Bubble?>(null)
    val bubble: StateFlow<Bubble?> = _bubble.asStateFlow()

    private var bubbleJob: Job? = null
    private var unsavedTicks = 0

    init {
        viewModelScope.launch {
            val stored = repo.state.first()
            val fresh = CutisEngine.advance(stored)
            _state.value = fresh
            if (fresh != stored) repo.save(fresh)
            startTicker()
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                val current = _state.value ?: continue
                val next = CutisEngine.advance(current)
                if (next != current) {
                    _state.value = next
                    unsavedTicks++
                    if (unsavedTicks >= 5) {
                        unsavedTicks = 0
                        repo.save(next)
                    }
                }
            }
        }
    }

    fun persistNow() {
        val current = _state.value ?: return
        viewModelScope.launch { repo.save(current) }
    }

    fun say(text: String) {
        val b = Bubble(System.currentTimeMillis(), text)
        _bubble.value = b
        bubbleJob?.cancel()
        bubbleJob = viewModelScope.launch {
            delay(3600)
            if (_bubble.value?.id == b.id) _bubble.value = null
        }
    }

    private fun mutate(block: (CutisState) -> CutisState) {
        val current = _state.value ?: return
        val next = block(current)
        _state.value = next
        viewModelScope.launch { repo.save(next) }
    }

    private fun currentMood(): Mood {
        val s = _state.value ?: return Mood.NEUTRAL
        return CutisEngine.moodOf(s)
    }

    fun tapPet() {
        mutate { s -> s.copy(affection = s.affection + 1) }
        say(Kaomoji.forMood(currentMood()))
    }

    fun pet() {
        val cost = 0
        if (cost > 0) return
        mutate { s ->
            s.copy(
                affection = s.affection + 3,
                pets = s.pets.map { p ->
                    if (p.id == s.pets.first().id) p.copy(stats = p.stats.adjusted(mood = 6f)) else p
                },
            )
        }
        say(Kaomoji.pat.random())
    }

    fun feed() {
        val s = _state.value ?: return
        if (s.coins < SNACK_PRICE) {
            say(Kaomoji.broke.random())
            return
        }
        mutate { st ->
            st.copy(
                coins = st.coins - SNACK_PRICE,
                pets = st.pets.map { p ->
                    if (p.id == st.pets.first().id) {
                        p.copy(stats = p.stats.adjusted(hunger = 28f, mood = 3f))
                    } else p
                },
            )
        }
        say(listOf("(⁠*⁠´⁠ω⁠｀⁠*⁠) ням!", "(⁠≧⁠▽⁠≦⁠) вкусно!", "(⁠・⁠∀⁠・⁠)🍙").random())
    }

    fun sleep() {
        mutate { st ->
            st.copy(
                pets = st.pets.map { p ->
                    if (p.id == st.pets.first().id) {
                        p.copy(stats = p.stats.adjusted(sleep = 35f, mood = 4f))
                    } else p
                },
            )
        }
        say(Kaomoji.sleepy.random())
    }

    fun wash() {
        val s = _state.value ?: return
        if (s.coins < WASH_PRICE) {
            say(Kaomoji.broke.random())
            return
        }
        mutate { st ->
            st.copy(
                coins = st.coins - WASH_PRICE,
                pets = st.pets.map { p ->
                    if (p.id == st.pets.first().id) {
                        p.copy(stats = p.stats.adjusted(hygiene = 30f, mood = 3f))
                    } else p
                },
            )
        }
        say("(⁠๑⁠•⁠ө⁠•⁠๑⁠) чисто-чисто!")
    }

    fun selectRoom(room: RoomType) {
        mutate { it.copy(room = room) }
    }

    fun lockedPet(name: String, price: Int) {
        say("(⁠￣⁠ω⁠￣⁠) $name стоит $price у.е. — скоро!")
    }

    companion object {
        const val SNACK_PRICE = 1
        const val WASH_PRICE = 1
    }
}
