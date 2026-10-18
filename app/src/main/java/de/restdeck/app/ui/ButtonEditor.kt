package de.restdeck.app.ui

import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.restdeck.app.data.Repository
import de.restdeck.app.domain.ActionButton
import de.restdeck.app.domain.LocalNetwork
import de.restdeck.app.domain.RestCall
import de.restdeck.app.trigger.Shortcuts

@Composable
fun ButtonEditor(repo: Repository, buttonId: String, onBack: () -> Unit, onEditCall: (String) -> Unit, onNewCall: () -> Unit) {
    val deck by repo.deck.collectAsState()
    val b = deck.button(buttonId) ?: return
    val ctx = LocalContext.current
    val style = LocalStyle.current
    var confirmDelete by remember { mutableStateOf(false) }
    val index = deck.buttons.indexOfFirst { it.id == buttonId }
    fun save(nb: ActionButton) { repo.edit { it.upsertButton(nb) } }

    AppBackground {
        Column(Modifier.fillMaxSize().imePadding()) {
            TopBar("Button", subtitle = "${b.calls.size} Calls · feuern parallel", onBack = {
                Shortcuts.refresh(ctx, repo.deck.value.buttons, style); onBack()
            }) {
                GlassIconButton(Icons.Default.Delete, "Löschen", tint = Ink.Bad) { confirmDelete = true }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 8.dp, 16.dp, 120.dp)) {
                item {
                    // Vorschau
                    Box(
                        Modifier.fillMaxWidth().height(120.dp).clip(TileShape).background(Palette.brush(b.color)).tileDecor(style, Palette.on(b.color)).padding(24.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Text(b.name.ifBlank { "Ohne Namen" }, color = Palette.on(b.color), fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        b.name, { save(b.copy(name = it)) }, label = { Text("Name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), colors = fieldColors(), shape = FieldShape,
                    )
                    SectionLabel("Farbe")
                    val swatchCount = Palette.size
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items((0 until swatchCount).toList()) { i ->
                            val selected = i == b.color
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(Palette.brush(i))
                                    .border(if (selected) 3.dp else 0.dp, Ink.Text, CircleShape)
                                    .clickable { save(b.copy(color = i)) },
                                contentAlignment = Alignment.Center,
                            ) { if (selected) Icon(Icons.Default.Check, null, tint = Palette.on(i)) }
                        }
                    }
                    SectionLabel("Position & Homescreen")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassIconButton(Icons.Default.KeyboardArrowLeft, "Nach vorne") { repo.edit { it.moveButton(b.id, -1) } }
                        Text("${index + 1} / ${deck.buttons.size}", color = Ink.Muted, fontFamily = Mono)
                        GlassIconButton(Icons.Default.KeyboardArrowRight, "Nach hinten") { repo.edit { it.moveButton(b.id, +1) } }
                        Spacer(Modifier.weight(1f))
                        GradientButton("Homescreen", brush = Palette.brush(b.color), onColor = Palette.on(b.color), icon = Icons.Default.Home) {
                            val ok = Shortcuts.canPin(ctx) && Shortcuts.pin(ctx, b, style)
                            if (!ok) Toast.makeText(ctx, "Launcher unterstützt keine angehefteten Shortcuts", Toast.LENGTH_LONG).show()
                        }
                    }
                    SectionLabel("Calls")
                    if (b.calls.isEmpty()) {
                        GlassCard { Text("Noch keine Calls. Füge unten den ersten hinzu.", color = Ink.Muted) }
                    }
                }
                itemsIndexed(b.calls, key = { _, c -> c.id }) { _, c ->
                    CallRow(c, onClick = { onEditCall(c.id) })
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp)) {
            GradientButton("Call", icon = Icons.Default.Add, onClick = onNewCall)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("„${b.name}“ löschen?") },
            text = { Text("Der Button und seine ${b.calls.size} Calls werden entfernt. Homescreen-Shortcuts werden deaktiviert.") },
            confirmButton = {
                TextButton(onClick = {
                    repo.edit { it.deleteButton(b.id) }
                    Shortcuts.refresh(ctx, repo.deck.value.buttons, style)
                    confirmDelete = false
                    onBack()
                }) { Text("Löschen", color = Ink.Bad) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun CallRow(c: RestCall, onClick: () -> Unit) {
    val problem = LocalNetwork.urlProblem(c.url)
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MethodChip(c.method)
            Spacer(Modifier.width(10.dp))
            Text(c.name.ifBlank { "Ohne Namen" }, color = Ink.Text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${c.timeoutMs / 1000.0}s", color = Ink.Muted, fontSize = 12.sp, fontFamily = Mono)
        }
        Spacer(Modifier.height(6.dp))
        Text(c.url, style = CodeStyle.copy(color = if (problem == null) Ink.Muted else Ink.Bad), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (c.headers.isNotEmpty() || c.body.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(
                    c.headers.size.takeIf { it > 0 }?.let { "$it Header" },
                    c.body.takeIf { it.isNotEmpty() }?.let { "Body ${it.length} Zeichen" },
                ).joinToString(" · "),
                color = Ink.Muted, fontSize = 12.sp,
            )
        }
    }
}
