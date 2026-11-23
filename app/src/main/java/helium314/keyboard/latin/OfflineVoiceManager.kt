package helium314.keyboard.latin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.utils.prefs
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File


class OfflineVoiceManager(private val ime: LatinIME) : RecognitionListener {

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var isListening = false
    private var isUnpacking = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val silenceTimeoutRunnable = Runnable {
        if (isListening) {
            stopListening()
        }
    }

        private fun getSilenceTimeoutMs(): Long {
        val seconds = ime.prefs().getInt(Defaults.KEY_VOICE_SILENCE_TIMEOUT, Defaults.PREF_VOICE_SILENCE_TIMEOUT)
        return seconds * 1000L
    }

    private fun isOptimizationEnabled(): Boolean {
        return ime.prefs().getBoolean(Defaults.KEY_VOICE_OPTIMIZATION, Defaults.PREF_VOICE_OPTIMIZATION)
    }

    private fun resetSilenceTimer() {

        mainHandler.removeCallbacks(silenceTimeoutRunnable)
        val timeoutMs = getSilenceTimeoutMs()
        if (timeoutMs > 0L) {
            mainHandler.postDelayed(silenceTimeoutRunnable, timeoutMs)
        }
    }

    fun toggleListening() {
        if (ContextCompat.checkSelfPermission(ime, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            showToast("Activa el permiso de Micrófono en Ajustes de HeliBoard")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ime.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ime.startActivity(intent)
            return
        }

        if (isListening) {
            stopListening()
            return
        }

                if (model == null) {
            if (isUnpacking) {
                showToast("Preparando modelo de voz offline, espera...")
                return
            }
            isUnpacking = true
            val cachedModelDir = File(ime.getExternalFilesDir(null), "model")
            val uuidFile = File(cachedModelDir, "uuid")
            if (isOptimizationEnabled() && cachedModelDir.isDirectory && uuidFile.exists()) {
                Thread {
                    try {
                        val loadedModel = Model(cachedModelDir.absolutePath)
                        mainHandler.post {
                            model = loadedModel
                            isUnpacking = false
                            startListening()
                        }
                    } catch (_: Exception) {
                        mainHandler.post {
                            unpackAndLoadModel()
                        }
                    }
                }.start()
            } else {
                unpackAndLoadModel()
            }
        } else {
            startListening()
        }
    }

    private fun unpackAndLoadModel() {
        StorageService.unpack(ime, "model-es", "model",
            { loadedModel ->
                model = loadedModel
                isUnpacking = false
                startListening()
            },
            { exception ->
                isUnpacking = false
                showToast("Error al cargar modelo: ${exception.message}")
            }
        )
    }

    }

    private fun startListening() {
        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
            isListening = true
            ime.updateVoiceIndicator(true)
            resetSilenceTimer()
        } catch (e: Exception) {
            isListening = false
            ime.updateVoiceIndicator(false)
            showToast("Error iniciando micrófono: ${e.message}")
        }
    }

    fun stopListening() {
        mainHandler.removeCallbacks(silenceTimeoutRunnable)
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        isListening = false
        ime.updateVoiceIndicator(false)
    }

    fun releaseModel() {
        if (!isListening && model != null) {
            model?.close()
            model = null
        }
    }

    fun destroy() {
        stopListening()
        releaseModel()
    }

    override fun onPartialResult(hypothesis: String?) {
        if (!hypothesis.isNullOrEmpty() && hypothesis.contains("\"partial\" : \"") && !hypothesis.contains("\"partial\" : \"\"")) {
            resetSilenceTimer()
        }
    }

    override fun onResult(hypothesis: String?) {
        val text = parseText(hypothesis)
        if (text.isNotEmpty()) {
            resetSilenceTimer()
            mainHandler.post { ime.onTextInput("$text ") }
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        val text = parseText(hypothesis)
        if (text.isNotEmpty()) {
            mainHandler.post { ime.onTextInput("$text ") }
        }
        stopListening()
    }

    override fun onError(exception: Exception?) {
        stopListening()
        showToast("Error de voz: ${exception?.message}")
    }

    override fun onTimeout() {
        stopListening()
    }

    private fun showToast(msg: String) {
        mainHandler.post {
            Toast.makeText(ime, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseText(jsonStr: String?): String {
        if (jsonStr.isNullOrEmpty()) return ""
        return try {
            JSONObject(jsonStr).optString("text", "").trim()
        } catch (e: Exception) {
            ""
        }
    }
}
