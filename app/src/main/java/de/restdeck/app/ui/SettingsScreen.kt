package de.restdeck.app.ui

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.restdeck.app.data.Appearance
import de.restdeck.app.data.Repository
import de.restdeck.app.domain.AppSettings
import de.restdeck.app.domain.StyleSpec
import de.restdeck.app.domain.Styles
import de.restdeck.app.trigger.Shortcuts
import kotlinx.coroutines.launch

/** R11–R13: Stil wählen, Foto-Hintergrund setzen und einstellen. */
@Composable
fun SettingsScreen(appearance: Appearance, repo: Repository, onBack: () -> Unit) {
    val settings by appearance.settings.collectAsState()
    val photo by appearance.photo.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            importing = true
            try {
                appearance.importPhoto(ctx.contentResolver, uri)
            } catch (e: Exception) {
                Toast.makeText(ctx, "Foto konnte nicht geladen werden: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                importing = false
            }
        }
    }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    AppBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("Aussehen", subtitle = "Stil & Hintergrund", onBack = onBack)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                SectionLabel("Stil")
                Styles.all.forEach { spec ->
                    StylePreview(spec, selected = spec.id == settings.styleId) {
                        appearance.update { it.copy(styleId = spec.id) }
                        Shortcuts.refresh(ctx, repo.deck.value.buttons, spec)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                SectionLabel("Hintergrundfoto")
                val img = remember(photo) { photo?.asImageBitmap() }
                if (img != null) {
                    // Vorschau mit den aktuellen Reglern
                    val bg = Ink.Bg
                    Box(Modifier.fillMaxWidth().height(180.dp).clip(CardShape).border(Ink.StrokeWidth, Ink.Stroke, CardShape)) {
                        Image(
                            img, null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().let { if (Build.VERSION.SDK_INT >= 31 && settings.blur > 0f) it.blur(settings.blur.dp) else it },
                        )
                        Box(Modifier.fillMaxSize().drawBehind { drawRect(bg.copy(alpha = settings.dim)) })
                        GlassCard(Modifier.align(Alignment.Center)) { Text("So liest sich Text", color = Ink.Text, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientButton(if (importing) "Lädt …" else "Foto ändern", Modifier.weight(1f)) { if (!importing) pick() }
                        GradientButton("Entfernen", brush = androidx.compose.ui.graphics.SolidColor(Ink.Card), onColor = Ink.Bad, icon = Icons.Default.Delete) {
                            appearance.removePhoto()
                        }
                    }

                    SectionLabel("Abdunkeln · ${(settings.dim * 100).toInt()} %")
                    StyledSlider(settings.dim, 0f..AppSettings.MAX_DIM) { v -> appearance.update { it.copy(dim = v) } }
                    if (settings.dim < 0.3f) Text("Wenig abgedunkelt: Texte können auf hellen Fotos schlecht lesbar sein.", color = Ink.Muted, fontSize = 12.sp)

                    if (Build.VERSION.SDK_INT >= 31) {
                        SectionLabel("Weichzeichnen · ${settings.blur.toInt()}")
                        StyledSlider(settings.blur, 0f..AppSettings.MAX_BLUR) { v -> appearance.update { it.copy(blur = v) } }
                    } else {
                        Text("Weichzeichnen gibt es erst ab Android 12.", color = Ink.Muted, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Mit Foto werden die Verzierungen des Stils ausgeblendet; Farben, Schrift und Formen bleiben.", color = Ink.Muted, fontSize = 12.sp)
                } else {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text("Eigenes Foto als Hintergrund", color = Ink.Text, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Wird verkleinert in die App kopiert – das Original bleibt unberührt.", color = Ink.Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        GradientButton(if (importing) "Lädt …" else "Foto wählen") { if (!importing) pick() }
                    }
                }
                Spacer(Modifier.navigationBarsPadding().height(32.dp))
            }
        }
    }
}

@Composable
private fun StyledSlider(value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Slider(
        value = value, onValueChange = onChange, valueRange = range,
        colors = SliderDefaults.colors(thumbColor = Ink.Accent, activeTrackColor = Ink.Accent, inactiveTrackColor = Ink.Stroke),
    )
}

/** Mini-Vorschau eines Stils, gezeichnet in diesem Stil selbst. */
@Composable
private fun StylePreview(spec: StyleSpec, selected: Boolean, onClick: () -> Unit) {
    val outerShape = spec.shape(spec.cornerDp)
    val selColor = Ink.Accent
    val selOn = Ink.OnAccent
    CompositionLocalProvider(LocalStyle provides spec) {
        Box(
            Modifier.fillMaxWidth().height(140.dp).clip(outerShape)
                .background(spec.bg.toColor())
                .styleDecor(spec)
                .border(if (selected) 3.dp else 1.dp, if (selected) selColor else Ink.Stroke, outerShape)
                .clickable(onClick = onClick)
                .padding(16.dp),
        ) {
            Column(Modifier.align(Alignment.TopStart)) {
                Text(
                    if (spec.uppercaseTitles) spec.name.uppercase() else spec.name,
                    color = Ink.Text, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = spec.font.family(),
                    letterSpacing = if (spec.uppercaseTitles) 1.5.sp else 0.sp,
                )
                Text(spec.tagline, color = Ink.Muted, fontSize = 13.sp, fontFamily = spec.font.family())
            }
            Row(Modifier.align(Alignment.BottomStart), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until 4).forEach { i ->
                    Box(
                        Modifier.size(46.dp).clip(spec.shape((spec.tileCornerDp / 2).coerceAtLeast(2)))
                            .background(Palette.brush(spec, i))
                            .border(1.dp, Palette.on(i).copy(alpha = 0.3f), spec.shape((spec.tileCornerDp / 2).coerceAtLeast(2))),
                        contentAlignment = Alignment.Center,
                    ) { Text(spec.glyph.ifEmpty { "A" }, color = Palette.on(i), fontWeight = FontWeight.Bold, fontFamily = spec.font.family()) }
                }
            }
            if (selected) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(30.dp).clip(PillShape).background(selColor),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Check, "Aktiv", tint = selOn, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}
