package com.hm.ai4mbse.plugin.modules.ui;

import com.hm.ai4mbse.plugin.interfaces.UiController;
import com.hm.ai4mbse.plugin.model.FormFieldDefinition;
import com.hm.ai4mbse.plugin.model.ReviewDisplayItem;
import com.hm.ai4mbse.plugin.model.RuleDefinition;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

    private final UiController controller;
    private Map<String, JComponent> dynamicInputs;

    // UI Komponenten Tab 1 (Regeln verwalten)
    private JPanel rulesListContainer;

    // UI Komponenten Tab 2 (Review)
    private JComboBox<RuleDefinition> ruleSelector;
    private DefaultTableModel reviewTableModel;

    public MainFrame(UiController controller) {
        this.controller = controller;
        this.dynamicInputs = new HashMap<>();

        setTitle("AI4MBSE - Assistant");
        setSize(1350, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Regeln verwalten
        tabbedPane.addTab("Regeln verwalten", createRulesPanel());

        // Tab 2: Modell reviewen
        JPanel reviewPanel = createReviewPanel();
        tabbedPane.addTab("Modell reviewen", reviewPanel);

        // Listener: Wenn man auf den Review-Tab wechselt, Dropdown aktualisieren
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedComponent() == reviewPanel) {
                refreshRuleDropdown();
            }
        });

        add(tabbedPane);
    }

    // =================================================================================
    //       TAB 1: REGELN VERWALTEN
    // =================================================================================

    private JPanel createRulesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Linke Seite: Custom Liste
        rulesListContainer = new JPanel();
        rulesListContainer.setLayout(new BoxLayout(rulesListContainer, BoxLayout.Y_AXIS));

        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.add(rulesListContainer, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(northWrapper);
        scrollPane.setPreferredSize(new Dimension(550, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.WEST);

        // Rechte Seite: Formular
        panel.add(createFormPanel(), BorderLayout.CENTER);

        refreshRuleList();
        return panel;
    }

    private JPanel createFormPanel() {
        List<FormFieldDefinition> formStructure = controller.requestRuleFormStructure();

        JPanel formContent = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        dynamicInputs.clear();
        int gridY = 0;

        for (FormFieldDefinition fieldDef : formStructure) {
            gbc.gridx = 0; gbc.gridy = gridY; gbc.weightx = 0.3;
            // Hier Vergrößerung des Eingabefeldes für das "Ziel" (späteres Feature)
            // Aktuell Standard
            formContent.add(new JLabel(fieldDef.getLabel()), gbc);

            JComponent input;
            if (fieldDef.getType() == FormFieldDefinition.FieldType.DROPDOWN) {
                input = new JComboBox<>(fieldDef.getOptions());
            } else {
                input = new JTextField(20);
            }
            gbc.gridx = 1; gbc.weightx = 0.7;
            formContent.add(input, gbc);
            dynamicInputs.put(fieldDef.getId(), input);
            gridY++;
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClear = new JButton("Neu / Leeren");
        JButton btnSave = new JButton("KI-Generieren & Speichern");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));

        buttonPanel.add(btnClear);
        buttonPanel.add(btnSave);

        gbc.gridx = 0; gbc.gridy = gridY; gbc.gridwidth = 2;
        formContent.add(buttonPanel, gbc);

        btnSave.addActionListener(e -> {
            RuleDefinition newRule = new RuleDefinition();
            for (Map.Entry<String, JComponent> entry : dynamicInputs.entrySet()) {
                String key = entry.getKey();
                String value = "";
                JComponent comp = entry.getValue();
                if (comp instanceof JTextField) value = ((JTextField) comp).getText();
                else if (comp instanceof JComboBox) value = (String) ((JComboBox<?>) comp).getSelectedItem();
                newRule.put(key, value);
            }
            controller.handleSaveRuleRequest(newRule);
            refreshRuleList();
        });

        btnClear.addActionListener(e -> resetInputs());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(new JScrollPane(formContent), BorderLayout.CENTER);
        wrapper.setBorder(BorderFactory.createTitledBorder("Regel definieren"));
        return wrapper;
    }

    private void refreshRuleList() {
        rulesListContainer.removeAll();
        List<RuleDefinition> rules = controller.handleLoadRulesRequest();

        for (RuleDefinition rule : rules) {
            rulesListContainer.add(new RuleRowPanel(rule));
            rulesListContainer.add(Box.createVerticalStrut(2));
        }
        rulesListContainer.revalidate();
        rulesListContainer.repaint();
    }

    private class RuleRowPanel extends JPanel {
        private final JPanel buttonPanel;
        private final Color defaultColor = new Color(250, 250, 250);
        private final Color hoverColor = new Color(225, 240, 255);

        public RuleRowPanel(RuleDefinition rule) {
            setLayout(new BorderLayout(5, 5));
            setBorder(new CompoundBorder(
                    new LineBorder(Color.LIGHT_GRAY, 1),
                    new EmptyBorder(8, 8, 8, 8)
            ));
            setBackground(defaultColor);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

            JLabel titleLabel = new JLabel(rule.get("regeltitel"));
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            add(titleLabel, BorderLayout.CENTER);

            buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            buttonPanel.setOpaque(false);
            buttonPanel.setVisible(false);

            // Buttons
            JButton btnEdit = createTextBtn("Edit", "Regel bearbeiten", Color.DARK_GRAY);
            btnEdit.addActionListener(e -> loadRuleIntoForm(rule));

            JButton btnReq = createTextBtn("Prompt", "Gesendeter Prompt ansehen", Color.BLUE);
            btnReq.addActionListener(e -> showTextDialog("An Gemini gesendet", rule.get("generated_prompt_request")));

            JButton btnRes = createTextBtn("Result", "Erhaltene KI-Antwort ansehen", new Color(0, 100, 150));
            btnRes.addActionListener(e -> showTextDialog("Von Gemini empfangen", rule.get("technical_prompt")));

            JButton btnPlay = createTextBtn("Run", "Regel ausführen", new Color(0, 150, 0));
            btnPlay.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnPlay.addActionListener(e -> controller.handleRunSingleRuleRequest(rule));

            JButton btnDel = createTextBtn("X", "Löschen", Color.RED);
            btnDel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnDel.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(MainFrame.this, "Regel wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    controller.handleDeleteRuleRequest(rule);
                    refreshRuleList();
                }
            });

            buttonPanel.add(btnEdit);
            buttonPanel.add(btnReq);
            buttonPanel.add(btnRes);
            buttonPanel.add(btnPlay);
            buttonPanel.add(btnDel);

            add(buttonPanel, BorderLayout.EAST);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(hoverColor);
                    buttonPanel.setVisible(true);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), RuleRowPanel.this);
                    if (!RuleRowPanel.this.contains(p)) {
                        setBackground(defaultColor);
                        buttonPanel.setVisible(false);
                    }
                }
            };
            addMouseListener(ma);
            buttonPanel.addMouseListener(ma);
        }

        private JButton createTextBtn(String text, String tooltip, Color fgColor) {
            JButton b = new JButton(text);
            b.setToolTipText(tooltip);
            b.setForeground(fgColor);
            b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            b.setMargin(new Insets(2, 6, 2, 6));
            b.setFocusPainted(false);
            b.setContentAreaFilled(true);
            b.setBackground(Color.WHITE);
            b.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
            return b;
        }
    }

    // =================================================================================
    //       TAB 2: MODELL REVIEWEN (MIT US-BUTTONS)
    // =================================================================================

    private JPanel createReviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- OBERER BEREICH (Container für Konfiguration & US-Buttons) ---
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));

        // 1. Control Leiste (Manuelle Auswahl)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Manuelle Prüfungskonfiguration"));

        JLabel lblSelect = new JLabel("Regel auswählen:");

        // Dropdown für Regeln
        ruleSelector = new JComboBox<>();
        ruleSelector.setPreferredSize(new Dimension(300, 30));
        ruleSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof RuleDefinition) {
                    setText(((RuleDefinition) value).get("regeltitel"));
                }
                return this;
            }
        });

        JButton btnStart = new JButton("▶ Prüfung starten");
        btnStart.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnStart.setPreferredSize(new Dimension(150, 30));

        controlPanel.add(lblSelect);
        controlPanel.add(ruleSelector);
        controlPanel.add(btnStart);
