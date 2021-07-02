package com.raine.gl.audiotracksample

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.concurrent.Executors

/**
 * 尝试录制裸的音频pcm，
 * 44100 16 双声道,并且将音频数据放在固定的位置; 这是一个尝试
 *
 * 1.尝试一；先尝试采集一次
 */
class AudioRecorder {
    private lateinit var aimFile: File
    private lateinit var audioRecorder: AudioRecord
    private val readDataThread = Executors.newSingleThreadExecutor()
    private var callback: Callback? = null

    fun init(context: Context, callback: Callback? = null) {
        val file = File(context.filesDir, savedPath)
        if (file.exists()) {
            file.delete()
        }
        aimFile = file
        this.callback = callback
    }

    fun startRecord() {
        val audioBufferSizeInBytes =
            AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val byteBuffer = ByteArray(audioBufferSizeInBytes)
        val shortBuffer = ByteBuffer.wrap(byteBuffer)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        audioRecorder = AudioRecord(
            AUDIO_RESOURCE,
            AUDIO_SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            audioBufferSizeInBytes
        )
        val state = audioRecorder.state
        if (state == AudioRecord.STATE_UNINITIALIZED) {
            throw RuntimeException("AudioRecord STATE_UNINITIALIZED")
        }
        audioRecorder.startRecording()
        /**
         * 读取采集的数据
         */
        val task = Runnable {
            val dataOutputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(aimFile)))
            while (audioRecorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val readedByteCount = audioRecorder.read(byteBuffer, 0, audioBufferSizeInBytes)
                if (AudioRecord.ERROR_INVALID_OPERATION != readedByteCount && AudioRecord.ERROR_BAD_VALUE != readedByteCount) {
                    shortBuffer.limit(readedByteCount/2)
                    callback?.onAudioRecorded(shortBuffer)
                    dataOutputStream.write(byteBuffer, 0, readedByteCount)
                }
            }
            dataOutputStream.close()
        }
        readDataThread.execute(task)
    }

    fun stopRecord() {
        if (audioRecorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecorder.stop()
        }
        if (audioRecorder.state == AudioRecord.STATE_INITIALIZED) {
            audioRecorder.release()
        }
    }


    interface Callback {
       fun onAudioRecorded(dataView: ShortBuffer)
    }

    companion object {
        private const val savedPath = "sample.pcm"
        private const val AUDIO_RESOURCE = MediaRecorder.AudioSource.MIC
        private const val AUDIO_SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    }
}