package com.laksh.app

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Records a short voice note into private app storage. */
class VoiceRecorder(private val ctx: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    fun start(): Boolean {
        val f = File(ctx.filesDir, "voice_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(ctx)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            file = f
            true
        } catch (e: Exception) {
            runCatching { r.release() }
            false
        }
    }

    /** Stops recording and returns the saved file path, or null if it failed. */
    fun stop(): String? {
        val r = recorder ?: return null
        recorder = null
        return try {
            r.stop()
            r.release()
            file?.absolutePath
        } catch (e: Exception) {
            runCatching { r.release() }
            file?.delete()
            null
        }
    }

    fun cancel() {
        recorder?.let { runCatching { it.stop() }; runCatching { it.release() } }
        recorder = null
        file?.delete()
    }
}
