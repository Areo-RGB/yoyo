package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

data class ProtocolDef(val level: Int, val speed: Double, val shuttles: Int)

data class Shuttle(
    val level: Int,
    val shuttleNum: Int,
    val speed: Double,
    val runTime: Double,
    val restTime: Double,
    val accumulatedDistance: Double
)

enum class Phase { IDLE, RUN, REST, DONE }
enum class ParticipantState { ACTIVE, WARNED, SAVED }

data class Participant(
    val id: Int,
    val name: String,
    val state: ParticipantState = ParticipantState.ACTIVE,
    val savedDistance: Int? = null
)

data class TrackerState(
    val phase: Phase = Phase.IDLE,
    val currentShuttleIndex: Int = 0,
    val phaseElapsed: Double = 0.0,
    val isPlaying: Boolean = false,
    val participants: List<Participant>,
    val globalLiveDistance: Double = 0.0
) {
    val shuttles: List<Shuttle> = generateShuttles()
    val currentShuttle: Shuttle = shuttles[min(currentShuttleIndex, shuttles.size - 1)]
    
    val phaseDuration = when (phase) {
        Phase.IDLE, Phase.RUN -> currentShuttle.runTime
        Phase.REST -> currentShuttle.restTime
        Phase.DONE -> 0.0
    }
    
    val timeLeft = maxOf(0.0, phaseDuration - phaseElapsed)

    companion object {
        fun generateShuttles(): List<Shuttle> {
            val protocolDefs = listOf(
                ProtocolDef(5, 10.0, 1),
                ProtocolDef(9, 12.0, 1),
                ProtocolDef(11, 13.0, 2),
                ProtocolDef(12, 13.5, 3),
                ProtocolDef(13, 14.0, 4),
                ProtocolDef(14, 14.5, 8),
                ProtocolDef(15, 15.0, 8),
                ProtocolDef(16, 15.5, 8),
                ProtocolDef(17, 16.0, 8),
                ProtocolDef(18, 16.5, 8),
                ProtocolDef(19, 17.0, 8),
                ProtocolDef(20, 17.5, 8)
            )
            val shuttles = mutableListOf<Shuttle>()
            var accDistance = 0.0
            for (def in protocolDefs) {
                for (i in 1..def.shuttles) {
                    accDistance += 40.0
                    val runTime = 40.0 / (def.speed / 3.6)
                    shuttles.add(
                        Shuttle(
                            level = def.level,
                            shuttleNum = i,
                            speed = def.speed,
                            runTime = runTime,
                            restTime = 10.0,
                            accumulatedDistance = accDistance
                        )
                    )
                }
            }
            return shuttles
        }
        
        fun initialParticipants(): List<Participant> {
            val names = listOf("Silas", "Finley", "Eray", "Arvid", "Lion", "Jakob", "Paul", "Lennox", "Levi", "Lasse", "Berat", "Lionell")
            return names.mapIndexed { index, name -> Participant(id = index, name = name) }
        }
    }
}

class YoYoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TrackerState(participants = TrackerState.initialParticipants()))
    val uiState: StateFlow<TrackerState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var lastTimestamp: Long = 0

    fun togglePlayPause() {
        if (_uiState.value.phase == Phase.DONE) return

        if (_uiState.value.isPlaying) {
            pause()
        } else {
            start()
        }
    }

    private fun start() {
        _uiState.update { it.copy(isPlaying = true) }
        lastTimestamp = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(16) // ~60fps
                val now = System.currentTimeMillis()
                val delta = (now - lastTimestamp) / 1000.0
                lastTimestamp = now
                tick(delta)
            }
        }
    }

    private fun pause() {
        _uiState.update { it.copy(isPlaying = false) }
        timerJob?.cancel()
    }

    fun reset() {
        pause()
        _uiState.update {
            TrackerState(participants = TrackerState.initialParticipants())
        }
    }

    private fun tick(delta: Double) {
        _uiState.update { state ->
            if (!state.isPlaying) return@update state

            var newPhase = state.phase
            var newPhaseElapsed = state.phaseElapsed
            var newIndex = state.currentShuttleIndex
            var newIsPlaying = state.isPlaying

            if (newPhase == Phase.IDLE) {
                newPhase = Phase.RUN
                newPhaseElapsed = 0.0
            }

            newPhaseElapsed += delta
            var currentShuttle = state.shuttles[min(newIndex, state.shuttles.size - 1)]
            var phaseDuration = if (newPhase == Phase.RUN) currentShuttle.runTime else currentShuttle.restTime

            if (newPhaseElapsed >= phaseDuration) {
                if (newPhase == Phase.RUN) {
                    newPhase = Phase.REST
                    newPhaseElapsed = 0.0
                } else if (newPhase == Phase.REST) {
                    newPhase = Phase.RUN
                    newPhaseElapsed = 0.0
                    newIndex++
                    if (newIndex >= state.shuttles.size) {
                        newPhase = Phase.DONE
                        newIsPlaying = false
                    }
                }
            }

            // Recalculate distance
            currentShuttle = state.shuttles[min(newIndex, state.shuttles.size - 1)]
            var dist = currentShuttle.accumulatedDistance
            if (newPhase == Phase.RUN) {
                dist -= 40.0
                dist += 40.0 * min(1.0, newPhaseElapsed / currentShuttle.runTime)
            } else if (newPhase == Phase.IDLE) {
                dist = 0.0
            }

            state.copy(
                phase = newPhase,
                phaseElapsed = newPhaseElapsed,
                currentShuttleIndex = newIndex,
                isPlaying = newIsPlaying,
                globalLiveDistance = dist
            )
        }
    }

    fun onParticipantClick(participant: Participant) {
        _uiState.update { state ->
            val currentState = participant.state
            val updatedParticipants = state.participants.map { p ->
                if (p.id == participant.id) {
                    when (currentState) {
                        ParticipantState.ACTIVE -> p.copy(state = ParticipantState.WARNED)
                        ParticipantState.WARNED -> p.copy(
                            state = ParticipantState.SAVED,
                            savedDistance = state.globalLiveDistance.toInt()
                        )
                        ParticipantState.SAVED -> p
                    }
                } else p
            }
            state.copy(participants = updatedParticipants)
        }
    }
}
