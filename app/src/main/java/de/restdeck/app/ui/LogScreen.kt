package de.restdeck.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.restdeck.app.data.Repository
import de.restdeck.app.domain.LogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("dd.MM. HH:mm:ss", Locale.GERMANY)

@Composable
fun LogScreen(repo: Repository, onBack: () -> Unit) {
    val log by repo.log.collectAsState()
    AppBackground {
        Column(Modifier.fillMaxSize()) {
            TopBar("Log", subtitle = "${log.size} Einträge · ${log.count { !it.success }} Fehler", onBack = onBack) {
                if (log.isNotEmpty()) GlassIconButton(Icons.Default.Delete, "Log leeren", tint = Ink.Bad) { repo.clearLog() }
            }
            if (log.isEmpty()) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Noch nichts geloggt", color = Ink.Muted)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(log, key = { it.id }) { LogCard(it) }
                }
            }
        }
    }
}

@Composable
private fun LogCard(e: LogEntry) {
    var open by rememberSaveable(e.id) { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth().animateContentSize(), onClick = { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(e.success, e.status?.toString() ?: "ERR")
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${e.buttonName} · ${e.callName}", color = Ink.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${timeFmt.format(Date(e.time))} · ${e.durationMs} ms", color = Ink.Muted, fontSize = 12.sp, fontFamily = Mono)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MethodChip(e.method)
            Spacer(Modifier.width(8.dp))
            Text(e.url, style = CodeStyle.copy(color = Ink.Muted, fontSize = 12.sp), maxLines = if (open) 4 else 1, overflow = TextOverflow.Ellipsis)
        }
        val detail = e.error ?: e.body
        if (detail.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SelectionContainer {
                Text(
                    detail,
                    Modifier.fillMaxWidth().clip(FieldShape).background(Ink.CodeBg).padding(10.dp),
                    style = CodeStyle.copy(color = if (e.error != null) Ink.Bad else Ink.Text, fontSize = 12.sp),
                    maxLines = if (open) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
