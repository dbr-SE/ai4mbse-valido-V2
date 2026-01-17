package com.hm.ai4mbse.plugin.core;

import com.hm.ai4mbse.plugin.model.*;
import com.hm.ai4mbse.plugin.modules.database.JsonDatabaseService;
import com.hm.ai4mbse.plugin.modules.review.Create_Rule;
import com.hm.ai4mbse.plugin.modules.review.KI_Communication;
import com.hm.ai4mbse.plugin.modules.review.Run_Review;
import com.hm.ai4mbse.plugin.modules.ui.MainFrame;
import com.hm.ai4mbse.plugin.interfaces.UiController;
import com.hm.ai4mbse.plugin.modules.visualization.VisualizationService;

// EXPORT IMPORTS
import com.hm.ai4mbse.plugin.catiamsosa.ModelExportHelper;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.InputStream;

/**
 * Der "Super-Orchestrator".
 */
public class Orchestrator implements UiController {

    private final JsonDatabaseService database;
    private final VisualizationService visualization;
    private final KI_Communication kiCommunication;
    private final Create_Rule createRuleLogic;
    private final Run_Review runReviewLogic;

    // NEU: Referenz auf das Hauptfenster halten
    private MainFrame mainFrame;
    private File autoExportedFile;

    public Orchestrator() {
        this.kiCommunication = new KI_Communication();

        // 1. Sicherheits-Check: API Key prüfen
        // (Feature 5 machen wir später: Lazy Check. Aktuell bleibt es im Konstruktor)
        if (!ensureApiKeyExists()) {
            System.out.println("Orchestrator: Kein API Key. Start abgebrochen.");
        }

        this.database = new JsonDatabaseService();
        this.visualization = new VisualizationService();
        this.createRuleLogic = new Create_Rule();
        this.runReviewLogic = new Run_Review(createRuleLogic, kiCommunication);

        System.out.println("Orchestrator gestartet und bereit.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame(new Orchestrator()).setVisible(true));
    }

    public void startWithFile(File exportFile) {
        this.autoExportedFile = exportFile;
        System.out.println("Orchestrator: Datei empfangen: " + exportFile.getAbsolutePath());
    }

    // --- Geändert für Feature 1 (Minimierung verhindern) ---
    public void showMainWindow() {
        SwingUtilities.invokeLater(() -> {
            // Falls schon offen, nur nach vorne holen
            if (mainFrame != null && mainFrame.isVisible()) {
                mainFrame.toFront();
                return;
            }
            // Neu erstellen und Referenz speichern
            mainFrame = new MainFrame(this);
            mainFrame.setVisible(true);
            mainFrame.toFront();
        });
    }

    @Override
    public List<FormFieldDefinition> requestRuleFormStructure() {
        List<FormFieldDefinition> fields = new ArrayList<>();
        fields.add(new FormFieldDefinition("regeltitel", "Regeltitel (kurz):"));
        fields.add(new FormFieldDefinition("ziel", "Ziel der Regel (Was soll erreicht werden?):"));
        fields.add(new FormFieldDefinition("elementtypen", "Betroffene Elementtypen:"));
        fields.add(new FormFieldDefinition("pakete", "Zu prüfende Pakete:"));
        fields.add(new FormFieldDefinition("persona", "KI Persona:"));
        fields.add(new FormFieldDefinition("tonalitaet", "Tonalität:"));
        fields.add(new FormFieldDefinition("beispiel_korrekt", "Beispiel (Korrekt):"));
        fields.add(new FormFieldDefinition("beispiel_fehlerhaft", "Beispiel (Fehlerhaft):"));
        return fields;
    }

    @Override
    public void handleSaveRuleRequest(RuleDefinition ruleInput) {
        executeAsyncWithLoading("Regel wird generiert...", () -> {
            try {
                RuleCreationConfig config = new RuleCreationConfig();
                config.setRegeltitel(ruleInput.get("regeltitel"));
                config.setZiel(ruleInput.get("ziel"));
                config.setElementtypen(ruleInput.get("elementtypen"));
                config.setPakete(ruleInput.get("pakete"));
                config.setPersona(ruleInput.get("persona"));
                config.setTonalitaet(ruleInput.get("tonalitaet"));
                config.setBeispielKorrekt(ruleInput.get("beispiel_korrekt"));
                config.setBeispielFehlerhaft(ruleInput.get("beispiel_fehlerhaft"));
                config.setSprache("de");
                config.setStrenge("normal");
                config.setAusgabeformat("Markdown Tabelle");
                config.setPrueflogik(ruleInput.get("ziel"));

                String prompt = createRuleLogic.buildRuleCreationPrompt(config);
                String technicalRule = kiCommunication.callGeminiWithPrompt(prompt);

                RuleDefinition finalRule = new RuleDefinition();
                finalRule.getData().putAll(ruleInput.getData());
                finalRule.put("generated_prompt_request", prompt);
                finalRule.put("technical_prompt", technicalRule);

                database.saveRule(finalRule);
                // Fix: mainFrame als Parent nutzen
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame, "Regel erfolgreich generiert und gespeichert!"));
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame, "Fehler: " + e.getMessage()));
            }
        });
    }

    @Override
    public void handleDeleteRuleRequest(RuleDefinition rule) {
        database.deleteRule(rule);
    }

    @Override
    public List<RuleDefinition> handleLoadRulesRequest() {
        return database.loadRules();
    }

    @Override
    public List<RuleDefinition> handleLoadStandardRulesRequest() {
        return database.loadStandardRules();
    }

    @Override
    public void handleRunSingleRuleRequest(RuleDefinition rule) {
        String title = rule.get("regeltitel");
        handleRunReviewFromTab(rule, (items) -> {
            StringBuilder sb = new StringBuilder();
            sb.append("Review-Ergebnis für: ").append(title).append("\n\n");
            if (items.isEmpty()) {
                sb.append("Keine Fehler gefunden oder keine strukturierten Daten.");
            } else {
                for (ReviewDisplayItem item : items) {
                    sb.append("• ").append(item.getProblemColumn()).append("\n")
                            .append("  Element: ").append(item.getElementColumn()).append("\n\n");
                }
            }
            JTextArea area = new JTextArea(sb.toString());
            area.setEditable(false);
            // Fix: mainFrame als Parent nutzen
            JOptionPane.showMessageDialog(mainFrame, new JScrollPane(area), "Ergebnis", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // --- Feature 1 (Fixed) ---
    @Override
    public void handleRunReviewFromTab(RuleDefinition rule, Consumer<List<ReviewDisplayItem>> resultCallback) {
        String technicalRule = rule.get("technical_prompt");
        if (technicalRule == null || technicalRule.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Regel hat keinen technischen Prompt.");
            return;
        }

        executeAsyncWithLoading("Modellprüfung läuft...", () -> {
            try {
                // Check ob Export da ist
                if (this.autoExportedFile == null || !this.autoExportedFile.exists()) {
                    SwingUtilities.invokeLater(() ->
                            // Fix: mainFrame als Parent nutzen
                            JOptionPane.showMessageDialog(mainFrame,
                                    "Keine Modelldaten gefunden!\nBitte klicken Sie erst auf 'XML Exportieren'.",
                                    "Export fehlt",
                                    JOptionPane.WARNING_MESSAGE)
                    );
                    // WICHTIG: KEIN leeres Result senden, um das "Keine Verstöße" Fenster zu verhindern.
                    return;
                }
                File xmlFile = this.autoExportedFile;

                Path tempRuleFile = Files.createTempFile("active_rule_", ".txt");
                Files.writeString(tempRuleFile, technicalRule);

                ReviewStartConfig config = new ReviewStartConfig();
                config.setUseCase("Review starten");
                config.setRuleFile(tempRuleFile.toAbsolutePath().toString());
                String xmlPath = (xmlFile != null) ? xmlFile.getAbsolutePath() : "dummy_path.xml";
                config.setProjectXml(xmlPath);
                config.setSystemElementScope("Aktuelles Modell");
                config.getParameters().put("runId", "run_" + System.currentTimeMillis());

                Path tempDir = Files.createTempDirectory("ai4mbse_run_");
                Path dummyJsonAnchor = tempDir.resolve("anchor.json");

                runReviewLogic.startReview(config, dummyJsonAnchor);

                String runId = config.getParameters().get("runId");
                Path resultFile = tempDir.resolve("review_result_" + runId + ".txt");

                List<ReviewDisplayItem> displayItems = new ArrayList<>();
                if (Files.exists(resultFile)) {
                    String reportText = Files.readString(resultFile);
                    List<ReviewIssue> issues = visualization.parseTextReportToIssues(reportText);
                    displayItems = visualization.prepareDisplayData(issues);

                    Files.deleteIfExists(tempRuleFile);
                    Files.deleteIfExists(resultFile);
                    Files.deleteIfExists(tempDir);
                }

                List<ReviewDisplayItem> finalItems = displayItems;
                SwingUtilities.invokeLater(() -> resultCallback.accept(finalItems));

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame, "Fehler: " + e.getMessage()));
            }
        });
    }

    // --- Feature 1 (Fixed) ---
    @Override
    public void handleManualExportRequest() {
        executeAsyncWithLoading("Exportiere Modell...", () -> {
            try {
                Project project = Application.getInstance().getProject();
                if (project == null) {
                    throw new IllegalStateException("Kein aktives Projekt gefunden.");
                }

                File xmlFile = ModelExportHelper.createXmlSnapshot(project);
                this.autoExportedFile = xmlFile;

                SwingUtilities.invokeLater(() ->
                        // Fix: mainFrame als Parent nutzen
                        JOptionPane.showMessageDialog(mainFrame,
                                "Export erfolgreich!\nDatei: " + xmlFile.getName(),
                                "Info",
                                JOptionPane.INFORMATION_MESSAGE)
                );
            } catch (Throwable t) {
                t.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        // Fix: mainFrame als Parent nutzen
                        JOptionPane.showMessageDialog(mainFrame,
                                "Export fehlgeschlagen: " + t.getMessage(),
                                "Fehler",
                                JOptionPane.ERROR_MESSAGE)
                );
            }
        });
    }

    @Override
    public List<ReviewDisplayItem> handleDisplayRequest(String reviewType) { return new ArrayList<>(); }

    private void executeAsyncWithLoading(String title, Runnable task) {
        // Fix: mainFrame als Parent nutzen, damit Ladebalken AUF dem Fenster bleibt
        JDialog loadingDialog = new JDialog(mainFrame, title, true);

        JPanel p = new JPanel(new BorderLayout(20, 20));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        CircleLoader loader = new CircleLoader();
        loader.setPreferredSize(new Dimension(60, 60));

        JLabel timerLabel = new JLabel("0.0s", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        timerLabel.setForeground(Color.DARK_GRAY);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(loader, BorderLayout.CENTER);

        p.add(centerPanel, BorderLayout.CENTER);
        p.add(timerLabel, BorderLayout.SOUTH);

        loadingDialog.add(p);
        loadingDialog.setUndecorated(true);
        ((JPanel)loadingDialog.getContentPane()).setBorder(new javax.swing.border.LineBorder(Color.LIGHT_GRAY, 1));

        loadingDialog.pack();
        loadingDialog.setLocationRelativeTo(mainFrame); // Zentriert zum Plugin Fenster

        long startTime = System.currentTimeMillis();
        Timer uiTimer = new Timer(100, e -> {
            long duration = System.currentTimeMillis() - startTime;
            double seconds = duration / 1000.0;
            timerLabel.setText(String.format("%.1fs", seconds));
        });
        uiTimer.start();

        Thread worker = new Thread(() -> {
            try {
                task.run();
            } finally {
                SwingUtilities.invokeLater(() -> {
                    uiTimer.stop();
                    loader.stopAnimation();
                    loadingDialog.dispose();
                });
            }
        });

        worker.start();
        loadingDialog.setVisible(true);
    }

    private static class CircleLoader extends JPanel {
        private int angle = 0;
        private Timer animationTimer;
        public CircleLoader() {
            setOpaque(false);
            animationTimer = new Timer(40, e -> { angle = (angle + 15) % 360; repaint(); });
            animationTimer.start();
        }
        public void stopAnimation() { animationTimer.stop(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 10;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g2.setColor(new Color(230, 230, 230));
            g2.setStroke(new BasicStroke(5));
            g2.drawOval(x, y, size, size);
            g2.setColor(new Color(0, 120, 215));
            g2.draw(new Arc2D.Float(x, y, size, size, 90 - angle, 100, Arc2D.OPEN));
        }
    }

    // =================================================================
    //               API KEY HELPER
    // =================================================================

    private boolean ensureApiKeyExists() {
        while (!hasValidKey()) {
            Object[] options = {"Erneut prüfen", "Hilfe (?)", "Manuell eingeben"};
            // Hier nutzen wir noch 'null', da mainFrame beim Start noch nicht existiert.
            // Feature 5 wird das später komplett verschieben.
            int choice = JOptionPane.showOptionDialog(null,
                    "Der GEMINI_API_KEY wurde nicht gefunden!\nDas Plugin kann ohne Key keine Anfragen senden.", "Konfiguration fehlt",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);

            if (choice == 0) {
                continue;
            } else if (choice == 1) {
                showApiHelp();
            } else if (choice == 2) {
                String input = JOptionPane.showInputDialog(null, "Bitte API Key hier einfügen:", "Manuelle Eingabe", JOptionPane.PLAIN_MESSAGE);
                if (input != null && !input.isBlank()) {
                    handleManualApiKeySubmit(input);
                    return true;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean hasValidKey() {
        if (kiCommunication.hasSessionKey()) return true;
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) return true;
        Path keyFile = Path.of("api_key.txt");
        if (Files.exists(keyFile)) {
            try { return !Files.readString(keyFile).trim().isEmpty(); } catch (IOException e) { return false; }
        }
        return false;
    }

    private void showApiHelp() {
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/api_help.json");
            String json;
            if (is != null) {
                json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } else {
                json = "{\"windows\": \"Hilfe-Datei nicht gefunden (Classpath Error).\"}";
            }

            String windowsText = extractJsonValue(json, "windows");
            JTextArea textArea = new JTextArea("=== ANLEITUNG ===\n" + windowsText);
            textArea.setEditable(false);
            JOptionPane.showMessageDialog(null, new JScrollPane(textArea), "Hilfe", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1).replace("\\n", "\n") : "-";
    }

    @Override
    public void handleManualApiKeySubmit(String key) {
        if (key != null && !key.isBlank()) {
            kiCommunication.setSessionApiKey(key.trim());
        }
    }
}