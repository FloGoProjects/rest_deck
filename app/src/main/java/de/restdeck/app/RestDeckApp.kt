package de.restdeck.app

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import de.restdeck.app.data.Appearance
import de.restdeck.app.data.HttpClient
import de.restdeck.app.data.Repository
import de.restdeck.app.domain.ButtonRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

/** Ab Android 17 Runtime-Permission; vorher implizit über INTERNET (und unbekannt -> nicht prüfen). */
fun Context.hasLocalNetworkPermission(): Boolean =
    Build.VERSION.SDK_INT < 37 || checkSelfPermission(LOCAL_NETWORK_PERMISSION) == PackageManager.PERMISSION_GRANTED

class RestDeckApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        appearance = Appearance(filesDir, scope)
        repo = Repository(filesDir, ButtonRunner(HttpClient { hasLocalNetworkPermission() }), scope)
    }

    companion object {
        lateinit var instance: RestDeckApp; private set
        lateinit var repo: Repository; private set
        lateinit var appearance: Appearance; private set
        /** Lebt mit dem Prozess: Calls aus Shortcuts laufen weiter, auch wenn keine Activity offen ist. */
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}
