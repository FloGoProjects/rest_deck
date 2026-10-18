package de.restdeck.app.domain

import kotlinx.serialization.Serializable

/** R11–R13: Aussehen der App. */
@Serializable
data class AppSettings(
    val styleId: String = "modern",
    val hasPhoto: Boolean = false,
    /** Wird bei jedem neuen Foto hochgezählt, damit die UI neu lädt. */
    val photoVersion: Long = 0,
    /** Abdunkeln des Fotos, 0 … MAX_DIM. */
    val dim: Float = 0.6f,
    /** Weichzeichnen in dp, 0 … MAX_BLUR (nur ab Android 12 wirksam). */
    val blur: Float = 0f,
) {
    fun normalized(): AppSettings = copy(dim = dim.coerceIn(0f, MAX_DIM), blur = blur.coerceIn(0f, MAX_BLUR))

    companion object {
        const val MAX_DIM = 0.85f
        const val MAX_BLUR = 24f
    }
}

object SettingsJson {
    fun encode(s: AppSettings): String = JsonFormat.encodeToString(AppSettings.serializer(), s)

    fun decode(text: String): AppSettings =
        runCatching { JsonFormat.decodeFromString(AppSettings.serializer(), text) }.getOrDefault(AppSettings()).normalized()
}
