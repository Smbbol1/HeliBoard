// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import androidx.compose.runtime.Composable
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.preferences.SliderPreference
import helium314.keyboard.settings.preferences.SwitchPreference

@Composable
fun PersonalSettingsScreen(
    onClickBack: () -> Unit,
) {
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = "Ajustes personales",
                        settings = listOf(
            Defaults.KEY_VOICE_SILENCE_TIMEOUT,
            Defaults.KEY_MOVE_PINNED_KEYS,
            Defaults.KEY_FLOATING_EXIT_BUTTON,
            Defaults.KEY_FLOATING_PERSISTENT_POS
        )
    )
}



fun createPersonalSettings(context: Context) = listOf(
    Setting(context, Defaults.KEY_VOICE_SILENCE_TIMEOUT, R.string.ime_settings) { setting ->
        SliderPreference(
            name = "Tiempo de apagado automático del micrófono",
            key = setting.key,
            default = Defaults.PREF_VOICE_SILENCE_TIMEOUT,
            description = { seconds ->
                when {
                    seconds <= 0 -> "Desactivado (no se apaga por silencio)"
                    seconds < 60 -> "$seconds segundos"
                    seconds % 60 == 0 -> "${seconds / 60} min"
                    else -> "${seconds / 60} min ${seconds % 60} s"
                }
            },
            range = 0f..300f,
            stepSize = 5
        )
    },
        Setting(context, Defaults.KEY_MOVE_PINNED_KEYS, R.string.ime_settings) { setting ->
        SwitchPreference(
            name = "Mover teclas ancladas",
            key = setting.key,
            default = Defaults.PREF_MOVE_PINNED_KEYS,
            description = "Mantén presionado y arrastra un icono en la barra para intercambiar su posición"
        )
    },
        Setting(context, Defaults.KEY_FLOATING_EXIT_BUTTON, R.string.ime_settings) { setting ->
        SwitchPreference(
            name = "Botón salir de ventana flotante",
            key = setting.key,
            default = Defaults.PREF_FLOATING_EXIT_BUTTON,
            description = "Muestra un icono en la esquina inferior derecha del teclado flotante para volver al modo normal"
        )
    },
    Setting(context, Defaults.KEY_FLOATING_PERSISTENT_POS, R.string.ime_settings) { setting ->
        SwitchPreference(
            name = "Posición permanente del teclado flotante",
            key = setting.key,
            default = Defaults.PREF_FLOATING_PERSISTENT_POS,
            description = "Mantiene fija la ubicación donde colocaste el teclado flotante al abrirlo y cerrarlo hasta moverlo manualmente"
        )
    }
)


