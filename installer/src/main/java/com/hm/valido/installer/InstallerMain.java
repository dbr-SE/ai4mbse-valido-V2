package com.hm.valido.installer;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class InstallerMain extends JFrame {

    private static final String APP_NAME = "VALIDO - AI4MBSE Installer";
    private final boolean isWindows;
    private final boolean isMac;

    // UI Elemente
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JTextField txtCatiaPath;
    private JTextField txtExportPath;
    private JPasswordField txtApiKey;
    private JCheckBox chkPersistKey;
    private JTextArea txtLog;

    // Daten für die Installation
    private File catiaInstallDir;
    private File xmlExportDir;
    private String apiKey;

    public InstallerMain() {
        String os = System.getProperty("os.name").toLowerCase();
        isWindows = os.contains("win");
        isMac = os.contains("mac");
        initUI();
    }

    private void initUI() {
        setTitle(APP_NAME);
        setSize(750, 600); // Etwas höher für das Logo
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. Fenster-Icon setzen (Taskleiste / Dock)
        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/logo.png")));
            setIconImage(icon.getImage());
            // Für Mac Dock Icon (funktioniert ab Java 9+)
            if (isMac) {
                Taskbar.getTaskbar().setIconImage(icon.getImage());
            }
        } catch (Exception e) {
            System.out.println("Logo für Taskleiste nicht gefunden.");
        }

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Panels hinzufügen
        mainPanel.add(createWelcomePanel(), "WELCOME");
        mainPanel.add(createPathPanel(), "PATHS");
        mainPanel.add(createKeyPanel(), "APIKEY");
        mainPanel.add(createInstallPanel(), "INSTALL");

        add(mainPanel);
    }

    // --- SCREEN 1: WILLKOMMEN (MIT LOGO) ---
    private JPanel createWelcomePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 20)); // Abstand zwischen Elementen

        // A) Logo Bereich
        JLabel logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            ImageIcon originalIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/images/logo.png")));
            // Skalieren auf max 200px Breite/Höhe, aber Verhältnis beibehalten
            Image img = originalIcon.getImage();
            int width = 200;
            int height = (int) ((double) img.getHeight(null) / img.getWidth(null) * width);

            // Falls das Bild sehr hoch ist, begrenzen wir die Höhe
            if (height > 200) {
                height = 200;
                width = (int) ((double) img.getWidth(null) / img.getHeight(null) * height);
            }

            Image scaledImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            logoLabel.setIcon(new ImageIcon(scaledImg));
        } catch (Exception e) {
            logoLabel.setText("[LOGO]"); // Fallback
        }

        // B) Text Bereich
        JLabel t = new JLabel("Willkommen beim Installer für VALIDO", SwingConstants.CENTER);
        t.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel i = new JLabel("<html><center>Dieses Tool installiert das AI4MBSE Plugin für Catia Magic.<br><br>" +
                "Erkanntes System: <b>" + (isMac ? "macOS" : "Windows") + "</b><br>" +
                "Bitte stellen Sie sicher, dass Catia Magic geschlossen ist.</center></html>", SwingConstants.CENTER);
        i.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // Layout zusammenbauen
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        i.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(logoLabel);
        centerPanel.add(Box.createVerticalStrut(20)); // Abstand
        centerPanel.add(t);
        centerPanel.add(Box.createVerticalStrut(10)); // Abstand
        centerPanel.add(i);

        JButton b = new JButton("Installation starten ➔");
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.addActionListener(e -> cardLayout.show(mainPanel, "PATHS"));

        p.add(centerPanel, BorderLayout.CENTER);
        p.add(b, BorderLayout.SOUTH);
        return p;
    }

    // --- SCREEN 2: PFADE ---
    private JPanel createPathPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5,5,5,5); g.fill = GridBagConstraints.HORIZONTAL;

        // Catia Pfad
        g.gridx=0; g.gridy=0; g.gridwidth=2; p.add(new JLabel("1. Catia Magic Installationsordner:"), g);
        g.gridy++; g.gridwidth=1; g.weightx=1.0; txtCatiaPath = new JTextField(); p.add(txtCatiaPath, g);
        g.gridx=1; g.weightx=0.0; JButton b1 = new JButton("Suchen..."); b1.addActionListener(e -> chooseDir(txtCatiaPath)); p.add(b1, g);

        // Export Pfad
        g.gridx=0; g.gridy++; g.gridwidth=2; g.weightx=0.0; p.add(new JLabel("2. Ordner für KI-Exporte (XML):"), g);
        g.gridy++; g.gridwidth=1; g.weightx=1.0; txtExportPath = new JTextField(); p.add(txtExportPath, g);
        g.gridx=1; g.weightx=0.0; JButton b2 = new JButton("Suchen..."); b2.addActionListener(e -> chooseDir(txtExportPath)); p.add(b2, g);

        detectPaths();

        JButton next = new JButton("Weiter ➔");
        next.addActionListener(e -> {
            if(txtCatiaPath.getText().isBlank() || txtExportPath.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Bitte beide Pfade angeben.");
                return;
            }
            catiaInstallDir = new File(txtCatiaPath.getText());
            xmlExportDir = new File(txtExportPath.getText());
            cardLayout.show(mainPanel, "APIKEY");
        });
        g.gridx=0; g.gridy++; g.gridwidth=2; g.weighty=1.0; g.anchor=GridBagConstraints.SOUTH; p.add(next, g);
        return p;
    }

    // --- SCREEN 3: API KEY ---
    private JPanel createKeyPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5,5,5,5); g.fill=GridBagConstraints.HORIZONTAL;

        g.gridx=0; g.gridy=0; p.add(new JLabel("<html><b>Gemini API Key</b><br>Wird als Umgebungsvariable (GEMINI_API_KEY) gespeichert.</html>"), g);
        g.gridy++; txtApiKey = new JPasswordField(30); p.add(txtApiKey, g);
        g.gridy++; chkPersistKey = new JCheckBox("Dauerhaft im System speichern", true); p.add(chkPersistKey, g);

        JButton install = new JButton("Jetzt Installieren 🚀");
        install.setFont(new Font("SansSerif", Font.BOLD, 14));
        install.addActionListener(e -> {
            apiKey = new String(txtApiKey.getPassword());
            cardLayout.show(mainPanel, "INSTALL");
            startInstallation();
        });
        g.gridy++; g.weighty=1.0; g.anchor=GridBagConstraints.SOUTH; p.add(install, g);
        return p;
    }

    // --- SCREEN 4: LOG ---
    private JPanel createInstallPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("Installation läuft...", SwingConstants.CENTER), BorderLayout.NORTH);
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtLog.setBackground(new Color(40, 44, 52));
        txtLog.setForeground(new Color(171, 178, 191));
        p.add(new JScrollPane(txtLog), BorderLayout.CENTER);
        return p;
    }

    // --- HELPER & LOGIK ---

    private void chooseDir(JTextField f) {
        JFileChooser c = new JFileChooser(); c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if(!f.getText().isBlank()) c.setCurrentDirectory(new File(f.getText()));
        if(c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) f.setText(c.getSelectedFile().getAbsolutePath());
    }

    private void detectPaths() {
        txtExportPath.setText(new File(System.getProperty("user.home"), "Documents").getAbsolutePath());
        File pot = null;
        if(isMac) pot = new File("/Applications/Cameo Systems Modeler.app");
        if(isWindows) {
            File p = new File("C:\\Program Files\\Dassault Systemes");
            if(p.exists()) {
                File[] s = p.listFiles((d,n) -> n.contains("Magic") || n.contains("Cameo"));
                if(s!=null && s.length>0) pot = s[0];
            }
        }
        if(pot!=null) txtCatiaPath.setText(pot.getAbsolutePath());
    }

    private void log(String m) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(m + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private void createPluginConfig(File exportDir) {
        try {
            File configDir = new File(System.getProperty("user.home"), ".ai4mbse");
            if (!configDir.exists()) configDir.mkdirs();

            File configFile = new File(configDir, "config.properties");
            String content = "export_path=" + exportDir.getAbsolutePath();
            Files.writeString(configFile.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            log("Konfiguration gespeichert: " + configFile.getAbsolutePath());
            log("Ziel für Exporte: " + exportDir.getAbsolutePath());
        } catch (Exception e) {
            log("WARNUNG: Konnte Config nicht schreiben: " + e.getMessage());
        }
    }

    private void startInstallation() {
        new Thread(() -> {
            try {
                log("--- Start Installation ---");

                // 1. Env Var setzen
                if(apiKey != null && !apiKey.isBlank() && chkPersistKey.isSelected()) {
                    log("Setze API Key...");
                    if(isWindows) {
                        new ProcessBuilder("setx", "GEMINI_API_KEY", apiKey).start().waitFor();
                    } else {
                        // Mac/Linux Env var persist is tricky without restart/sourcing.
                        // We write to rc files but current session might not see it immediately in other terminal tabs
                        // but Catia launched via Finder usually picks up launchctl setenv
                        File rc = new File(System.getProperty("user.home"), ".zshrc");
                        Files.writeString(rc.toPath(), "\nexport GEMINI_API_KEY=\""+apiKey+"\"\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                        try {
                            new ProcessBuilder("launchctl", "setenv", "GEMINI_API_KEY", apiKey).start().waitFor();
                        } catch(Exception ex) { /* Ignore if launchctl fails */ }
                    }
                    log("API Key gesetzt.");
                }

                // 2. Plugins Ordner finden
                File plugins = new File(catiaInstallDir, "plugins");
                if(!plugins.exists() && isMac && catiaInstallDir.getName().endsWith(".app")) {
                    File c = new File(catiaInstallDir, "Contents/plugins");
                    File j = new File(catiaInstallDir, "Contents/Resources/Java/plugins");
                    if(c.exists()) plugins = c;
                    else if(j.exists()) plugins = j;
                }
                if (!plugins.exists()) plugins.mkdirs();
                log("Installiere in: " + plugins.getAbsolutePath());

                // 3. Kopieren
                copyRes("ai4mbse-plugin.jar", new File(plugins, "ai4mbse-plugin.jar"));
                copyRes("plugin.xml", new File(plugins, "plugin.xml"));

                // 4. Config schreiben
                createPluginConfig(xmlExportDir);

                log("--- Installation erfolgreich! ---");
                log("Bitte starten Sie Catia Magic neu.");

                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Installation erfolgreich!\nDas Plugin ist nun einsatzbereit."));

            } catch(Exception e) {
                log("FEHLER: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void copyRes(String name, File dest) throws Exception {
        log("Kopiere " + name + "...");
        try(java.io.InputStream is = getClass().getResourceAsStream("/"+name)) {
            if(is==null) throw new Exception("Ressource fehlt im Installer: "+name);
            Files.copy(is, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void main(String[] args) {
        try { FlatDarkLaf.setup(); } catch(Exception e){}
        SwingUtilities.invokeLater(() -> new InstallerMain().setVisible(true));
    }
}