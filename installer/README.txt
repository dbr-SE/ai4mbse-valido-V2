============================================================
VALIDO - AI4MBSE Plugin Installer
Version 1.0
============================================================

Vielen Dank, dass Sie VALIDO nutzen!
Dieses Tool installiert das KI-Plugin automatisch in Ihre
Catia Magic / Cameo Systems Modeler Umgebung.

VORAUSSETZUNG:
Sie benötigen eine installierte Java-Laufzeitumgebung (JRE/JDK 11 oder höher).

-> Mac-Nutzer:
   Meistens bereits vorhanden.

-> Windows-Nutzer:
   Oft NICHT systemweit vorhanden (auch wenn Catia installiert ist).
   Falls der Installer nicht startet:
   1. Laden Sie Java herunter: https://adoptium.net/de/temurin/releases/
   2. Installieren Sie es (Standard-Einstellungen genügen).
   3. Versuchen Sie es erneut.

------------------------------------------------------------
KURZANLEITUNG (Für Eilige)
------------------------------------------------------------
1. Entpacken Sie dieses ZIP-Archiv vollständig.
2. Starten Sie die Installation:
   - Windows: Doppelklick auf "start_windows.bat"
   - macOS:   Doppelklick auf "start_mac.command"
3. Folgen Sie den Anweisungen im Fenster.
4. Starten Sie nach der Installation Catia Magic neu.

------------------------------------------------------------
DETAILLIERTE ANLEITUNG (Schritt für Schritt)
------------------------------------------------------------

SCHRITT 1: ENTPACKEN
Lassen Sie die Dateien nicht im ZIP-Ordner. Ziehen Sie den
gesamten Ordner "Valido_Installer_v1" auf Ihren Desktop oder
in Ihre Dokumente, bevor Sie starten.

SCHRITT 2: INSTALLER STARTEN
Öffnen Sie den entpackten Ordner.

-> Für Windows-Nutzer:
   Klicken Sie doppelt auf die Datei "start_windows.bat".

   Hinweis: Falls Windows eine blaue Warnmeldung zeigt ("Der Computer
   wurde durch Windows geschützt"):
   1. Klicken Sie auf "Weitere Informationen".
   2. Klicken Sie auf den Button "Trotzdem ausführen".

-> Für Mac-Nutzer:
   Klicken Sie doppelt auf die Datei "start_mac.command".

   WICHTIG (Beim ersten Start):
   Falls eine Meldung erscheint, dass die Datei nicht geöffnet werden kann,
   weil sie von einem nicht verifizierten Entwickler stammt:
   1. Klicken Sie mit der RECHTEN Maustaste auf die Datei.
   2. Wählen Sie im Kontextmenü "Öffnen".
   3. Bestätigen Sie das Fenster mit "Öffnen".
   (Dies ist eine Sicherheitsfunktion von macOS und nur einmalig nötig).

SCHRITT 3: PFADE WÄHLEN
Der Installer versucht, Ihren Catia-Ordner automatisch zu finden.
Falls das Feld leer ist, klicken Sie auf "Suchen..." und navigieren Sie
zu Ihrem Installationsverzeichnis (z.B. "C:\Program Files\Dassault Systemes\...").

Wählen Sie im zweiten Feld einen Ordner, in dem die KI-Exporte
(XML-Dateien) gespeichert werden sollen. Wir empfehlen einen separaten
Ordner auf dem Desktop oder in den Dokumenten.

SCHRITT 4: API KEY
Geben Sie Ihren Gemini API Key ein.
Setzen Sie den Haken bei "Dauerhaft speichern", damit der Key sicher
im System hinterlegt wird und Sie ihn nicht jedes Mal neu eingeben müssen.

SCHRITT 5: ABSCHLUSS
Klicken Sie auf "Installieren". Sobald im Logfenster "FERTIG!" steht,
können Sie den Installer schließen.
Starten Sie nun Ihre Catia Magic Anwendung neu. Das Plugin ist jetzt aktiv.

------------------------------------------------------------
SUPPORT / FEHLERBEHEBUNG
------------------------------------------------------------
Falls sich der Installer nicht öffnet:
- Prüfen Sie, ob Java korrekt installiert ist (z.B. durch Eingabe von
  "java -version" in der Kommandozeile).
- Versuchen Sie alternativ, die Datei "Valido_Installer.jar" direkt
  per Doppelklick zu öffnen.