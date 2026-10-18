package de.restdeck.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.restdeck.app.data.Repository
import de.restdeck.app.domain.Header
import de.restdeck.app.domain.HttpMethod
import de.restdeck.app.domain.LocalNetwork
import de.restdeck.app.domain.RestCall

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallEditor(repo: Repository, buttonId: String, callId: String, onBack: () -> Unit) {
    val deck by repo.deck.collectAsState()
    val c = deck.button(buttonId)?.calls?.firstOrNull { it.id == callId } ?: return
    fun save(nc: RestCall) = repo.edit { it.upsertCall(buttonId, nc) }
    val urlProblem = LocalNetwork.urlProblem(c.url)
    val bodyAllowed = c.method != HttpMethod.GET && c.method != HttpMethod.HEAD

    AppBackground {
        Column(Modifier.fillMaxSize().imePadding()) {
            TopBar("Call", subtitle = deck.button(buttonId)?.name, onBack = onBack) {
                QuickChip("Kopie") { repo.edit { it.duplicateCall(buttonId, c.id) }; onBack() }
                Spacer(Modifier.width(4.dp))
                GlassIconButton(Icons.Default.Delete, "Löschen", tint = Ink.Bad) { repo.edit { it.deleteCall(buttonId, c.id) }; onBack() }
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                OutlinedTextField(c.name, { save(c.copy(name = it)) }, label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = fieldColors(), shape = FieldShape)

                SectionLabel("Methode")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HttpMethod.entries.forEach { m ->
                        val sel = m == c.method
                        val col = m.color()
                        Text(
                            m.name,
                            Modifier.clip(FieldShape)
                                .background(if (sel) col.copy(alpha = 0.22f) else Ink.Card)
                                .border(Ink.StrokeWidth, if (sel) col else Ink.Stroke, FieldShape)
                                .clickable { save(c.copy(method = m)) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            color = if (sel) col else Ink.Muted, fontFamily = Mono, fontWeight = FontWeight.Bold,
                        )
                    }
                }

                SectionLabel("URL")
                OutlinedTextField(
                    c.url, { save(c.copy(url = it.replace("\n", ""))) },
                    textStyle = CodeStyle, modifier = Modifier.fillMaxWidth(), colors = fieldColors(), shape = FieldShape,
                    isError = urlProblem != null,
                    supportingText = { Text(urlProblem ?: "Ziel muss im lokalen Netz liegen (192.168.x, 10.x, .local …)", color = if (urlProblem != null) Ink.Bad else Ink.Muted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )

                SectionLabel("Header")
                c.headers.forEachIndexed { i, h ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(h.name, { v -> save(c.copy(headers = c.headers.replace(i, h.copy(name = v)))) },
                            placeholder = { Text("Name") }, singleLine = true, textStyle = CodeStyle,
                            modifier = Modifier.weight(1f), colors = fieldColors(), shape = FieldShape)
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(h.value, { v -> save(c.copy(headers = c.headers.replace(i, h.copy(value = v)))) },
                            placeholder = { Text("Wert") }, singleLine = true, textStyle = CodeStyle,
                            modifier = Modifier.weight(1.4f), colors = fieldColors(), shape = FieldShape)
                        IconButton(onClick = { save(c.copy(headers = c.headers.filterIndexed { j, _ -> j != i })) }) {
                            Icon(Icons.Default.Close, "Header entfernen", tint = Ink.Muted)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickChip("+ Header") { save(c.copy(headers = c.headers + Header("", ""))) }
                    if (c.headers.none { it.name.equals("Content-Type", true) })
                        QuickChip("+ JSON") { save(c.copy(headers = c.headers + Header("Content-Type", "application/json"))) }
                    if (c.headers.none { it.name.equals("Authorization", true) })
                        QuickChip("+ Bearer") { save(c.copy(headers = c.headers + Header("Authorization", "Bearer "))) }
                }

                SectionLabel(if (bodyAllowed) "Body" else "Body (bei ${c.method} nicht gesendet)")
                OutlinedTextField(
                    c.body, { save(c.copy(body = it)) }, enabled = bodyAllowed,
                    placeholder = { Text("""{"on": true}""", style = CodeStyle.copy(color = Ink.Muted)) },
                    textStyle = CodeStyle, modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp), colors = fieldColors(), shape = FieldShape,
                )

                SectionLabel("Timeout · ${"%.1f".format(c.timeoutMs / 1000.0)} s")
                Slider(
                    value = c.timeoutMs.toFloat(), onValueChange = { save(c.copy(timeoutMs = (it / 500).toInt() * 500)) },
                    valueRange = 500f..60_000f,
                    colors = SliderDefaults.colors(thumbColor = Ink.Accent2, activeTrackColor = Ink.Accent2, inactiveTrackColor = Ink.CardStrong),
                )
                Spacer(Modifier.height(16.dp))
                GradientButton("Fertig", Modifier.fillMaxWidth(), icon = Icons.Default.Done, onClick = onBack)
                Spacer(Modifier.navigationBarsPadding().height(24.dp))
            }
        }
    }
}

@Composable
private fun QuickChip(text: String, onClick: () -> Unit) {
    Text(
        text,
        Modifier.clip(PillShape).background(Ink.Card).border(Ink.StrokeWidth, Ink.Stroke, PillShape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        color = Ink.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
    )
}

private fun <T> List<T>.replace(i: Int, v: T): List<T> = mapIndexed { j, x -> if (j == i) v else x }
