package com.hm.ai4mbse.plugin.modules.review;

import com.hm.ai4mbse.plugin.model.ReviewStartConfig;
import java.nio.file.Path;

/**
 * Use Case: Review starten.
 * Nutzt File-Upload und den ausführlichen Prompt (Java 11 Style).
 */
public class Run_Review {

    private final Create_Rule createRule;
    private final KI_Communication kiCommunication;

    public Run_Review(Create_Rule createRule, KI_Communication kiCommunication) {
        this.createRule = createRule;
        this.kiCommunication = kiCommunication;
    }

    public void startReview(ReviewStartConfig config, Path jsonSourcePath) {
        try {
            // 1. Regel laden
            String ruleText = createRule.loadRuleText(config.getRuleFile());

            // 2. Pfad holen (nicht lesen!)
            Path projectPath = Path.of(config.getProjectXml());

            // 3. Ausführlichen Prompt bauen
            String prompt = buildReviewPrompt(config.getSystemElementScope(), ruleText);

            System.out.println("Sende Prompt + XML File an Gemini...");

            // 4. KI aufrufen (File Upload)
            String aiResponse = kiCommunication.callGeminiWithPromptAndFile(prompt, projectPath);

            // 5. Speichern
            String runId = config.getParameters().getOrDefault("runId", "run");
            Path outputDir = jsonSourcePath.getParent();
            Path outputPath = outputDir.resolve("review_result_" + runId + ".txt");

            createRule.writeTextToFile(aiResponse, outputPath);
            System.out.println("Review Ergebnis gespeichert: " + outputPath);

        } catch (Exception e) {
            System.err.println("Fehler im Run_Review: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Der ausführliche Prompt (Java 11 kompatibel).
     * Fordert Berichtstruktur UND parsbares Format für die Tabelle.
     */
    private String buildReviewPrompt(String scope, String rule) {
        return "Du bist ein KI-Assistent für Model-Based Systems Engineering (MBSE).\n" +
                "Du arbeitest mit Catia Magic / MagicDraw-Systemmodellen.\n" +
                "Ich habe dir eine XML-Datei angehängt. Sie enthält das zu prüfende Modell.\n\n" +
                "Use Case: Führe einen Modell-Review durch.\n\n" +
                "Scope:\n" + scope + "\n\n" +
                "REGELBESCHREIBUNG:\n" +
                "---\n" + rule + "\n" +
                "---\n\n" +
                "AUFGABE:\n" +
                "- Wende die Regel konsequent auf das angehängte XML-Modell an.\n" +
                "- Erstelle einen strukturierten Review-Bericht.\n\n" +
                "AUSGABEFORMAT:\n" +
                "Antworte als reiner Klartext (kein Markdown-Block).\n" +
                "Halte dich strikt an folgende Struktur:\n\n" +
                "Modell-Review-Report\n" +
                "Datum: <Heute>\n\n" +
                "1. REVIEW-UMFANG\n" +
                "----------------\n" +
                "- Beschreibe in 2-3 Sätzen, was du geprüft hast.\n\n" +
                "2. ZUSAMMENFASSUNG DER REGEL\n" +
                "----------------------------\n" +
                "- Fasse kurz zusammen, was die Regel fordert.\n\n" +
                "3. BEFUNDE (WICHTIG FÜR PARSER)\n" +
                "-------------------------------\n" +
                "- Liste hier ALLE Verstöße auf.\n" +
                "- Nutze für JEDEN Verstoß exakt dieses Format (pro Zeile):\n" +
                "Severity: <FEHLER/WARNUNG/OK>; Element-ID: <ID>; Element-Name: <Name>; Element-Typ: <Typ>; Beschreibung: <Kurze Begründung>; Empfehlung: <Kurzer Lösungsvorschlag>\n" +
                "- Wenn alles OK ist, schreibe: \"Keine Verstöße gefunden.\"\n\n" +
                "4. FAZIT\n" +
                "--------\n" +
                "- Gib eine kurze Gesamtbewertung ab.\n" +
                "- Falls Informationen im XML fehlten (z.B. IDs), vermerke das hier.\n\n" +
                "Antworte sachlich und präzise.";
    }
}