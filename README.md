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

Voraussetzungen: JDK 17 oder neuer (`JAVA_HOME`), Android SDK mit Plattform 37
(Pfad über `ANDROID_HOME` oder `local.properties` mit `sdk.dir=…`).

```
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Unter Windows `gradlew.bat` statt `./gradlew`.

Ein signierter Release-Build (`:app:assembleRelease`) braucht `signing/keystore.properties` + Keystore –
beides ist absichtlich nicht im Repo. Ohne diese Dateien entsteht eine unsignierte Release-APK.

Anforderungen: [`requirements.json`](requirements.json) · Projekt-Notizen: [`CLAUDE.md`](CLAUDE.md)
