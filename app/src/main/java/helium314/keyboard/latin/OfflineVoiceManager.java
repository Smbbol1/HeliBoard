package helium314.keyboard.latin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;

public class OfflineVoiceManager implements RecognitionListener {

    private final LatinIME mIme;
    private Model mModel;
    private SpeechService mSpeechService;
    private boolean mIsListening = false;
    private boolean mIsInitializing = false;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public OfflineVoiceManager(LatinIME ime) {
        this.mIme = ime;
    }

    public void toggleListening() {
        if (mIsListening) {
            stopListening();
            return;
        }

        // 1. Verificar permiso de micrófono
        if (ContextCompat.checkSelfPermission(mIme, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(mIme, VoicePermissionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mIme.startActivity(intent);
            return;
        }

        // 2. Si el modelo ya está cargado, empezar a escuchar
        if (mModel != null) {
            startRecognition();
            return;
        }

        // 3. Extraer y cargar el modelo desde assets/model-es
        if (mIsInitializing) {
            showToast("Cargando modelo de voz...");
            return;
        }

        mIsInitializing = true;
        showToast("Preparando voz offline (primera vez)...");

        StorageService.unpack(mIme, "model-es", "model-es",
                (model) -> {
                    mModel = model;
                    mIsInitializing = false;
                    startRecognition();
                },
                (exception) -> {
                    mIsInitializing = false;
                    showToast("Error cargando modelo Vosk: " + exception.getMessage());
                });
    }

    private void startRecognition() {
        try {
            if (mSpeechService != null) {
                mSpeechService.stop();
                mSpeechService.shutdown();
                mSpeechService = null;
            }
            Recognizer rec = new Recognizer(mModel, 16000.0f);
            mSpeechService = new SpeechService(rec, 16000.0f);
            mSpeechService.startListening(this);
            mIsListening = true;
            showToast("Escuchando...");
        } catch (IOException e) {
            mIsListening = false;
            showToast("Error de micrófono: " + e.getMessage());
        }
    }

    public void stopListening() {
        if (mSpeechService != null) {
            mSpeechService.stop();
            mSpeechService.shutdown();
            mSpeechService = null;
        }
        if (mIsListening) {
            mIsListening = false;
            showToast("Dictado detenido");
        }
    }

    public void destroy() {
        stopListening();
        if (mModel != null) {
            mModel.close();
            mModel = null;
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        // Opcional: aquí podrías mostrar texto parcial mientras hablas
    }

    @Override
    public void onResult(String hypothesis) {
        commitVoskJson(hypothesis);
    }

    @Override
    public void onFinalResult(String hypothesis) {
        commitVoskJson(hypothesis);
        stopListening();
    }

    private void commitVoskJson(String jsonStr) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            String text = obj.optString("text", "").trim();
            if (!text.isEmpty()) {
                mMainHandler.post(() -> mIme.onTextInput(text + " "));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onError(Exception exception) {
        mIsListening = false;
        showToast("Error de reconocimiento: " + exception.getMessage());
    }

    @Override
    public void onTimeout() {
        stopListening();
    }

    private void showToast(String msg) {
        mMainHandler.post(() -> Toast.makeText(mIme, msg, Toast.LENGTH_SHORT).show());
    }
}
