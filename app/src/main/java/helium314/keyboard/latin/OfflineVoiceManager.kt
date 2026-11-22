package helium314.keyboard.latin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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

    fun toggleListening() {
        if (ContextCompat.checkSelfPermission(ime, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(ime, "Activa el permiso de Micrófono en Ajustes de la app", Toast.LENGTH_LONG).show()
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
                Toast.makeText(ime, "Preparando modelo de voz offline, espera un segundo...", Toast.LENGTH_SHORT).show()
                return
            }
            isUnpacking = true
            Toast.makeText(ime, "Cargando voz offline por primera vez...", Toast.LENGTH_SHORT).show()
            StorageService.unpack(ime, "model-es", "model",
                { loadedModel ->
                    model = loadedModel
                    isUnpacking = false
                    startListening()
                },
                { exception ->
                    isUnpacking = false
                    Toast.makeText(ime, "Error al cargar modelo: ${exception.message}", Toast.LENGTH_LONG).show()
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
            Toast.makeText(ime, "Escuchando (Offline)...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(ime, "Error iniciando micrófono: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        isListening = false
    }

    fun destroy() {
        stopListening()
        model?.close()
        model = null
    }

    override fun onPartialResult(hypothesis: String?) {
        // Opcional: aquí se reciben resultados parciales mientras hablas
    }

    override fun onResult(hypothesis: String?) {
        val text = parseText(hypothesis)
        if (text.isNotEmpty()) {
            ime.onTextInput("$text ")
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        val text = parseText(hypothesis)
        if (text.isNotEmpty()) {
            ime.onTextInput("$text ")
        }
        stopListening()
    }

    override fun onError(exception: Exception?) {
        stopListening()
        Toast.makeText(ime, "Error de voz: ${exception?.message}", Toast.LENGTH_SHORT).show()
    }

    override fun onTimeout() {
        stopListening()
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
