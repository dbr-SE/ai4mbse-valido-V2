package com.hm.ai4mbse.plugin.core;

import com.hm.ai4mbse.plugin.model.*;
import com.hm.ai4mbse.plugin.modules.database.JsonDatabaseService;
import com.hm.ai4mbse.plugin.modules.review.Create_Rule;
import com.hm.ai4mbse.plugin.modules.review.KI_Communication;
import com.hm.ai4mbse.plugin.modules.review.Run_Review;
import com.hm.ai4mbse.plugin.modules.ui.MainFrame;
import com.hm.ai4mbse.plugin.interfaces.UiController;
import com.hm.ai4mbse.plugin.modules.visualization.VisualizationService;

import com.hm.ai4mbse.plugin.catiamsosa.ModelExportHelper;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

public class Orchestrator implements UiController {

    private final JsonDatabaseService database;
    private final VisualizationService visualization;
    private final KI_Communication kiCommunication;
    private final Create_Rule createRuleLogic;
    private final Run_Review runReviewLogic;

    private MainFrame mainFrame;
    private File autoExportedFile;
    private volatile boolean currentProcessCancelled = false;

    public Orchestrator() {
        this.kiCommunication = new KI_Communication();
        // Feature 5: Lazy Check (Kein Check im Konstruktor)

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

    public void showMainWindow() {
        SwingUtilities.invokeLater(() -> {
            if (mainFrame != null && mainFrame.isVisible()) {
                mainFrame.toFront();
                return;
            }
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
        if (!ensureApiKeyExists()) return;

        currentProcessCancelled = false;
        executeAsyncWithLoading("Regel wird generiert...", () -> {
            try {
                if (currentProcessCancelled) return;
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

                if (currentProcessCancelled) return;

                RuleDefinition finalRule = new RuleDefinition();
                finalRule.getData().putAll(ruleInput.getData());
                finalRule.put("generated_prompt_request", prompt);
                finalRule.put("technical_prompt", technicalRule);

                database.saveRule(finalRule);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame, "Regel erfolgreich generiert und gespeichert!"));
            } catch (Exception e) {
                handleGeminiError(e);
            }
        });
    }

    @Override
    public void handleDeleteRuleRequest(RuleDefinition rule) { database.deleteRule(rule); }

    @Override
    public List<RuleDefinition> handleLoadRulesRequest() { return database.loadRules(); }

    @Override
    public List<RuleDefinition> handleLoadStandardRulesRequest() { return database.loadStandardRules(); }

    @Override
    public void handleRunSingleRuleRequest(RuleDefinition rule) {
        handleRunReviewFromTab(rule, (result) -> {
            SwingUtilities.invokeLater(() -> {
                if (result.getStatus() == ReviewResult.Status.ISSUES_FOUND) {
                    StringBuilder sb = new StringBuilder("Gefundene Probleme:\n");
                    for(ReviewDisplayItem item : result.getItems()) sb.append("- ").append(item.getProblemColumn()).append("\n");
                    JOptionPane.showMessageDialog(mainFrame, sb.toString());
                } else {
                    JOptionPane.showMessageDialog(mainFrame, result.getMessage());
                }
            });
        });
    }

    @Override
    public void handleRunReviewFromTab(RuleDefinition rule, Consumer<ReviewResult> resultCallback) {
        if (!ensureApiKeyExists()) return;

        String technicalRule = rule.get("technical_prompt");
        if (technicalRule == null || technicalRule.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, "Regel hat keinen technischen Prompt.");
            return;
        }

        currentProcessCancelled = false;

        executeAsyncWithLoading("Modellprüfung läuft...", () -> {
            try {
                if (currentProcessCancelled) return;
                if (this.autoExportedFile == null || !this.autoExportedFile.exists()) {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(mainFrame,
                                    "Keine Modelldaten gefunden!\nBitte klicken Sie erst auf 'XML Exportieren'.",
                                    "Export fehlt", JOptionPane.WARNING_MESSAGE)
                    );
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

                if (currentProcessCancelled) {
                    System.out.println("Prozess wurde abgebrochen. Ergebnisse werden verworfen.");
                    Files.deleteIfExists(tempRuleFile);
                    try { Files.deleteIfExists(tempDir); } catch (Exception ignored){}
                    return;
                }

                String runId = config.getParameters().get("runId");
                Path resultFile = tempDir.resolve("review_result_" + runId + ".txt");

                if (Files.exists(resultFile)) {
                    String reportText = Files.readString(resultFile);
                    VisualizationService.AnalysisResult analysis = visualization.analyzeReport(reportText);

                    Files.deleteIfExists(tempRuleFile);
                    Files.deleteIfExists(resultFile);
                    Files.deleteIfExists(tempDir);

                    ReviewResult uiResult;
                    switch (analysis.getStatus()) {
                        case SUCCESS_NO_ISSUES:
                            uiResult = new ReviewResult(ReviewResult.Status.SUCCESS,
                                    "Glückwunsch, dein Modell wurde erfolgreich gereviewed.\nVALIDO konnte keine Fehler erkennen.", new ArrayList<>());
                            break;
                        case PARSING_ERROR:
                            uiResult = new ReviewResult(ReviewResult.Status.FAILURE,
                                    "Der Review ist fehlgeschlagen (Parsing Error).\n\nDas Plugin konnte die Antwort der KI nicht lesen.\nBitte warten Sie eine Minute.", new ArrayList<>());
                            break;
                        case ISSUES_FOUND:
                        default:
                            List<ReviewDisplayItem> items = visualization.prepareDisplayData(analysis.getIssues());
                            uiResult = new ReviewResult(ReviewResult.Status.ISSUES_FOUND, "Fehler gefunden.", items);
                            break;
                    }
                    if (!currentProcessCancelled) {
                        SwingUtilities.invokeLater(() -> resultCallback.accept(uiResult));
                    }
                } else {
                    if (!currentProcessCancelled)
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame, "Fehler: Keine Ergebnisdatei."));
                }
            } catch (Exception e) {
                handleGeminiError(e);
            }
        });
    }

    @Override
    public void handleManualExportRequest() {
        currentProcessCancelled = false;
        executeAsyncWithLoading("Exportiere Modell...", () -> {
            try {
                if (currentProcessCancelled) return;
                Project project = Application.getInstance().getProject();
                if (project == null) throw new IllegalStateException("Kein aktives Projekt.");

                File xmlFile = ModelExportHelper.createXmlSnapshot(project);
                if (currentProcessCancelled) return;

                this.autoExportedFile = xmlFile;

                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(mainFrame, "Export erfolgreich!\n" + xmlFile.getName(), "Info", JOptionPane.INFORMATION_MESSAGE)
                );
            } catch (Throwable t) {
                if (!currentProcessCancelled) {
                    t.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame, "Export fehlgeschlagen: " + t.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE));
                }
            }
        });
    }

    @Override
    public void handleExportReportRequest(File targetFile, List<ReviewDisplayItem> results) {
        executeAsyncWithLoading("Speichere Bericht...", () -> {
            try {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile, StandardCharsets.UTF_8))) {
                    writer.write('\ufeff');
                    writer.write("Betroffenes Element;Problem;Priorität;Vorschlag\n");

                    for (ReviewDisplayItem item : results) {
                        String element = cleanCsv(item.getElementColumn());
                        String problem = cleanCsv(item.getProblemColumn());
                        String conf = cleanCsv(item.getConfidenceColumn());
                        String explanation = cleanCsv(item.getExplanationText().replaceAll("<[^>]*>", " "));

                        writer.write(element + ";" + problem + ";" + conf + ";" + explanation + "\n");
                    }
                }
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(mainFrame, "Bericht gespeichert unter:\n" + targetFile.getAbsolutePath(), "Gespeichert", JOptionPane.INFORMATION_MESSAGE)
                );
            } catch (IOException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(mainFrame, "Fehler beim Speichern: " + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE)
                );
            }
        });
    }

    // --- OPTIMIERTE FEHLERBEHANDLUNG ---
    private void handleGeminiError(Exception e) {
        if (currentProcessCancelled) return;

        // Sicherstellen, dass msg nicht null ist (bei NullPointerException)
        String rawMsg = e.getMessage();
        String msg = (rawMsg != null) ? rawMsg : e.toString();

        StringBuilder userText = new StringBuilder();
        userText.append("Prüfung konnte nicht abgeschlossen werden.\n\n");

        boolean isKnownError = false;

        // --- FALL 1: Bekannte API-Fehler (Schön dargestellt) ---

        if (msg.contains("429") || msg.toLowerCase().contains("quota") || msg.contains("423")) {
            userText.append("Grund: Nutzungslimit (Quota) von Google Gemini überschritten.\n");
            userText.append("Lösung: Bitte warten Sie kurz oder nutzen Sie einen anderen API Key.\n");
            isKnownError = true;
        }
        else if (msg.contains("400") || msg.contains("401") || msg.contains("403")) {
            userText.append("Grund: Der API Key scheint ungültig zu sein.\n");
            userText.append("Lösung: Bitte prüfen Sie den Key.\n");
            isKnownError = true;
        }
        else if (msg.contains("UnknownHost") || msg.contains("Socket") || msg.contains("Connect")) {
            userText.append("Grund: Keine Verbindung zum Internet.\n");
            userText.append("Lösung: Bitte prüfen Sie Ihre Verbindung.\n");
            isKnownError = true;
        }

        // --- FALL 2: Unbekannte Fehler (Müssen angezeigt werden zum Fixen!) ---

        if (!isKnownError) {
            userText.append("Ein unerwarteter technischer Fehler ist aufgetreten.\n");
            // HIER zeigen wir die Fehlermeldung an, damit wir wissen, was los ist
            userText.append("Fehler-Detail: ").append(msg).append("\n");
        }

        // Allgemeiner Hinweis für alle Fälle
        userText.append("\nSie können im Hauptmenü über 'API Key ändern' jederzeit einen neuen Schlüssel hinterlegen.");

        // Stacktrace in die Konsole für dich als Entwickler
        System.err.println("--- VALIDO ERROR ---");
        e.printStackTrace();

        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(mainFrame, userText.toString(), "Fehler", JOptionPane.ERROR_MESSAGE)
        );
    }
    // ----------------------------------------------------

    private String cleanCsv(String input) {
        if (input == null) return "";
        return input.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }

    @Override
    public List<ReviewDisplayItem> handleDisplayRequest(String reviewType) { return new ArrayList<>(); }

    private void executeAsyncWithLoading(String title, Runnable task) {
        JDialog loadingDialog = new JDialog(mainFrame, title, true);
        JPanel p = new JPanel(new BorderLayout(20, 20));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        CircleLoader loader = new CircleLoader();
        loader.setPreferredSize(new Dimension(60, 60));

        JLabel timerLabel = new JLabel("0.0s", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        timerLabel.setForeground(Color.DARK_GRAY);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(loader, BorderLayout.CENTER);

        p.add(centerPanel, BorderLayout.CENTER);
        p.add(timerLabel, BorderLayout.NORTH);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        southPanel.setBackground(Color.WHITE);
        JButton btnCancel = new JButton("Abbrechen");
        btnCancel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnCancel.setForeground(Color.RED);
        southPanel.add(btnCancel);
        p.add(southPanel, BorderLayout.SOUTH);

        loadingDialog.add(p);
        loadingDialog.setUndecorated(true);
        ((JPanel)loadingDialog.getContentPane()).setBorder(new javax.swing.border.LineBorder(Color.LIGHT_GRAY, 1));
        loadingDialog.pack();
        loadingDialog.setLocationRelativeTo(mainFrame);

        long startTime = System.currentTimeMillis();
        Timer uiTimer = new Timer(100, e -> {
            long duration = System.currentTimeMillis() - startTime;
            timerLabel.setText(String.format("%.1fs", duration / 1000.0));
        });
        uiTimer.start();

        Thread[] workerRef = new Thread[1];

        btnCancel.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(loadingDialog,
                    "Möchten Sie die Prüfung wirklich abbrechen?",
                    "Abbrechen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                currentProcessCancelled = true;
                if (workerRef[0] != null) workerRef[0].interrupt();
                uiTimer.stop();
                loader.stopAnimation();
                loadingDialog.dispose();
                System.out.println("User cancelled operation.");
            }
        });

        workerRef[0] = new Thread(() -> {
            try {
                task.run();
            } finally {
                if (loadingDialog.isVisible()) {
                    SwingUtilities.invokeLater(() -> {
                        uiTimer.stop();
                        loader.stopAnimation();
                        loadingDialog.dispose();
                    });
                }
            }
        });
        workerRef[0].start();

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

    private boolean ensureApiKeyExists() {
        while (!hasValidKey()) {
            Object[] options = {"Erneut prüfen", "Hilfe (?)", "Manuell eingeben"};
            Component parent = (mainFrame != null) ? mainFrame : null;
            int choice = JOptionPane.showOptionDialog(parent,
                    "Der GEMINI_API_KEY wurde nicht gefunden!\nDas Plugin kann ohne Key keine Anfragen senden.", "Konfiguration fehlt",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            if (choice == 0) continue;
            else if (choice == 1) showApiHelp();
            else if (choice == 2) {
                String input = JOptionPane.showInputDialog(parent, "Bitte API Key hier einfügen:", "Manuelle Eingabe", JOptionPane.PLAIN_MESSAGE);
                handleManualApiKeySubmit(input);
                if (hasValidKey()) return true;
            } else return false;
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
            InputStream is = getClass().getResourceAsStream("/api_help.json");
            String json = (is != null) ? new String(is.readAllBytes(), StandardCharsets.UTF_8) : "{}";
            JTextArea textArea = new JTextArea("=== ANLEITUNG ===\n" + extractJsonValue(json, "windows"));
            textArea.setEditable(false);
            JOptionPane.showMessageDialog(null, new JScrollPane(textArea), "Hilfe", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { e.printStackTrace(); }
    }
    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1).replace("\\n", "\n") : "-";
    }

    @Override
    public void handleManualApiKeySubmit(String key) {
        if (key != null && !key.isBlank()) kiCommunication.setSessionApiKey(key.trim());
    }
}