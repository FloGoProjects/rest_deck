package de.restdeck.app.trigger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import de.restdeck.app.MainActivity
import de.restdeck.app.RestDeckApp
import de.restdeck.app.domain.summary
import de.restdeck.app.hasLocalNetworkPermission
import kotlinx.coroutines.launch

/** Unsichtbar: feuert den Button aus dem Shortcut und zeigt das Ergebnis als Toast. */
class TriggerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = applicationContext
        val id = intent.getStringExtra(Shortcuts.EXTRA_BUTTON)
        val button = id?.let { RestDeckApp.repo.deck.value.button(it) }
        when {
            button == null -> Toast.makeText(app, "Button existiert nicht mehr", Toast.LENGTH_SHORT).show()
            !hasLocalNetworkPermission() -> {
                Toast.makeText(app, "Bitte einmal in der App „Lokales Netzwerk“ erlauben", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
            }
            else -> {
                Toast.makeText(app, "⚡ ${button.name} …", Toast.LENGTH_SHORT).show()
                RestDeckApp.scope.launch {
                    val entries = RestDeckApp.repo.fire(button)
                    Toast.makeText(app, summary(button.name, entries), Toast.LENGTH_LONG).show()
                }
            }
        }
        finish()
    }
}
