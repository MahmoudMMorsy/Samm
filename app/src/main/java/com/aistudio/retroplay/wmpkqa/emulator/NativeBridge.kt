package com.aistudio.retroplay.wmpkqa.emulator

import android.graphics.Bitmap
import android.util.Log

class NativeBridge(private val callback: EmulatorCallback) {

    interface EmulatorCallback {
        fun onVideoFrame(bitmap: Bitmap)
        fun onAudioSamples(samples: ShortArray)
        fun getInputState(buttonId: Int): Short
    }

    private var videoBitmap: Bitmap? = null

    init {
        try {
            System.loadLibrary("mgba")
            System.loadLibrary("retroplay")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("NativeBridge", "Failed to load native libraries: ${e.message}")
        }
    }

    external fun loadCore(corePath: String): Boolean
    external fun loadGame(gamePath: String): Boolean
    external fun runFrame()
    external fun reset()
    external fun stop()

    // JNI Callbacks
    fun onNativeVideoFrame(pixelData: IntArray, width: Int, height: Int) {
        if (videoBitmap == null || videoBitmap?.width != width || videoBitmap?.height != height) {
            videoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        videoBitmap?.setPixels(pixelData, 0, width, 0, 0, width, height)
        videoBitmap?.let { callback.onVideoFrame(it) }
    }

    fun onNativeAudioSamples(samples: ShortArray) {
        callback.onAudioSamples(samples)
    }

    fun getNativeInputState(buttonId: Int): Short {
        return callback.getInputState(buttonId)
    }
}
