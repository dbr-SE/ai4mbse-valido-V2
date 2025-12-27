package com.hm.valido.installer;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class InstallerMain extends JFrame {

    private static final String APP_NAME = "VALIDO - AI4MBSE Plugin Installer";
    private final boolean isWindows;
    private final boolean isMac;

    // UI Layout & Panels
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // UI Fields für Dateneingabe
    private JTextField txtCatiaPath;
    private JTextField txtExportPath;
    private JPasswordField txtApiKey;
    private JCheckBox chkPersistKey;
    private JTextArea txtLog;

    // Gespeicherte Daten für die Installation
    private File catiaInstallDir;
    private File xmlExportDir;
    private String apiKey;

    public InstallerMain() {
        // 1. OS Erkennung
        String os = System.getProperty("os.name").toLowerCase();
        isWindows = os.contains("win");
        isMac = os.contains("mac");

        initUI();
    }

    private void initUI() {
        setTitle(APP_NAME);
        setSize(700, 500); // Etwas größer für bessere Lesbarkeit
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Zentrieren

        // Logo laden
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/images/logo.png"));
            setIconImage(icon.getImage());
        } catch (Exception e) {
            System.out.println("Hinweis: Logo 'logo.png' nicht in resources/images gefunden.");
        }

        // Layout Setup (CardLayout für Wizard-Style)
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- PANEL 1: WILLKOMMEN ---
        mainPanel.add(createWelcomePanel(), "WELCOME");

        // --- PANEL 2: PFADE KONFIGURIEREN ---
        mainPanel.add(createPathPanel(), "PATHS");

        // --- PANEL 3: API KEY ---
        mainPanel.add(createKeyPanel(), "APIKEY");

        // --- PANEL 4: INSTALLATION & LOG ---
        mainPanel.add(createInstallPanel(), "INSTALL");

        add(mainPanel);
    }

    // =================================================================================
    //       UI PANELS
    // =================================================================================

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel lblTitle = new JLabel("Willkommen beim Installer für VALIDO", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JLabel lblInfo = new JLabel("<html><center>Dieses Tool installiert das AI4MBSE Plugin für Catia Magic.<br><br>" +
                "Erkanntes System: <b>" + (isMac ? "macOS" : (isWindows ? "Windows" : "Unbekannt")) + "</b><br>" +
                "Bitte stellen Sie sicher, dass Catia Magic geschlossen ist.</center></html>", SwingConstants.CENTER);
        lblInfo.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JButton btnNext = new JButton("Installation starten ➔");
        btnNext.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnNext.addActionListener(e -> cardLayout.show(mainPanel, "PATHS"));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(btnNext);

        panel.add(lblTitle, BorderLayout.NORTH);
        panel.add(lblInfo, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPathPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- 1. Catia Magic Pfad ---
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(new JLabel("1. Installationsordner von Catia Magic / Cameo:"), gbc);

        gbc.gridy++; gbc.gridwidth = 1; gbc.weightx = 1.0;
        txtCatiaPath = new JTextField();
        panel.add(txtCatiaPath, gbc);

        gbc.gridx = 1; gbc.weightx = 0.0;
        JButton btnBrowseCatia = new JButton("Suchen...");
        btnBrowseCatia.addActionListener(e -> chooseDirectory(txtCatiaPath));
        panel.add(btnBrowseCatia, gbc);

        // --- 2. XML Export Pfad ---
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(20, 5, 5, 5); // Etwas Abstand nach oben
        panel.add(new JLabel("2. Speicherort für temporäre KI-Exporte (XML):"), gbc);

        gbc.gridy++; gbc.gridwidth = 1; gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        txtExportPath = new JTextField();
        panel.add(txtExportPath, gbc);

        gbc.gridx = 1; gbc.weightx = 0.0;
        JButton btnBrowseExport = new JButton("Suchen...");
        btnBrowseExport.addActionListener(e -> chooseDirectory(txtExportPath));
        panel.add(btnBrowseExport, gbc);

        // Auto-Detect Versuchen
        detectPaths();

        // --- Navigation ---
        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.insets = new Insets(30, 5, 5, 5);
        JButton btnNext = new JButton("Weiter zu API Key ➔");
        btnNext.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnNext.addActionListener(e -> {
            // Validierung
            if (txtCatiaPath.getText().isBlank() || txtExportPath.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Bitte beide Pfade angeben.", "Fehler", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Pfade speichern
            this.catiaInstallDir = new File(txtCatiaPath.getText());
            this.xmlExportDir = new File(txtExportPath.getText());
            cardLayout.show(mainPanel, "APIKEY");
        });
        panel.add(btnNext, gbc);

        return panel;
    }

    private JPanel createKeyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblInfo = new JLabel("<html><b>Gemini API Key konfigurieren</b><br><br>" +
                "Damit das Plugin funktioniert, wird ein API Key von Google benötigt.<br>" +
                "Dieser kann hier sicher als System-Umgebungsvariable (GEMINI_API_KEY) gespeichert werden.<br>" +
                "Falls Sie dies überspringen, müssen Sie den Key bei jedem Start manuell eingeben.</html>");
        panel.add(lblInfo, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(20, 5, 5, 5);
        panel.add(new JLabel("API Key:"), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(5, 5, 5, 5);
        txtApiKey = new JPasswordField(30);
        panel.add(txtApiKey, gbc);

        gbc.gridy++;
        chkPersistKey = new JCheckBox("Key dauerhaft im System speichern (Empfohlen)", true);
        panel.add(chkPersistKey, gbc);

        // Button: Installieren
        gbc.gridy++;
        gbc.weighty = 1.0; // Restplatz füllen
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.insets = new Insets(20, 5, 5, 5);

        JButton btnInstall = new JButton("Jetzt Installieren 🚀");
        btnInstall.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnInstall.addActionListener(e -> {
            this.apiKey = new String(txtApiKey.getPassword());
            cardLayout.show(mainPanel, "INSTALL");
            startInstallation(); // Startet den Prozess
        });
        panel.add(btnInstall, gbc);

        return panel;
    }

    private JPanel createInstallPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JLabel lblHeader = new JLabel("Installation läuft...", SwingConstants.CENTER);
        lblHeader.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(lblHeader, BorderLayout.NORTH);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtLog.setBackground(new Color(30, 30, 30)); // Dunkler Hintergrund für Log
        txtLog.setForeground(Color.LIGHT_GRAY);

        JScrollPane scrollPane = new JScrollPane(txtLog);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // =================================================================================
    //       LOGIK & HELPER
    // =================================================================================

    private void chooseDirectory(JTextField targetField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (!targetField.getText().isBlank()) {
            chooser.setCurrentDirectory(new File(targetField.getText()));
        }
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            targetField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength()); // Auto-Scroll
        });
    }

    private void detectPaths() {
        // Default Export Path: User Home Documents
        File docDir = new File(System.getProperty("user.home"), "Documents");
        txtExportPath.setText(docDir.getAbsolutePath());

        // Smart Detect für Catia
        File potentialPath = null;
        if (isWindows) {
            File programFiles = new File("C:\\Program Files\\Dassault Systemes");
            if (programFiles.exists()) {
                File[] subs = programFiles.listFiles((dir, name) -> name.contains("Magic") || name.contains("Cameo"));
                if (subs != null && subs.length > 0) potentialPath = subs[0];
            }
        } else if (isMac) {
            File app = new File("/Applications/Cameo Systems Modeler.app");
            if (app.exists()) potentialPath = app;
        }

        if (potentialPath != null) {
            txtCatiaPath.setText(potentialPath.getAbsolutePath());
        }
    }

    private void startInstallation() {
        new Thread(() -> {
            try {
                log("--- Start Installation ---");
                log("System: " + (isWindows ? "Windows" : (isMac ? "macOS" : "Linux/Other")));
                log("Zielverzeichnis: " + catiaInstallDir.getAbsolutePath());

                // 1. Environment Variable setzen
                if (apiKey != null && !apiKey.isBlank() && chkPersistKey.isSelected()) {
                    log("Setze Umgebungsvariable GEMINI_API_KEY...");
                    setEnvironmentVariable("GEMINI_API_KEY", apiKey);
                } else {
                    log("API Key wird übersprungen (leer oder nicht gewünscht).");
                }

                // 2. Plugins Ordner finden oder erstellen
                File pluginsDir = new File(catiaInstallDir, "plugins");
                if (!pluginsDir.exists()) {
                    // Fallback für Mac Package Contents, falls User nur die .app ausgewählt hat
                    if (isMac && catiaInstallDir.getName().endsWith(".app")) {
                        File contentPlugins = new File(catiaInstallDir, "Contents/plugins"); // Pfad bei manchen Versionen
                        File javaPlugins = new File(catiaInstallDir, "Contents/Resources/Java/plugins"); // Pfad bei anderen

                        if (contentPlugins.exists()) pluginsDir = contentPlugins;
                        else if (javaPlugins.exists()) pluginsDir = javaPlugins;
                    }
                }

                if (!pluginsDir.exists()) {
                    log("Info: 'plugins' Ordner nicht gefunden. Erstelle ihn...");
                    boolean created = pluginsDir.mkdirs();
                    if (!created) {
                        log("FEHLER: Konnte plugins Ordner nicht erstellen. Fehlende Rechte?");
                        // Wir machen trotzdem weiter, vielleicht existiert er und Java sieht es nur nicht
                    }
                }
                log("Installationspfad final: " + pluginsDir.getAbsolutePath());

                // 3. Kopieren (Aktuell Simulation - Später Payload)
                log("Kopiere Dateien...");
                Thread.sleep(800);
                log(">> Kopiere ai4mbse-plugin.jar... [SIMULATION OK]");

                Thread.sleep(500);
                log(">> Kopiere plugin.xml... [SIMULATION OK]");

                // TODO: Hier fügen wir später den echten Code ein, um die JARs aus den Ressourcen zu extrahieren

                log("--- Installation erfolgreich! ---");
                log("Bitte starten Sie Catia Magic neu.");

                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Installation erfolgreich abgeschlossen!\nBitte Catia neu starten.",
                                "Fertig",
                                JOptionPane.INFORMATION_MESSAGE)
                );

                // Optional: Fenster schließen oder offen lassen
                // System.exit(0);

            } catch (Exception e) {
                log("FEHLER: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Fehler: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }

    private void setEnvironmentVariable(String key, String value) {
        try {
            if (isWindows) {
                // Windows: setx für User-Variable (permanent)
                ProcessBuilder pb = new ProcessBuilder("setx", key, value);
                pb.start().waitFor();
                log("Windows Environment Variable gesetzt (User Scope).");
            } else {
                // Mac/Linux: Eintrag in .zshrc (für Terminal) & launchctl (für GUI Apps)
                String home = System.getProperty("user.home");
                File zshrc = new File(home, ".zshrc");

                // 1. In .zshrc schreiben (append)
                String exportCmd = "\n# Added by VALIDO Installer\nexport " + key + "=\"" + value + "\"\n";
                Files.writeString(zshrc.toPath(), exportCmd, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                log("Eintrag in ~/.zshrc hinzugefügt.");

                // 2. Für aktuelle Session und GUI Apps (launchctl)
                // Hinweis: launchctl setenv gilt bis zum Reboot. .zshrc gilt für neue Terminals.
                // Für GUI Apps (wie Catia) permanent auf Mac ist schwierig ohne Admin/Plist.
                // .zshrc reicht oft, wenn Catia aus dem Terminal gestartet wird.
                new ProcessBuilder("launchctl", "setenv", key, value).start().waitFor();
                log("Variable via launchctl gesetzt (Session).");
            }
        } catch (Exception e) {
            log("WARNUNG: Konnte Umgebungsvariable nicht setzen: " + e.getMessage());
            log("Bitte manuell setzen oder Catia neu starten.");
        }
    }

    public static void main(String[] args) {
        // Look and Feel setzen (FlatLaf Dark)
        try {
            FlatDarkLaf.setup();
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        SwingUtilities.invokeLater(() -> new InstallerMain().setVisible(true));
    }
}