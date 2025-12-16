# AI4MBSE Plug-in Prototype

Ein KI-gestützter Assistent für **Catia Magic System of Systems Architect** (Cameo), entwickelt zur Optimierung von Traceability-Reviews und zur Identifikation von "Muda" (Verschwendung) im Systems Engineering.

> **Status:** Prototyp (Mockup-Phase)
> **Technologie:** Java 11, Swing, Maven

## 🎯 Features des Prototypen

Dieser Prototyp demonstriert die geplante Benutzeroberfläche (GUI) und die User Experience (UX) für folgende Funktionen:

* **Muda Detection:** Identifikation von isolierten Anforderungen ohne Trace-Links.
* **Traceability Review:** Tabellarische Übersicht des Modell-Status.
* **Natural Language Rules:** Eingabe von Prüfregeln in natürlicher Sprache (Simulation).
* **AI-Feedback:** Simulation von Verbesserungsvorschlägen durch einen KI-Agenten.

## 🛠 Developer Guide & Simulation Mode

Dieses Projekt verwendet ein **Mock-Service-Pattern**, um die Entwicklung der UI von der Backend-Logik (Catia API & KI-Anbindung) zu entkoppeln. Dies ermöglicht Rapid Prototyping ohne eine laufende Instanz von Catia Magic.

### Wie der Simulations-Modus funktioniert

Das Plug-in ist aktuell für den **Demo-Modus** konfiguriert:

* **Mock-Service:** Die Klasse `com.ai4mbse.service.mock.MockReviewService` simuliert die Antworten.
* **Daten:** Es werden statische Dummy-Daten zurückgegeben (z.B. simulierte "Muda"-Funde).
* **Latenz:** Eine künstliche Verzögerung simuliert die Berechnungszeit der KI, um Ladebalken zu testen.

### Anleitung für Entwickler

Um das Projekt in IntelliJ IDEA zu starten:

1.  Stellen Sie sicher, dass **Java 11 SDK** konfiguriert ist.
2.  Führen Sie die Klasse `com.ai4mbse.app.App` aus.
3.  Klicken Sie im UI auf **"Muda Detection"**, um den Analyse-Workflow zu simulieren.

---
*Projekt im Rahmen des AI4MBSE Forschungsprojekts.*