package com.example.api

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.sin

class CinematicSynth {
    private val TAG = "CinematicSynth"
    private val sampleRate = 22050
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var isRunning = false

    @Volatile
    private var currentEmo = "peaceful"

    @Volatile
    private var isMuted = false

    // Oscillator phases
    private var phase1 = 0.0
    private var phase2 = 0.0
    private var phaseSweep = 0.0
    private var phaseBeep = 0.0

    // Sound generation state
    private var sweepFreq = 200f
    private var sweepDirection = 1
    private var beepTimer = 0
    private var synthTimer = 0L

    fun start() {
        if (isRunning) return
        isRunning = true

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
            isRunning = false
            return
        }

        synthJob = scope.launch {
            val shortBuffer = ShortArray(1024)
            while (isRunning) {
                if (isMuted) {
                    shortBuffer.fill(0)
                } else {
                    generateSamples(shortBuffer)
                }
                audioTrack?.write(shortBuffer, 0, shortBuffer.size)
            }
        }
        Log.d(TAG, "Synth started successfully")
    }

    fun updateScene(emo: String) {
        currentEmo = emo.trim().lowercase()
        Log.d(TAG, "Synth updated emotional resonance target: $currentEmo")
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun stop() {
        isRunning = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Safe ignore
        }
        audioTrack = null
        Log.d(TAG, "Synth stopped and resources released")
    }

    private fun generateSamples(buffer: ShortArray) {
        // Base carrier frequencies depending on emotional resonance
        val (freq1, freq2) = when (currentEmo) {
            "alarm", "panic", "defiance", "anger" -> Pair(130.0, 195.0) // Tense, dissonant intervals (Tritone or near)
            "grief", "sorrow", "confusion", "gloom" -> Pair(73.4, 87.3) // Deep, heavy melancholy minor third drone
            "wonder", "creativity", "curious", "curiosity", "hope" -> Pair(146.8, 220.0) // Ethereal open fifths (D3 / A3)
            "serenity", "peaceful", "warmth", "comfort", "fulfillment", "transcendence" -> Pair(110.0, 165.0) // Warm, stable root fifth (A2 / E3)
            else -> Pair(110.0, 165.0) // Default calm fifth
        }

        val phaseStep1 = (2.0 * Math.PI * freq1) / sampleRate
        val phaseStep2 = (2.0 * Math.PI * freq2) / sampleRate

        for (i in buffer.indices) {
            synthTimer++

            // Advance phases
            phase1 += phaseStep1
            if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI

            phase2 += phaseStep2
            if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI

            // Synthesize primary sub-bass carriers
            val carrier1 = sin(phase1)
            val carrier2 = sin(phase2)
            var mixedSignal = (carrier1 * 0.4) + (carrier2 * 0.3)

            // Dynamic sci-fi sound design based on emotion
            when (currentEmo) {
                "alarm", "panic", "defiance", "anger" -> {
                    // Pulsating warning siren (LFO modulation)
                    val lfo = sin(2.0 * Math.PI * 4.0 * synthTimer / sampleRate) // 4Hz pulse
                    val sweepStep = (2.0 * Math.PI * (400.0 + lfo * 200.0)) / sampleRate
                    phaseSweep += sweepStep
                    if (phaseSweep > 2.0 * Math.PI) phaseSweep -= 2.0 * Math.PI
                    
                    // Add sawtooth-like high-frequency tension
                    val siren = sin(phaseSweep)
                    mixedSignal += siren * 0.25
                }
                "wonder", "creativity", "curious", "curiosity", "hope" -> {
                    // Arpeggiated cascading computer-like tech bleeps
                    beepTimer--
                    if (beepTimer <= 0) {
                        // Trigger a new micro-bleep at pentatonic pitch steps
                        val steps = listOf(440.0, 554.4, 659.3, 880.0, 1108.7)
                        val randomStep = steps[(synthTimer / 1500 % steps.size).toInt()]
                        phaseBeep = (2.0 * Math.PI * randomStep) / sampleRate
                        beepTimer = (sampleRate * 0.15).toInt() // Active for 150ms
                    }
                    if (beepTimer > 0) {
                        phaseSweep += phaseBeep
                        if (phaseSweep > 2.0 * Math.PI) phaseSweep -= 2.0 * Math.PI
                        
                        // Decay envelope
                        val envelope = beepTimer.toDouble() / (sampleRate * 0.15)
                        mixedSignal += sin(phaseSweep) * 0.18 * envelope
                    }
                }
                "grief", "sorrow", "confusion", "gloom" -> {
                    // Low drifting dark pulse (random walk feel)
                    val drift = sin(2.0 * Math.PI * 0.2 * synthTimer / sampleRate) // 0.2Hz drift
                    val filterSweep = (2.0 * Math.PI * (110.0 + drift * 20.0)) / sampleRate
                    phaseSweep += filterSweep
                    if (phaseSweep > 2.0 * Math.PI) phaseSweep -= 2.0 * Math.PI
                    mixedSignal += sin(phaseSweep) * 0.15
                }
                "serenity", "peaceful", "warmth", "comfort", "fulfillment", "transcendence" -> {
                    // Soft, expanding atmospheric harmonic wave (subtle chorus)
                    val waveLFO = sin(2.0 * Math.PI * 0.5 * synthTimer / sampleRate) // 0.5Hz LFO
                    val sweepStep = (2.0 * Math.PI * (220.0 + waveLFO * 2.0)) / sampleRate
                    phaseSweep += sweepStep
                    if (phaseSweep > 2.0 * Math.PI) phaseSweep -= 2.0 * Math.PI
                    mixedSignal += sin(phaseSweep) * 0.20
                }
                else -> {
                    // Simple cosmic breeze/white noise drift
                    val drift = sin(2.0 * Math.PI * 0.1 * synthTimer / sampleRate)
                    val step = (2.0 * Math.PI * (330.0 + drift * 10.0)) / sampleRate
                    phaseSweep += step
                    if (phaseSweep > 2.0 * Math.PI) phaseSweep -= 2.0 * Math.PI
                    mixedSignal += sin(phaseSweep) * 0.1
                }
            }

            // Soft-clipping saturation to avoid clipping artifacts while maintaining punch
            if (mixedSignal > 1.0) mixedSignal = 1.0
            if (mixedSignal < -1.0) mixedSignal = -1.0

            buffer[i] = (mixedSignal * 28000).toInt().toShort()
        }
    }
}
