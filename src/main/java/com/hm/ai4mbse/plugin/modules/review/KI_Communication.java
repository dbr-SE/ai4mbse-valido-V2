package com.hm.ai4mbse.plugin.modules.review;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Zuständig für den HTTP-Call zu Gemini.
 * Features:
 * - Text-only Requests
 * - Text + File Requests (XML Upload)
 * - Zero-Dependency (Kein org.json nötig)
 * - API Key aus Env oder api_key.txt
 */
public class KI_Communication {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GEMINI_ENDPOINT = GEMINI_BASE_URL + "/v1beta/models/gemini-2.5-flash:generateContent";

    /**
     * Text-only-Version.
     */
    public String callGeminiWithPrompt(String prompt) throws IOException, InterruptedException {
        String apiKey = getApiKey();
        String requestJson = buildTextOnlyRequestJson(prompt);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_ENDPOINT + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Gemini API Fehler (Text-only): " + response.statusCode() + "\nBody: " + response.body());
        }

        return extractTextFromResponse(response.body());
    }

    /**
     * Schickt Prompt + XML-Datei (über File API) an Gemini.
     */
    public String callGeminiWithPromptAndFile(String prompt, Path xmlFilePath) throws IOException, InterruptedException {
        String apiKey = getApiKey();
        HttpClient client = HttpClient.newHttpClient();

        // 1) XML-Datei zu Gemini hochladen -> file_uri
        String mimeType = "text/xml";
        String fileUri = uploadFileToGemini(client, apiKey, xmlFilePath, mimeType);

        // 2) Request-JSON bauen: Prompt + file_data-Part
        String requestJson = buildPromptWithFileRequestJson(prompt, fileUri, mimeType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_ENDPOINT + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Gemini API Fehler (Prompt+File): " + response.statusCode() + "\nBody: " + response.body());
        }

        return extractTextFromResponse(response.body());
    }

    // ------- File Upload (Gemini Files API) -------

    private String uploadFileToGemini(HttpClient client, String apiKey, Path filePath, String mimeType) throws IOException, InterruptedException {
        byte[] data = Files.readAllBytes(filePath);
        String startUrl = GEMINI_BASE_URL + "/upload/v1beta/files?key=" + apiKey;
        String metadataJson = "{\"file\":{\"display_name\":\"MODEL_XML\"}}";

        // 1) Upload-Session starten
        HttpRequest startRequest = HttpRequest.newBuilder()
                .uri(URI.create(startUrl))
                .header("X-Goog-Upload-Protocol", "resumable")
                .header("X-Goog-Upload-Command", "start")
                .header("X-Goog-Upload-Header-Content-Length", String.valueOf(data.length))
                .header("X-Goog-Upload-Header-Content-Type", mimeType)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(metadataJson))
                .build();

        HttpResponse<String> startResponse = client.send(startRequest, HttpResponse.BodyHandlers.ofString());

        if (startResponse.statusCode() != 200) {
            throw new IOException("Gemini File-Upload START fehlgeschlagen: " + startResponse.statusCode());
        }

        // Upload-URL aus Header auslesen
        Optional<String> uploadUrlOpt = startResponse.headers().firstValue("x-goog-upload-url");
        String uploadUrl = uploadUrlOpt.orElseThrow(() -> new IllegalStateException("Header 'x-goog-upload-url' fehlt."));

        // 2) Bytes hochladen
        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("X-Goog-Upload-Offset", "0")
                .header("X-Goog-Upload-Command", "upload, finalize")
                .header("Content-Type", mimeType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();

        HttpResponse<String> uploadResponse = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());

        if (uploadResponse.statusCode() != 200) {
            throw new IOException("Gemini File-Upload FINAL fehlgeschlagen: " + uploadResponse.statusCode());
        }

        // "uri" aus dem JSON Body extrahieren (String Parsing statt org.json)
        String body = uploadResponse.body();
        int uriIndex = body.indexOf("\"uri\"");
        if (uriIndex == -1) throw new IOException("Keine 'uri' im Upload-Response gefunden.");

        int firstQuote = body.indexOf("\"", uriIndex + 5);
        int secondQuote = body.indexOf("\"", firstQuote + 1);

        return body.substring(firstQuote + 1, secondQuote);
    }

    // ------- Request-JSON Builder -------

    private String buildTextOnlyRequestJson(String prompt) {
        return "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + escapeJson(prompt) + "\" }] }] }";
    }

    private String buildPromptWithFileRequestJson(String prompt, String fileUri, String mimeType) {
        return "{\n" +
                "  \"contents\": [\n" +
                "    {\n" +
                "      \"parts\": [\n" +
                "        { \"text\": \"" + escapeJson(prompt) + "\" },\n" +
                "        { \"file_data\": { \"mime_type\": \"" + mimeType + "\", \"file_uri\": \"" + escapeJson(fileUri) + "\" } }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    // ------- Hilfsfunktionen (Zero-Dependency) -------

    /**
     * Holt Key aus Env oder Datei.
     */
    private String getApiKey() throws IOException {
        // 1. Env
        String apiKey = System.getenv("GEMINI_API_KEY");

        // 2. Fallback Datei
        if (apiKey == null || apiKey.isBlank()) {
            Path keyFile = Path.of("api_key.txt");
            if (Files.exists(keyFile)) {
                apiKey = Files.readString(keyFile, StandardCharsets.UTF_8).trim();
            }
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Kein API Key gefunden! (ENV oder api_key.txt prüfen)");
        }
        return apiKey;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Extrahiert den Antworttext aus dem JSON.
     * Ersetzt die org.json Logik durch robustes String-Parsing.
     */
    private String extractTextFromResponse(String json) {
        if (json == null || json.isEmpty()) return "";

        // Suche nach "text": "..."
        String marker = "\"text\": \"";
        int start = json.indexOf(marker);

        if (start == -1) return cleanModelOutput(json); // Fallback auf Rohdaten

        start += marker.length();

        // Ende finden (beachte escaped Quotes)
        int i = start;
        boolean escaped = false;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\') {
                escaped = !escaped;
            } else if (c == '"' && !escaped) {
                break; // Ende gefunden
            } else {
                escaped = false;
            }
            i++;
        }

        if (i >= json.length()) return cleanModelOutput(json);

        String extracted = json.substring(start, i);
        return cleanModelOutput(unescapeJson(extracted));
    }

    private String unescapeJson(String text) {
        return text.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\\", "\\");
    }

    private String cleanModelOutput(String text) {
        if (text == null) return "";
        String trimmed = text.trim();

        // Code Fences entfernen
        if (trimmed.startsWith("```")) {
            int firstNL = trimmed.indexOf('\n');
            if (firstNL != -1) trimmed = trimmed.substring(firstNL + 1).trim();
            else trimmed = trimmed.substring(3).trim();
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }
        return trimmed;
    }
}