package com.hm.ai4mbse.plugin.modules.database;

import com.hm.ai4mbse.plugin.interfaces.DatabaseService;
import com.hm.ai4mbse.plugin.model.RuleDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonDatabaseService implements DatabaseService {

    private static final String DB_FILE = "rules_db.json";
    private static final String STD_DB_FILE = "std_rules_db.json";

    public JsonDatabaseService() {
        // Init: Sicherstellen, dass die Dateien existieren (aus Resources kopieren)
        ensureFileExists(DB_FILE);
        ensureFileExists(STD_DB_FILE);
    }

    private void ensureFileExists(String fileName) {
        Path path = Path.of(fileName);
        if (!Files.exists(path)) {
            System.out.println("[DB] Datei fehlt lokal: " + fileName + ". Versuche aus Resources zu kopieren...");

            // Versuch 1: Normaler Classloader
            InputStream in = getClass().getClassLoader().getResourceAsStream(fileName);

            // Versuch 2: Root-Pfad (wichtig für JARs)
            if (in == null) {
                in = getClass().getResourceAsStream("/" + fileName);
            }

            if (in != null) {
                try {
                    Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[DB] Erfolgreich kopiert: " + fileName);
                    in.close();
                } catch (IOException e) {
                    System.err.println("[DB] Fehler beim Kopieren von " + fileName);
                    e.printStackTrace();
                }
            } else {
                System.err.println("[DB] WARNUNG: Resource '" + fileName + "' nicht im Classpath/JAR gefunden!");
            }
        }
    }

    // --- Interface Methoden ---

    @Override
    public void saveRule(RuleDefinition newRule) {
        List<RuleDefinition> rules = loadRulesFromFile(DB_FILE);
        String newTitle = newRule.get("regeltitel");
        rules.removeIf(r -> r.get("regeltitel").equals(newTitle));
        rules.add(newRule);
        writeRulesToFile(DB_FILE, rules);
    }

    @Override
    public void deleteRule(RuleDefinition ruleToDelete) {
        List<RuleDefinition> rules = loadRulesFromFile(DB_FILE);
        String titleToDelete = ruleToDelete.get("regeltitel");
        boolean removed = rules.removeIf(r -> r.get("regeltitel").equals(titleToDelete));
        if (removed) {
            writeRulesToFile(DB_FILE, rules);
        }
    }

    @Override
    public List<RuleDefinition> loadRules() {
        return loadRulesFromFile(DB_FILE);
    }

    @Override
    public List<RuleDefinition> loadStandardRules() {
        return loadRulesFromFile(STD_DB_FILE);
    }

    // --- Interne Helper für Datei-Zugriff ---

    private List<RuleDefinition> loadRulesFromFile(String fileName) {
        Path path = Path.of(fileName);
        if (!Files.exists(path)) {
            // Versuch erneutes Kopieren falls gelöscht
            ensureFileExists(fileName);
            if (!Files.exists(path)) return new ArrayList<>();
        }

        try {
            String jsonContent = Files.readString(path, StandardCharsets.UTF_8);
            return parseJsonToRules(jsonContent);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void writeRulesToFile(String fileName, List<RuleDefinition> rules) {
        String jsonString = convertRulesToJson(rules);
        try {
            Files.writeString(Path.of(fileName), jsonString, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- ROBUSTE JSON ENGINE OHNE LIB ---

    private String convertRulesToJson(List<RuleDefinition> rules) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < rules.size(); i++) {
            RuleDefinition rule = rules.get(i);
            sb.append("  {\n");
            int mapSize = rule.getData().size();
            int counter = 0;
            for (Map.Entry<String, String> entry : rule.getData().entrySet()) {
                sb.append("    \"").append(entry.getKey()).append("\": ");
                sb.append("\"").append(escapeJson(entry.getValue())).append("\"");
                if (counter < mapSize - 1) sb.append(",");
                sb.append("\n");
                counter++;
            }
            sb.append("  }");
            if (i < rules.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<RuleDefinition> parseJsonToRules(String json) {
        List<RuleDefinition> list = new ArrayList<>();
        String inner = json.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
        if (inner.isBlank()) return list;

        String[] objects = inner.split("\\}\\s*,\\s*\\{");

        for (String objStr : objects) {
            RuleDefinition rule = new RuleDefinition();
            Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(objStr);

            while (matcher.find()) {
                String key = matcher.group(1);
                String value = unescapeJson(matcher.group(2));
                rule.put(key, value);
            }
            list.add(rule);
        }
        return list;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }
}