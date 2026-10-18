package de.restdeck.app.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import de.restdeck.app.domain.AppSettings
import de.restdeck.app.domain.SettingsJson
import de.restdeck.app.domain.StyleSpec
import de.restdeck.app.domain.Styles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/** R11–R13: Stil + Foto-Hintergrund, gespeichert in settings.json bzw. background.jpg. */
class Appearance(private val dir: File, private val scope: CoroutineScope) {
    private val settingsFile = File(dir, "settings.json")
    private val photoFile = File(dir, "background.jpg")
    private val io = Dispatchers.IO.limitedParallelism(1)

    private val _settings = MutableStateFlow(
        runCatching { SettingsJson.decode(settingsFile.readText()) }.getOrDefault(AppSettings())
            .let { if (it.hasPhoto && !photoFile.exists()) it.copy(hasPhoto = false) else it },
    )
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val style: StyleSpec get() = Styles.byId(_settings.value.styleId)

    private val _photo = MutableStateFlow<Bitmap?>(null)
    /** Dekodiertes Foto (null = keins oder noch nicht geladen). */
    val photo: StateFlow<Bitmap?> = _photo.asStateFlow()

    init {
        if (_settings.value.hasPhoto) scope.launch(io) { _photo.value = BitmapFactory.decodeFile(photoFile.path) }
    }

    fun update(f: (AppSettings) -> AppSettings) {
        _settings.update { f(it).normalized() }
        scope.launch(io) {
            val tmp = File(dir, "settings.json.tmp")
            tmp.writeText(SettingsJson.encode(_settings.value))
            tmp.renameTo(settingsFile)
        }
    }

    /** Kopiert das gewählte Foto verkleinert in den App-Speicher. Wirft bei unlesbaren Bildern. */
    suspend fun importPhoto(resolver: ContentResolver, uri: Uri) {
        val bmp = withContext(Dispatchers.IO) {
            val b = decodeScaled(resolver, uri, MAX_EDGE)
            val tmp = File(dir, "background.jpg.tmp")
            tmp.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            tmp.renameTo(photoFile)
            b
        }
        _photo.value = bmp
        update { it.copy(hasPhoto = true, photoVersion = it.photoVersion + 1) }
    }

    fun removePhoto() {
        _photo.value = null
        update { it.copy(hasPhoto = false) }
        scope.launch(io) { photoFile.delete() }
    }

    private fun decodeScaled(resolver: ContentResolver, uri: Uri, maxEdge: Int): Bitmap =
        if (Build.VERSION.SDK_INT >= 28) {
            // ImageDecoder beachtet die EXIF-Drehung
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { dec, info, _ ->
                val longest = max(info.size.width, info.size.height)
                if (longest > maxEdge) {
                    val f = maxEdge.toFloat() / longest
                    dec.setTargetSize((info.size.width * f).toInt(), (info.size.height * f).toInt())
                }
                dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            // Android 8/8.1: ohne EXIF-Drehung (bewusst, keine Zusatz-Bibliothek)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
            resolver.openInputStream(uri)!!.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: error("Bild nicht lesbar")
        }

    companion object {
        /** Längste Kante; reicht für Full-HD-Displays, hält Speicher klein. */
        const val MAX_EDGE = 2000
    }
}
