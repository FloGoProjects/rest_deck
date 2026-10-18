# RestDeck – Projekt-Notizen

Button-Board (Android, Kotlin/Compose): jeder Button feuert eine Liste von REST-Calls **parallel** ins **lokale Netz**,
Antworten nur ins Log. Anforderungen: `requirements.json` (Quelle der Wahrheit). Gerät: Fairphone 5.

## Datei-Landkarte
- `requirements.json` – Anforderungen R1–R10, Entscheidungen, Offenes
- `gradle/libs.versions.toml` – Versionen (übernommen von MorgenPlan, Stand 2026-10)
- `app/src/main/java/de/restdeck/app/`
  - `domain/Model.kt` – Deck/ActionButton/RestCall/LogEntry, Deck-Edits, JSON
  - `domain/Runner.kt` – `prepare` (Content-Type), `ButtonRunner` (parallel), `LogPolicy`, `summary`
  - `domain/LocalNetwork.kt` – private Adressbereiche, URL-Check (R6)
  - `domain/Styles.kt` – Stile als Daten (Modern, Viktorianisch, Schlicht, Terminal) + WCAG-Kontrast
  - `domain/Settings.kt` – AppSettings (Stil, Foto, Abdunkeln, Weichzeichnen) + JSON
  - `data/HttpClient.kt` – HttpURLConnection, DNS-Check „nur lokal“, keine Redirects
  - `data/Repository.kt` – StateFlows (deck, log, running, last), JSON in filesDir
  - `data/Appearance.kt` – settings.json + background.jpg (Import verkleinert, EXIF via ImageDecoder)
  - `trigger/Shortcuts.kt` – Pinned Shortcuts + Icon-Rendering; `TriggerActivity.kt` – unsichtbares Auslösen + Toast
  - `ui/Theme.kt` (LocalStyle, `Ink`-Tokens, Palette, Formen, AppBackground mit Foto), `ui/Decor.kt` (Aurora, Zahnräder, Scanlines, Nieten)
  - `ui/Components.kt` (Karten, Chips, Buttons), `ui/SettingsScreen.kt` (Stilwahl mit Vorschau, Foto + Regler)
  - `ui/HomeScreen.kt` (Kachel-Raster), `ui/ButtonEditor.kt`, `ui/CallEditor.kt`, `ui/LogScreen.kt`
  - `RestDeckApp.kt` (Repo, App-Scope, Permission-Check), `MainActivity.kt` (Routen-String-Navigation)
- `app/src/main/res/xml/network_security_config.xml` – Cleartext erlaubt (Schutz via R6 im Code)
- `signing/` – Release-Keystore + Passwörter (NIE weitergeben; sichern, sonst keine Updates)
- `dist/` – weitergebbare APKs
- `app/src/test/...` – Unit-Tests Domain

## Befehle (PowerShell, im Projektordner)
- Tests: `.\gradlew.bat :app:testDebugUnitTest`
- Build: `.\gradlew.bat :app:assembleDebug`
- Release-APK: `.\gradlew.bat :app:assembleRelease` -> nach `dist\RestDeck-<version>.apk` kopieren (R8 an, ~1,4 MB)
- Aufs Handy: `.\gradlew.bat :app:installDebug`
- adb: `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`
- Screenshot: `.\scripts\shot.ps1 <name>` -> `build\shots\<name>.png`
- App starten: `adb shell am start -n de.restdeck.app/.MainActivity` – NICHT `monkey` (tippt zufällig, feuert Buttons!)
- Shortcut simulieren: `adb shell am start -n de.restdeck.app/.trigger.TriggerActivity --es buttonId <id>`
- Echo-Gegenstelle: `python scripts/echo_server.py 8765`; Handy ohne WLAN: `adb reverse tcp:8765 tcp:8765` + URL `http://127.0.0.1:8765/...`
- Emulator (Android 17, testet auch ACCESS_LOCAL_NETWORK): `emulator -avd Medium_Phone_API_37.0 -no-window -gpu swiftshader_indirect`, adb mit `-e`. In Git-Bash `MSYS_NO_PATHCONV=1` setzen, sonst werden /sdcard-Pfade verbogen.
- JDK: über `JAVA_HOME` (hier JDK 25). Repo ist öffentlich -> KEINE rechnerspezifischen Pfade in `gradle.properties`.

## Konventionen
- Farben/Formen NIE hart kodieren: `Ink.*`, `Palette.*`, `CardShape`/`TileShape`/`PillShape` nutzen (lesen LocalStyle). Kachel-Schrift = `Palette.on(i)`.
- Neuer Stil = Eintrag in `Styles.all`; `StylesTest` prüft Kontraste und gleiche Farbfeld-Anzahl (8).
- `Ink.*` sind @Composable-Getter: in DrawScope-Lambdas vorher in lokale `val` ziehen.
- Vor jeder weitergegebenen APK `versionCode` hochzählen. Debug- und Release-Build haben verschiedene Signaturen: Wechsel nur per Deinstallation (Daten weg).
- TDD: Logik rein in `domain/` mit Tests; `HttpExecutor` ist das Interface, Tests nutzen Fakes.
- Jeder Call erzeugt genau einen LogEntry – auch bei ungültiger URL/Exception. Erfolg = Status 200–399.
- R6 wird zweimal geprüft: formal (`urlProblem`) und nach DNS-Auflösung (alle Adressen lokal) in `HttpClient`.
- targetSdk 37: `ACCESS_LOCAL_NETWORK` ist Runtime-Permission, nur bei API ≥ 37 prüfen/anfragen
  (`hasLocalNetworkPermission`). Ohne sie laufen TCP-Verbindungen nur in den Timeout.
- Calls laufen in `RestDeckApp.scope`, nicht im Composable-Scope (sonst bricht Navigation sie ab).
- Shortcut-ID = Button-ID; nach Umbenennen/Löschen `Shortcuts.refresh`.
- AGP 9 hat Kotlin eingebaut: KEIN `org.jetbrains.kotlin.android`-Plugin.
