// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import androidx.compose.runtime.Composable
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.preferences.SliderPreference

@Composable
fun PersonalSettingsScreen(
    onClickBack: () -> Unit,
) {
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = "Ajustes personales",
        settings = listOf(Defaults.KEY_VOICE_SILENCE_TIMEOUT)
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
    }
)