// Button: API Key manuell
        JButton btnApiKey = new JButton("🔑 API Key");
        btnApiKey.setToolTipText("API Key manuell eingeben (für diese Sitzung)");
        btnApiKey.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnApiKey.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Bitte Gemini API Key eingeben:", "API Key Setup", JOptionPane.QUESTION_MESSAGE);
            if (input != null && !input.isBlank()) {
                controller.handleManualApiKeySubmit(input);
            }
        });
        controlPanel.add(Box.createHorizontalStrut(20)); // Abstand
        controlPanel.add(btnApiKey);

        // 2. User Story Schnellzugriff (NEU HINZUGEFÜGT)
        JPanel usPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        usPanel.setBorder(BorderFactory.createTitledBorder("Schnellzugriff: Standard-Prüfungen"));

        JButton btnUS01 = new JButton("Trace-Plausibilität");
        JButton btnUS02 = new JButton("Muda-Detection");
        JButton btnUS03 = new JButton("Lücken-Analyse");
        JButton btnUS04 = new JButton("Formale Qualität");

        // Helper für Button-Events
        ActionListener usAction = e -> {
            String command = e.getActionCommand(); // Liest den Command (Titel)
            runReviewByTitle(command);
        };

        // Die Commands müssen exakt den Titeln in der JSON entsprechen
        btnUS01.setActionCommand("Trace-Plausibilität"); btnUS01.addActionListener(usAction);
        btnUS02.setActionCommand("Muda-Detection");      btnUS02.addActionListener(usAction);
        btnUS03.setActionCommand("Lücken-Analyse");      btnUS03.addActionListener(usAction);
        btnUS04.setActionCommand("Formale Qualität");    btnUS04.addActionListener(usAction);

        usPanel.add(btnUS01);
        usPanel.add(btnUS02);
        usPanel.add(btnUS03);
        usPanel.add(btnUS04);

        topContainer.add(controlPanel);
        topContainer.add(Box.createVerticalStrut(5));
        topContainer.add(usPanel);

        // --- TABELLE ---
        String[] columns = {"Betroffenes Element", "Problem / Vorschlag", "Konfidenz", "Hilfe (?)"};
        reviewTableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int row, int column) { return column == 3; }
        };
        JTable table = new JTable(reviewTableModel);
        table.setRowHeight(30);

        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(600);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);

        table.getColumn("Hilfe (?)").setCellRenderer(new ButtonRenderer());
        table.getColumn("Hilfe (?)").setCellEditor(new ButtonEditor(new JCheckBox()));

        panel.add(topContainer, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Logik für den Start-Button (Manuell)
        btnStart.addActionListener(e -> {
            RuleDefinition selectedRule = (RuleDefinition) ruleSelector.getSelectedItem();
            if (selectedRule == null) {
                JOptionPane.showMessageDialog(this, "Bitte wähle erst eine Regel aus.");
                return;
            }
            executeReview(selectedRule);
        });

        refreshRuleDropdown();

        return panel;
    }

    /**
     * Führt eine Review-Regel anhand ihres Titels aus (für die US-Buttons).
     */
    private void runReviewByTitle(String ruleTitle) {
        List<RuleDefinition> rules = controller.handleLoadRulesRequest();
        RuleDefinition foundRule = null;
        for (RuleDefinition r : rules) {
            if (ruleTitle.equalsIgnoreCase(r.get("regeltitel"))) {
                foundRule = r;
                break;
            }
        }

        if (foundRule != null) {
            executeReview(foundRule);
        } else {
            JOptionPane.showMessageDialog(this, "Die Regel '" + ruleTitle + "' wurde in der Datenbank nicht gefunden.\nBitte lege sie unter 'Regeln verwalten' an (oder prüfe rules_db.json).");
        }
    }

    /**
     * Zentralisierte Methode zum Ausführen der Prüfung
     */
    private void executeReview(RuleDefinition rule) {
        reviewTableModel.setRowCount(0);
        controller.handleRunReviewFromTab(rule, results -> {
            for (ReviewDisplayItem item : results) {
                reviewTableModel.addRow(new Object[]{
                        item.getElementColumn(),
                        item.getProblemColumn(),
                        item.getConfidenceColumn(),
                        item.getExplanationText()
                });
            }
            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Keine Verstöße gefunden (oder Parsing fehlgeschlagen).");
            }
        });
    }

    private void refreshRuleDropdown() {
        if (ruleSelector == null) return;
        ruleSelector.removeAllItems();
        List<RuleDefinition> rules = controller.handleLoadRulesRequest();
        for (RuleDefinition r : rules) {
            ruleSelector.addItem(r);
        }
    }

    // =================================================================================
    //       HELPER METHODEN
    // =================================================================================

    private void showTextDialog(String title, String content) {
        JTextArea area = new JTextArea(content != null && !content.isEmpty() ? content : "[Keine Daten gespeichert]");
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(this, scroll, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadRuleIntoForm(RuleDefinition rule) {
        for (Map.Entry<String, JComponent> entry : dynamicInputs.entrySet()) {
            String fieldId = entry.getKey();
            JComponent comp = entry.getValue();
            String value = rule.get(fieldId);
            if (comp instanceof JTextField) ((JTextField) comp).setText(value);
            else if (comp instanceof JComboBox) ((JComboBox<?>) comp).setSelectedItem(value);
        }
    }

    private void resetInputs() {
        for (JComponent comp : dynamicInputs.values()) {
            if (comp instanceof JTextField) ((JTextField) comp).setText("");
            else if (comp instanceof JComboBox && ((JComboBox<?>)comp).getItemCount() > 0)
                ((JComboBox<?>) comp).setSelectedIndex(0);
        }
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); setText("?"); }
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) { return this; }
    }

    class ButtonEditor extends DefaultCellEditor {
        private String currentHtml;
        public ButtonEditor(JCheckBox c) { super(c); }
        public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
            currentHtml = (String) v;
            JButton b = new JButton("?");
            b.addActionListener(e -> {
                JEditorPane ep = new JEditorPane("text/html", currentHtml);
                ep.setEditable(false);
                ep.setBackground(new Color(245, 245, 245));
                JScrollPane sp = new JScrollPane(ep);
                sp.setPreferredSize(new Dimension(400, 300));
                JOptionPane.showMessageDialog(t, sp, "Detailbericht", JOptionPane.PLAIN_MESSAGE);
                fireEditingStopped();
            });
            return b;
        }
        public Object getCellEditorValue() { return currentHtml; }
    }
}