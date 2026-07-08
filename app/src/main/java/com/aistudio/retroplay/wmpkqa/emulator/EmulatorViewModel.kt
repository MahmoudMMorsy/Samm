package com.aistudio.retroplay.wmpkqa.emulator

import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EmulatorViewModel : ViewModel(), NativeBridge.EmulatorCallback {

    private var bridge: NativeBridge? = null
    private var audioTrack: AudioTrack? = null
    var gameBitmap by mutableStateOf<Bitmap?>(null)
    private var emulatorJob: Job? = null

    var isFastForward by mutableStateOf(false)
    var isPaused by mutableStateOf(false)

    private val inputState = mutableMapOf<Int, Short>()

    fun startEmulator(gamePath: String) {
        bridge = NativeBridge(this)

        audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build())
                .setBufferSizeInBytes(8192)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                8192,
                AudioTrack.MODE_STREAM
            )
        }
        audioTrack?.play()

        bridge?.loadCore("") // Path not needed for static link
        bridge?.loadGame(gamePath)

        emulatorJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (!isPaused) {
                    bridge?.runFrame()
                    if (isFastForward) {
                        bridge?.runFrame() // 2x Speed
                    }
                }
                // Target ~60fps (16ms)
                delay(if (isFastForward) 8 else 16)
            }
        }
    }

    fun toggleFastForward() {
        isFastForward = !isFastForward
    }

    fun togglePause() {
        isPaused = !isPaused
    }

    fun onButtonPressed(buttonId: Int) {
        inputState[buttonId] = 1
    }

    fun onButtonReleased(buttonId: Int) {
        inputState[buttonId] = 0
    }

    override fun onVideoFrame(bitmap: Bitmap) {
        gameBitmap = bitmap
    }

    override fun onAudioSamples(samples: ShortArray) {
        audioTrack?.write(samples, 0, samples.size)
    }

    override fun getInputState(buttonId: Int): Short {
        return inputState[buttonId] ?: 0
    }

    override fun onCleared() {
        super.onCleared()
        emulatorJob?.cancel()
        bridge?.stop()
        audioTrack?.stop()
        audioTrack?.release()
    }
}
