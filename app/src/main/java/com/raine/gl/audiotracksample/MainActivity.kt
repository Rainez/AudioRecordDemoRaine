package com.raine.gl.audiotracksample

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.widget.ImageView
import java.nio.ShortBuffer
import java.security.SecureRandom

class MainActivity : AppCompatActivity() {
    private val random: SecureRandom = SecureRandom()
    private val data = ShortArray(44100)
    private val backHandlerThread = HandlerThread("Generate-Random")
    private lateinit var backHandler: Handler
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var mToggleRecordButton: ImageView
    private var isRecording: Boolean = false
    private var evaporableStart = false
    private var evaporableStop = false
    private lateinit var audioRecorder: AudioRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sampleRateView = findViewById<SampleRateView>(R.id.sample_rate_view)
        audioRecorder = AudioRecorder().apply { init(this@MainActivity, object: AudioRecorder.Callback {
            override fun onAudioRecorded(dataView: ShortBuffer) {
               sampleRateView.post {
                   sampleRateView.setData(dataView)
               }
            }
        }) }
        backHandlerThread.start()
        backHandler = Handler(backHandlerThread.looper)
        mToggleRecordButton = findViewById(R.id.start_record)
        mToggleRecordButton.setOnClickListener {
            isRecording = !isRecording

            if (isRecording) {
                if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO,) == PackageManager.PERMISSION_GRANTED) {
                    if (!evaporableStart && !evaporableStop) {
                        audioRecorder.startRecord()
                        evaporableStart = true
                    }
                    mToggleRecordButton.setImageResource(R.drawable.ic_baseline_pause_24)
                } else {
                    requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO),1)
                }
            } else {
                if (!evaporableStop && evaporableStart) {
                    audioRecorder.stopRecord()
                    evaporableStop = true
                }
                mToggleRecordButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
       if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
           if (!evaporableStart && !evaporableStop) {
               audioRecorder.startRecord()
               evaporableStart = true
           }
       }
    }

    override fun onDestroy() {
        super.onDestroy()
        backHandler.removeMessages(-1)
        backHandlerThread.quitSafely()
        if (!evaporableStop && evaporableStart) {
            audioRecorder.stopRecord()
            evaporableStop = true
        }
    }
}