package de.restdeck.app

import android.os.Build
import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import de.restdeck.app.domain.Styles
import de.restdeck.app.ui.Backdrop
import de.restdeck.app.ui.SettingsScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import de.restdeck.app.domain.ActionButton
import de.restdeck.app.domain.RestCall
import de.restdeck.app.ui.ButtonEditor
import de.restdeck.app.ui.CallEditor
import de.restdeck.app.ui.HomeScreen
import de.restdeck.app.ui.LogScreen
import de.restdeck.app.ui.RestDeckTheme

/** Navigation als Pfad-String: "" | "log" | "b/<id>" | "b/<id>/c/<id>" (überlebt Rotation). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBars(RestDeckApp.appearance.style.dark)
        if (!hasLocalNetworkPermission() && Build.VERSION.SDK_INT >= 37) requestPermissions(arrayOf(LOCAL_NETWORK_PERMISSION), 1)

        val repo = RestDeckApp.repo
        val appearance = RestDeckApp.appearance
        setContent {
            val settings by appearance.settings.collectAsState()
            val photo by appearance.photo.collectAsState()
            val style = Styles.byId(settings.styleId)
            val image = remember(photo) { photo?.asImageBitmap() }
            LaunchedEffect(style.dark) { applySystemBars(style.dark) }
            RestDeckTheme(style, Backdrop(image, settings.dim, settings.blur)) {
                var route by rememberSaveable { mutableStateOf("") }
                val parts = route.split("/")
                fun up() { route = when { parts.size >= 4 -> "b/${parts[1]}"; else -> "" } }
                BackHandler(enabled = route.isNotEmpty()) { up() }

                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        val forward = targetState.length > initialState.length
                        (slideInHorizontally(tween(260)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(260)))
                            .togetherWith(fadeOut(tween(180)))
                    },
                    label = "nav",
                ) { r ->
                    val p = r.split("/")
                    when {
                        r == "log" -> LogScreen(repo, onBack = { route = "" })
                        r == "settings" -> SettingsScreen(appearance, repo, onBack = { route = "" })
                        p.size >= 4 -> CallEditor(repo, p[1], p[3], onBack = { route = "b/${p[1]}" })
                        p.size >= 2 -> ButtonEditor(
                            repo, p[1], onBack = { route = "" },
                            onEditCall = { route = "b/${p[1]}/c/$it" },
                            onNewCall = {
                                val c = RestCall(name = "Call ${(repo.deck.value.button(p[1])?.calls?.size ?: 0) + 1}")
                                repo.edit { it.upsertCall(p[1], c) }
                                route = "b/${p[1]}/c/${c.id}"
                            },
                        )
                        else -> HomeScreen(
                            repo,
                            onEdit = { route = "b/$it" },
                            onNew = {
                                val n = repo.deck.value.buttons.size
                                val b = ActionButton(name = "Button ${n + 1}", color = n)
                                repo.edit { it.upsertButton(b) }
                                route = "b/${b.id}"
                            },
                            onLog = { route = "log" },
                            onSettings = { route = "settings" },
                        )
                    }
                }
            }
        }
    }

    /** Systemleisten transparent; Symbole hell auf dunklen Stilen, dunkel auf hellen. */
    private fun applySystemBars(dark: Boolean) {
        val bar = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
    }
}
