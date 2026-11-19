package fr.tp.contacts

import android.content.Context
import android.graphics.Color
import androidx.preference.PreferenceManager

data class ContactDisplayPreferences(val textSizeSp: Float, val textColor: Int, val callFromList: Boolean)

object PreferencesManager {
    fun read(context: Context): ContactDisplayPreferences {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val size = prefs.getString(KEY_SIZE, "16")?.toFloatOrNull() ?: 16f
        val colorText = prefs.getString(KEY_COLOR, "#000000") ?: "#000000"
        val color = runCatching { Color.parseColor(colorText) }.getOrDefault(Color.BLACK)
        return ContactDisplayPreferences(size, color, prefs.getBoolean(KEY_CALL, false))
    }

    const val KEY_SIZE = "taille_police"
    const val KEY_COLOR = "couleur_preferee"
    const val KEY_CALL = "appel_depuis_liste"
}
