package de.restdeck.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.restdeck.app.RestDeckApp
import de.restdeck.app.data.Repository
import de.restdeck.app.domain.ActionButton
import de.restdeck.app.domain.LogEntry
import de.restdeck.app.domain.TileDecor
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(repo: Repository, onEdit: (String) -> Unit, onNew: () -> Unit, onLog: () -> Unit, onSettings: () -> Unit) {
    val deck by repo.deck.collectAsState()
    val running by repo.running.collectAsState()
    val last by repo.last.collectAsState()
    val log by repo.log.collectAsState()
    val haptic = LocalHapticFeedback.current

    AppBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("RestDeck", subtitle = "${deck.buttons.size} Button${if (deck.buttons.size == 1) "" else "s"} · lokales Netz") {
                GlassIconButton(Icons.Default.Settings, "Aussehen", onClick = onSettings)
                GlassIconButton(Icons.AutoMirrored.Filled.List, "Log", tint = if (log.firstOrNull()?.success == false) Ink.Bad else Ink.Text, onClick = onLog)
            }
            if (deck.buttons.isEmpty()) {
                EmptyState(onNew)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(deck.buttons, key = { it.id }) { b ->
                        DeckTile(
                            b, running = b.id in running, result = last[b.id],
                            onFire = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                RestDeckApp.scope.launch {
                                    val r = repo.fire(b)
                                    haptic.performHapticFeedback(if (r.all { it.success }) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)
                                }
                            },
                            onEdit = { onEdit(b.id) },
                        )
                    }
                }
            }
        }
        if (deck.buttons.isNotEmpty()) {
            Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)) {
                GradientButton("Button", icon = Icons.Default.Add, onClick = onNew)
            }
        }
    }
}

@Composable
private fun EmptyState(onNew: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(96.dp).clip(TileShape).background(Palette.brush(0)), contentAlignment = Alignment.Center) {
            Text(LocalStyle.current.glyph.ifEmpty { "+" }, fontSize = 40.sp, color = Palette.on(0), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text("Dein Deck ist leer", color = Ink.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Lege einen Button an und hänge beliebig viele REST-Calls dran. Ein Tipp feuert alle parallel.",
            color = Ink.Muted, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        GradientButton("Ersten Button anlegen", icon = Icons.Default.Add, onClick = onNew)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckTile(b: ActionButton, running: Boolean, result: List<LogEntry>?, onFire: () -> Unit, onEdit: () -> Unit) {
    val style = LocalStyle.current
    val on = Palette.on(b.color)
    val ringColor = Ink.Accent2
    val scanTile = style.tileDecor == TileDecor.SCANLINES
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(dampingRatio = 0.45f, stiffness = 600f), label = "press")

    // Laufender Ring: rotierender Sweep-Verlauf um die Kachel
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "angle")
    val glow by spin.animateFloat(0.15f, 0.4f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow")

    // Ergebnis kurz aufblitzen lassen
    var flash by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(result) {
        if (result != null && result.isNotEmpty()) { flash = result.all { it.success }; kotlinx.coroutines.delay(900); flash = null }
    }
    val flashAlpha by animateFloatAsState(if (flash != null) 0.45f else 0f, tween(250), label = "flash")
    val flashColor = if (flash == false) Ink.Bad else Ink.Ok

    val shape = TileShape
    val ring by animateFloatAsState(if (running) 3f else 0f, tween(200), label = "ring")
    // Außen: rotierender Sweep-Verlauf, sichtbar als Ring, solange die Calls laufen
    Box(
        Modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(shape)
            .drawBehind {
                if (ring > 0f) rotate(angle) {
                    drawCircle(Brush.sweepGradient(listOf(Color.Transparent, on, ringColor, Color.Transparent)), radius = size.maxDimension)
                }
            }
            .padding(ring.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Palette.brush(b.color))
                .tileDecor(style, on)
                .border(if (scanTile) 1.dp else Ink.StrokeWidth, on.copy(alpha = if (scanTile) 0.8f else 0.18f), shape)
                .combinedClickable(interactionSource = interaction, indication = null, onClick = onFire, onLongClick = onEdit)
                .drawWithContent {
                    drawContent()
                    if (running) drawRect(on.copy(alpha = glow * 0.25f))
                    if (flashAlpha > 0f) drawRect(flashColor.copy(alpha = flashAlpha))
                }
                .padding(if (style.tileDecor == TileDecor.RIVETS) 22.dp else 16.dp),
        ) {
            if (style.glyph.isNotEmpty() || running) {
                Box(
                    Modifier.align(Alignment.TopStart).size(40.dp).clip(PillShape).background(Color.Black.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) { Text(if (running) "…" else style.glyph, fontSize = 18.sp, color = on, fontWeight = FontWeight.Bold) }
            }

            Box(
                Modifier.align(Alignment.TopEnd).size(36.dp).clip(PillShape).background(Color.Black.copy(alpha = 0.18f)).clickable(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Edit, "Bearbeiten", tint = on, modifier = Modifier.size(18.dp)) }

            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                val name = b.name.ifBlank { "Ohne Namen" }
                TileTitle(if (style.uppercaseTitles) name.uppercase() else name, on)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${b.calls.size} Call${if (b.calls.size == 1) "" else "s"}", color = on.copy(alpha = 0.8f), fontSize = 13.sp)
                    if (result != null && result.isNotEmpty()) {
                        val ok = result.count { it.success }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${if (ok == result.size) "✓" else "✕"} $ok/${result.size}",
                            Modifier.clip(PillShape).background(Color.Black.copy(alpha = 0.28f)).padding(horizontal = 8.dp, vertical = 2.dp),
                            color = if (ok == result.size) on else Color(0xFFFFE4E6), fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = Mono,
                        )
                    }
                }
            }
        }
    }
}

/** Kachel-Titel: Schrift so groß wie möglich, aber das längste Wort bricht nie mitten durch. */
@Composable
private fun TileTitle(text: String, color: Color) {
    val measurer = rememberTextMeasurer()
    val base = LocalTextStyle.current.copy(fontWeight = FontWeight.Black, color = color)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxPx = constraints.maxWidth
        val longest = text.split(' ').maxByOrNull { it.length }.orEmpty()
        val size = remember(text, maxPx, base) {
            (20 downTo 12).firstOrNull { sp ->
                measurer.measure(longest, base.copy(fontSize = sp.sp), maxLines = 1, softWrap = false).size.width <= maxPx
            } ?: 12
        }
        Text(text, style = base.copy(fontSize = size.sp, lineHeight = (size + 2).sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
