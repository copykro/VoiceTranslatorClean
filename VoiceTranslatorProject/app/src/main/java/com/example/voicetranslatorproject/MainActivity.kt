package com.example.voicetranslatorproject

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.*
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var txtResult: TextView
    private lateinit var txtLang: TextView
    private lateinit var progressBar: ProgressBar

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFile: File

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .readTimeout(2, TimeUnit.MINUTES)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        txtResult = findViewById(R.id.txtResult)
        txtLang = findViewById(R.id.txtLang)
        progressBar = findViewById(R.id.progressBar)

        progressBar.visibility = View.GONE
        btnStop.isEnabled = false

        btnStart.setOnClickListener {
            if (checkPermissions()) {
                startRecording()
            } else {
                requestPermissions()
            }
        }

        btnStop.setOnClickListener {
            stopRecording()
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            200
        )
    }

    private fun startRecording() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        audioFile = File(dir, "audio.mp4") // mp4 for MediaRecorder

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }

        txtResult.text = "🎧 Listening..."  // ✅ Show listening message
        txtLang.text = ""
        progressBar.visibility = View.VISIBLE
        btnStart.isEnabled = false
        btnStop.isEnabled = true

        Toast.makeText(this, "🎙️ Recording started...", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        Toast.makeText(this, "🛑 Recording stopped", Toast.LENGTH_SHORT).show()
        progressBar.visibility = View.VISIBLE
        btnStop.isEnabled = false
        btnStart.isEnabled = true

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaRecorder = null

        Handler(Looper.getMainLooper()).postDelayed({
            sendAudioToServer(audioFile)
        }, 1000)
    }

    private fun sendAudioToServer(audioFile: File) {
        runOnUiThread {
            progressBar.visibility = View.VISIBLE

            // ⏱️ Fallback timeout after 60 seconds
            progressBar.postDelayed({
                if (progressBar.visibility == View.VISIBLE) {
                    progressBar.visibility = View.GONE
                    txtResult.text = "⚠️ Timed out! Please try again."
                }
            }, 60000)
        }

        val mediaType = "audio/mp4".toMediaTypeOrNull()
        val fileBody = audioFile.asRequestBody(mediaType)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name, fileBody)
            .build()

        val request = Request.Builder()
            .url("http://192.168.75.250:5001/transcribe") // ✅ Use your IP
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    txtResult.text = "❌ Request failed: ${e.message}"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { progressBar.visibility = View.GONE }

                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val original = json.optString("original")
                    val lang = json.optString("language")
                    val translated = json.optString("translated")

                    runOnUiThread {
                        txtLang.text = "Language: $lang"
                        txtResult.text = "Original: $original\nTranslated: $translated"
                    }
                } else {
                    runOnUiThread {
                        txtResult.text = "⚠️ Server error: ${response.code}"
                    }
                }
            }
        })
    }
}
