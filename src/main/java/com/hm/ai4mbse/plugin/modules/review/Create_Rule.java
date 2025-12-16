package com.hm.ai4mbse.plugin.modules.review;

import com.hm.ai4mbse.plugin.model.RuleCreationConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Hilfsmodul für Regeln:
 * - Prompt-Erstellung für neue Regeln (Java 11 kompatibel)
 * - Datei-Helfer für den Review-Prozess
 */
public class Create_Rule {

    // ---------- File-Helfer (werden von Run_Review genutzt) ----------

    /**
     * Lädt den Inhalt einer Regeldatei als String.
     */
    public String loadRuleText(String ruleFilePath) throws IOException {
        Path path = Path.of(ruleFilePath);
        // Liest den reinen Text der Datei
        return Files.readString(path, StandardCharsets.UTF_8).trim();
    }

    public Path writeTextToFile(String content, Path targetFile) throws IOException {
        if (targetFile.getParent() != null) {
            Files.createDirectories(targetFile.getParent());
        }
        Files.writeString(targetFile, content, StandardCharsets.UTF_8);
        return targetFile;
    }

    // ---------- Use Case "Regel erstellen" (Prompt Builder) ----------

    /**
     * Baut den Systemprompt für die Regelgenerierung.
     * Java 11 Version (String.format statt Text Blocks).
     */
    public String buildRuleCreationPrompt(RuleCreationConfig c) {
        return String.format(
                "[Aufgabe]\n" +
                        "Du bist „Regelgenerator“ im Projekt AI4MBSE.\n" +
                        "Deine Aufgabe ist es, aus der Beschreibung einer gewünschten Review-Regel einen fertigen Systemprompt zu erzeugen.\n" +
                        "Dieser Systemprompt wird später von einem anderen KI-Modell verwendet, um ein MBSE-Modell in CATIA Magic / MagicDraw (SysML) auf Basis eines XML/XMI-Exports zu prüfen.\n" +
                        "\n" +
                        "[Kontext]\n" +
                        "Im Projekt AI4MBSE werden SysML-Modelle aus CATIA Magic exportiert (XML/XMI).\n" +
                        "Der Export enthält IDs, Namen, Texte, Beziehungen (Traces) und Stereotypen.\n" +
                        "Das nachgelagerte Prüf-LLM erhält: 1. Prüfaufgabe, 2. XML-Ausschnitt, 3. Parameter.\n" +
                        "\n" +
                        "[Eingabe des Nutzers]\n" +
                        "Regeltitel: %s\n" +
                        "\n" +
                        "Ziel der Regel:\n" +
                        "%s\n" +
                        "\n" +
                        "Scope:\n" +
                        "- Elementtypen: %s\n" +
                        "- Pakete: %s\n" +
                        "- Stereotypen: %s\n" +
                        "\n" +
                        "Fachliche Prüflogik:\n" +
                        "%s\n" +
                        "\n" +
                        "Gewünschtes Ausgabeformat:\n" +
                        "%s\n" +
                        "\n" +
                        "Strenge: %s\n" +
                        "Normen: %s\n" +
                        "\n" +
                        "Beispiele:\n" +
                        "- Korrekt: %s\n" +
                        "- Fehlerhaft: %s\n" +
                        "\n" +
                        "Sprache: %s\n" +
                        "Persona: %s\n" +
                        "Tonalität: %s\n" +
                        "\n" +
                        "[Deine Aufgabe]\n" +
                        "1. Analysiere die Beschreibung.\n" +
                        "2. Formuliere einen fertigen Systemprompt für das Prüf-LLM.\n" +
                        "   Der Systemprompt muss die Rolle, den XML-Kontext, die Prüfschritte und das Ausgabeformat definieren.\n" +
                        "3. Gib dein Ergebnis exakt in folgendem Format zurück (Klartext):\n" +
                        "\n" +
                        "Regeltitel: <Name>\n" +
                        "\n" +
                        "Kurzbeschreibung: <Text>\n" +
                        "\n" +
                        "Systemprompt:\n" +
                        "<Dein generierter Systemprompt>\n" +
                        "\n" +
                        "[Hinweise]\n" +
                        "- Erfinde keine Konventionen.\n" +
                        "- Keine Platzhalter [HIER EINFÜGEN] verwenden.\n",

                // Argumente
                c.getRegeltitel(),
                c.getZiel(),
                c.getElementtypen(),
                c.getPakete(),
                c.getStereotypen(),
                c.getPrueflogik(),
                c.getAusgabeformat(),
                c.getStrenge(),
                c.getNormen(),
                c.getBeispielKorrekt(),
                c.getBeispielFehlerhaft(),
                c.getSprache(),
                c.getPersona(),
                c.getTonalitaet()
        );
    }
}