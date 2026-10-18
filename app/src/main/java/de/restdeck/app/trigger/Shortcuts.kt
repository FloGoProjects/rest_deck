package de.restdeck.app.trigger

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Icon
import de.restdeck.app.domain.ActionButton
import de.restdeck.app.domain.FontKind
import de.restdeck.app.domain.StyleSpec
import de.restdeck.app.ui.Palette

/** R8: Buttons als angeheftete Homescreen-Shortcuts. */
object Shortcuts {
    const val EXTRA_BUTTON = "buttonId"

    private fun manager(ctx: Context) = ctx.getSystemService(ShortcutManager::class.java)

    fun canPin(ctx: Context): Boolean = manager(ctx)?.isRequestPinShortcutSupported == true

    private fun info(ctx: Context, b: ActionButton, style: StyleSpec): ShortcutInfo {
        val intent = Intent(ctx, TriggerActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(EXTRA_BUTTON, b.id)
        return ShortcutInfo.Builder(ctx, b.id)
            .setShortLabel(b.name.ifBlank { "Button" }.take(25))
            .setIcon(Icon.createWithAdaptiveBitmap(icon(b, style)))
            .setIntent(intent)
            .build()
    }

    fun pin(ctx: Context, b: ActionButton, style: StyleSpec): Boolean =
        manager(ctx)?.requestPinShortcut(info(ctx, b, style), null) == true

    /** Name/Farbe/Stil geändert -> bereits angeheftete Shortcuts nachziehen. */
    fun refresh(ctx: Context, buttons: List<ActionButton>, style: StyleSpec) {
        val m = manager(ctx) ?: return
        val pinned = m.pinnedShortcuts.map { it.id }.toSet()
        val alive = buttons.filter { it.id in pinned }
        if (alive.isNotEmpty()) runCatching { m.updateShortcuts(alive.map { info(ctx, it, style) }) }
        val gone = pinned - buttons.map { it.id }.toSet()
        if (gone.isNotEmpty()) m.disableShortcuts(gone.toList(), "Button wurde gelöscht")
    }

    /** Adaptive Icon (108dp-Raster): Verlauf vollflächig, Initiale in der sicheren Mitte. */
    private fun icon(b: ActionButton, style: StyleSpec): Bitmap {
        val size = 216
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val (from, to) = Palette.gradient(style, b.color)
        val on = Palette.swatch(style, b.color).on.toInt()
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), from.toArgbInt(), to.toArgbInt(), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bg)
        val letter = b.name.trim().firstOrNull()?.uppercase() ?: "⚡"
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = on
            textSize = size * 0.34f
            typeface = Typeface.create(if (style.font == FontKind.SERIF) Typeface.SERIF else if (style.font == FontKind.MONO) Typeface.MONOSPACE else Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val y = size / 2f - (text.descent() + text.ascent()) / 2f
        c.drawText(letter, size / 2f, y, text)
        return bmp
    }

    private fun androidx.compose.ui.graphics.Color.toArgbInt(): Int = android.graphics.Color.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(),
    )
}
