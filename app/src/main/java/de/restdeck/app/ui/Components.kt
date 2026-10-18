package de.restdeck.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.restdeck.app.domain.HttpMethod

@Composable
fun GlassCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(CardShape)
            .background(Ink.Card)
            .border(Ink.StrokeWidth, Ink.Stroke, CardShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun MethodChip(m: HttpMethod, modifier: Modifier = Modifier) {
    val c = m.color()
    Text(
        m.name,
        modifier
            .clip(PillShape)
            .background(c.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = c, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 12.sp,
    )
}

@Composable
fun StatusBadge(ok: Boolean, text: String) {
    val c = if (ok) Ink.Ok else Ink.Bad
    Row(
        Modifier.clip(PillShape).background(c.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(c))
        Spacer(Modifier.width(6.dp))
        Text(text, color = c, fontFamily = Mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TopBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Ink.Text) }
        } else Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val t = if (LocalStyle.current.uppercaseTitles) title.uppercase() else title
            Text(t, style = if (onBack == null) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleLarge, color = Ink.Text, maxLines = 1)
            if (subtitle != null) Text(subtitle, color = Ink.Muted, fontSize = 13.sp)
        }
        actions()
    }
}

@Composable
fun GlassIconButton(icon: ImageVector, desc: String, tint: Color = Ink.Text, onClick: () -> Unit) {
    Box(
        Modifier.padding(4.dp).size(44.dp).clip(PillShape).background(Ink.Card).border(Ink.StrokeWidth, Ink.Stroke, PillShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint) }
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, brush: Brush = Brush.linearGradient(listOf(Ink.Accent, Ink.Accent2)), onColor: Color = Ink.OnAccent, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        modifier.clip(FieldShape).background(brush).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = onColor); Spacer(Modifier.width(8.dp)) }
        Text(text, color = onColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), color = Ink.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp))
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Ink.Accent2, unfocusedBorderColor = Ink.Stroke,
    focusedContainerColor = Ink.Card, unfocusedContainerColor = Ink.Card,
    focusedLabelColor = Ink.Accent2, unfocusedLabelColor = Ink.Muted, cursorColor = Ink.Accent2,
)

