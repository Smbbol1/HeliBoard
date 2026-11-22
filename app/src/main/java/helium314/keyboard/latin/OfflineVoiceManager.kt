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
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

class OfflineVoiceManager(private val ime: LatinIME) : RecognitionListener {

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var isListening = false
    private var isUnpacking = false
    private val mainHandler = Handler(Looper.getMainLooper())

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
            showToast("Cargando voz offline por primera vez...")
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
        } else {
            startListening()
        }
    }

        private fun startListening() {
        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
            isListening = true
            ime.updateVoiceIndicator(true)
        } catch (e: Exception) {
            isListening = false
            ime.updateVoiceIndicator(false)
            showToast("Error iniciando micrófono: ${e.message}")
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        isListening = false
        ime.updateVoiceIndicator(false)
    }


    fun destroy() {
        stopListening()
        model?.close()
        model = null
    }

    override fun onPartialResult(hypothesis: String?) {
        // Resultados parciales mientras hablas
    }

    override fun onResult(hypothesis: String?) {
        val text = parseText(hypothesis)
        if (text.isNotEmpty()) {
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
