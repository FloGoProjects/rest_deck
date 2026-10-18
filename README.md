# RestDeck

Android-App: Buttons, die jeweils eine Liste von REST-Calls **parallel** ins **lokale Netz** feuern.
Antworten landen im Log. Buttons lassen sich auf den Homescreen legen.

- Vier Stile: Modern, Viktorianisch (Steampunk), Schlicht, Terminal
- Eigenes Hintergrundfoto mit Abdunkeln/Weichzeichnen
- Nur lokale Ziele (10.x, 172.16–31.x, 192.168.x, 127.x, .local …), Daten bleiben auf dem Gerät

## Installieren

Fertige APK: [`dist/RestDeck-1.1.apk`](dist/RestDeck-1.1.apk) aufs Handy laden und öffnen
(Installation aus unbekannten Quellen erlauben). Mindestens Android 8.

## Bauen

```
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleRelease
```

Der Release-Build braucht `signing/keystore.properties` + Keystore – beides ist absichtlich nicht im Repo.
Ohne diese Dateien entsteht eine unsignierte Release-APK; zum Ausprobieren reicht `:app:assembleDebug`.

Anforderungen: [`requirements.json`](requirements.json) · Projekt-Notizen: [`CLAUDE.md`](CLAUDE.md)
