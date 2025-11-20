package com.lab.app;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Utils {

    private static Random rand = new Random();

    // Sends an HTML or text response
    public static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Sends a redirect (e.g., after login)
    public static void sendRedirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, -1);
    }

    // Reads an HTML file from the 'html' folder
    public static String readFile(String htmlFile) throws IOException {
        return Files.readString(Paths.get("html/" + htmlFile));
    }

    // Generates a new patient ID, checking the database
    public static String generatePatientID() {
        String pid;
        do {
            pid = String.valueOf(100000 + rand.nextInt(900000));
        } while (Database.patientExists(pid));
        return pid;
    }

    public static String generatePatientPassword(String pid) {
        return "patient@" + pid;
    }

    // Parses simple form data (x-www-form-urlencoded)
    public static Map<String, String> parseFormData(InputStream is) throws IOException {
        Map<String, String> params = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (line != null) {
                String[] pairs = line.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                        params.put(key, value);
                    }
                }
            }
        }
        return params;
    }
    
    // Escapes HTML to prevent XSS
    public static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}