package com.hm.ai4mbse.plugin.modules.database;

import com.hm.ai4mbse.plugin.model.RuleDefinition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonDatabaseService {

    private static final String DB_FILE = "rules_db.json";

    public JsonDatabaseService() {}

    public void saveRule(RuleDefinition newRule) {
        List<RuleDefinition> rules = loadAllRules();
        String newTitle = newRule.get("regeltitel");
        // Update: Altes löschen, neues hinzufügen
        rules.removeIf(r -> r.get("regeltitel").equals(newTitle));
        rules.add(newRule);
        writeRulesToFile(rules);
    }

    public void deleteRule(RuleDefinition ruleToDelete) {
        List<RuleDefinition> rules = loadAllRules();
        String titleToDelete = ruleToDelete.get("regeltitel");
        boolean removed = rules.removeIf(r -> r.get("regeltitel").equals(titleToDelete));
        if (removed) writeRulesToFile(rules);
    }

    public List<RuleDefinition> loadAllRules() {
        if (!Files.exists(Path.of(DB_FILE))) return new ArrayList<>();
        try {
            String jsonContent = Files.readString(Path.of(DB_FILE), StandardCharsets.UTF_8);
            return parseJsonToRules(jsonContent);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void writeRulesToFile(List<RuleDefinition> rules) {
        String jsonString = convertRulesToJson(rules);
        try {
            Files.writeString(Path.of(DB_FILE), jsonString, StandardCharsets.UTF_8);
        } catch (IOException e) { e.printStackTrace(); }
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

        // Split nach Objekten }, {
        String[] objects = inner.split("\\}\\s*,\\s*\\{");

        for (String objStr : objects) {
            RuleDefinition rule = new RuleDefinition();
            // Verbesserter Regex: DOTALL Mode (?s) erlaubt Zeilenumbrüche im Value
            Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(objStr);

            while (matcher.find()) {
                String key = matcher.group(1);
                // Da Regex gierig sein kann, müssen wir aufpassen, dass wir nicht zu viel matchen.
                // Aber bei einfacher Struktur "key":"value" geht das meist.
                // Besserer Schutz gegen End-Quote im String:
                // Wir nehmen den Value und un-escapen ihn
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