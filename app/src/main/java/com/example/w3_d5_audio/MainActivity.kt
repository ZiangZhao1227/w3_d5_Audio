package com.example.w3_d5_audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.io.*

class MainActivity : AppCompatActivity() {
    private var recRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val record = btn_record
        record.setOnClickListener {
            if (recRunning) {
                record.background.setTint(Color.GREEN)
                CoroutineScope(Dispatchers.IO).async {
                    stopRecording()
                    while (getRecording() == null) {
                    }
                    playback(getRecording()!!)
                }
            } else {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
                } else {
                    record.background.setTint(Color.RED)
                    CoroutineScope(Dispatchers.IO).async {
                        startRecording()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {

        recRunning = true

        val recordedFile: File

        val recordedFileName = "testRecording.raw"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        try {
            recordedFile = File(storageDir.toString() + "/" + recordedFileName)

            try {
                val outputStream = FileOutputStream(recordedFile)
                val bufferedOutputStream = BufferedOutputStream(outputStream)
                val dataOutputStream = DataOutputStream(bufferedOutputStream)

                val minBufferSize = AudioRecord.getMinBufferSize(
                    44100,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val aFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()

                val recorder = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(aFormat)
                    .setBufferSizeInBytes(minBufferSize)
                    .build()
                val audioData = ByteArray(minBufferSize)
                recorder.startRecording()

                while (recRunning && recorder.read(
                        audioData,
                        0,
                        minBufferSize
                    ) > 0
                ) dataOutputStream.write(audioData)

                recorder.stop()
                dataOutputStream.close()
            } catch (e: IOException) {
                Log.e("re", "Recording error $e")
            }
        } catch (ex: IOException) {
            Log.e("ce", "Can't create audio file $ex")
        }
    }

    private fun getRecording(): InputStream? {
        val recFileName = "testRecording.raw"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val playFile: File

        return try {
            playFile = File(storageDir.toString() + "/" + recFileName)
            playFile.inputStream()
        } catch (ex: IOException) {
            Log.e("pe", "Can't play audio file $ex")
            null
        }
    }

    private suspend fun stopRecording() {
        delay(1000)
        recRunning = false
    }

    private fun playback(isTream: InputStream) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            44100, AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val aBuilder = AudioTrack.Builder()
        val aAttr: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val aFormat: AudioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44100)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val track = aBuilder.setAudioAttributes(aAttr)
            .setAudioFormat(aFormat)
            .setBufferSizeInBytes(minBufferSize)
            .build()
        track.setVolume(0.4f)

        track.play()
        var i: Int
        val buffer = ByteArray(minBufferSize)
        try {
            i = isTream.read(buffer, 0, minBufferSize)
            while (i != -1) {
                track.write(buffer, 0, i)
                i = isTream.read(buffer, 0, minBufferSize)
            }
        } catch (e: IOException) {
            Log.e("se", "Stream read error $e")
        }
        try {
            isTream.close()
        } catch (e: IOException) {
            Log.e("ce", "Closing error $e")
        }
        track.stop()
        track.release()
    }

}