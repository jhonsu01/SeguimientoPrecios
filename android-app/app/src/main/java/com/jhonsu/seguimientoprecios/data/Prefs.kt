package com.jhonsu.seguimientoprecios.data

import android.content.Context

/** Ajustes locales: hash del PIN y API key de OpenAI (nunca salen del dispositivo). */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    var pinHash: String?
        get() = sp.getString("pin_hash", null)
        set(value) {
            sp.edit().apply {
                if (value == null) remove("pin_hash") else putString("pin_hash", value)
            }.apply()
        }

    var openAiKey: String
        get() = sp.getString("openai_key", "") ?: ""
        set(value) { sp.edit().putString("openai_key", value).apply() }

    var moneda: String
        get() = sp.getString("moneda", "COP") ?: "COP"
        set(value) { sp.edit().putString("moneda", value).apply() }

    val tienePin: Boolean get() = !pinHash.isNullOrBlank()
}
