package com.lab.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class StaffHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            handleGet(exchange);
        } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            handlePost(exchange);
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        
        String query = exchange.getRequestURI().getQuery();
        String filterDateStr = null;

        if (query != null && query.contains("filter_date=")) {
            filterDateStr = query.split("filter_date=")[1].split("&")[0];
            if (filterDateStr.trim().isEmpty()) filterDateStr = null;
        }

        java.sql.Date sqlDate;
        if (filterDateStr == null) {
            sqlDate = java.sql.Date.valueOf(java.time.LocalDate.now());
            filterDateStr = sqlDate.toString();
        } else {
            sqlDate = java.sql.Date.valueOf(filterDateStr);
        }
        
        String html = Utils.readFile("staff_dashboard.html");
        html = html.replace("{{FILTER_DATE}}", Utils.escapeHtml(filterDateStr)); 

        List<String[]> tokens = Database.getTokensByDate(sqlDate);

        StringBuilder rows = new StringBuilder();
        if (tokens.isEmpty()) {
            rows.append("<tr><td colspan='13'>No tokens found for selected date.</td></tr>");
        } else {
            for (String[] t : tokens) {
                String tokenNum = Utils.escapeHtml(t[0]);
                String pid = Utils.escapeHtml(t[1]);
                String name = Utils.escapeHtml(t[2]);
                String test = Utils.escapeHtml(t[3]);
                String venue = Utils.escapeHtml(t[4]);
                String time = Utils.escapeHtml(t[5]);
                String mobile = Utils.escapeHtml(t[6]);
                String place = Utils.escapeHtml(t[7]);
                String blood = Utils.escapeHtml(t[8]);
                String presFile = t[9];
                String bookDate = (t[10] != null) ? Utils.escapeHtml(t[10]) : "N/A";
                // Get Status
                String status = (t.length > 11 && t[11] != null) ? t[11] : "Pending";
                
                rows.append("<tr>");
                rows.append("<td>").append(tokenNum).append("</td>");
                rows.append("<td>").append(pid).append("</td>");
                rows.append("<td>").append(name).append("</td>");
                rows.append("<td>").append(test).append("</td>");
                rows.append("<td>").append(mobile).append("</td>");
                rows.append("<td>").append(place).append("</td>");
                rows.append("<td>").append(blood).append("</td>");
                
                if (presFile != null && !presFile.isEmpty()) {
                    String encoded = URLEncoder.encode(presFile, StandardCharsets.UTF_8);
                    rows.append("<td><a href='/uploads/").append(encoded).append("' target='_blank'>")
                        .append("<img src='/uploads/").append(encoded).append("' width='100'></a></td>");
                } else {
                    rows.append("<td>No Image</td>");
                }
                
                rows.append("<td>").append(time).append("</td>");
                rows.append("<td>").append(venue).append("</td>");
                rows.append("<td>").append(bookDate).append("</td>");
                
                // Status Column
                String statusColor = "Pending".equals(status) ? "red" : "green";
                rows.append("<td style='color:").append(statusColor).append("; font-weight:bold;'>")
                    .append(Utils.escapeHtml(status)).append("</td>");
                
                // --- LOGIC CHANGE HERE ---
                rows.append("<td class='upload-form-cell'>");
                
                if ("Uploaded".equals(status)) {
                    // If uploaded, show a simple checkmark or text instead of the form
                    rows.append("<span style='color:green; font-size:20px;'>✔ Done</span>");
                } else {
                    // If Pending, show the upload form
                    rows.append("<form method='post' action='/staff' enctype='multipart/form-data'>");
                    rows.append("<input type='hidden' name='pid' value='").append(pid).append("'>");
                    rows.append("<input type='hidden' name='token_number' value='").append(tokenNum).append("'>");
                    rows.append("<input type='file' name='file' required>");
                    rows.append("<button type='submit' class='btn'>Upload</button>");
                    rows.append("</form>");
                }
                
                rows.append("</td>");
                // --- END OF CHANGE ---
                
                rows.append("</tr>");
            }
        }
        
        html = html.replace("{{TOKEN_ROWS}}", rows.toString());
        Utils.sendResponse(exchange, 200, html);
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        // ... (Post logic is unchanged) ...
        String contentType = exchange.getRequestHeaders().getFirst("Content-type");
        String boundary = "--" + contentType.split("boundary=")[1];
        InputStream input = exchange.getRequestBody();
        byte[] data = input.readAllBytes();
        String body = new String(data, "ISO-8859-1");

        String pid = null;
        String tokenNumber = null;
        byte[] fileBytes = null;
        String originalFilename = null;

        for (String part : body.split(boundary)) {
            if (!part.contains("Content-Disposition")) continue;
            if (part.contains("name=\"pid\"")) {
                pid = part.substring(part.indexOf("\r\n\r\n")+4, part.lastIndexOf("\r\n")).trim();
            } else if (part.contains("name=\"token_number\"")) {
                tokenNumber = part.substring(part.indexOf("\r\n\r\n")+4, part.lastIndexOf("\r\n")).trim();
            } else if (part.contains("name=\"file\"") && part.contains("filename=")) {
                originalFilename = part.split("filename=\"")[1].split("\"")[0];
                if (originalFilename != null && !originalFilename.isEmpty()) {
                    int s = part.indexOf("\r\n\r\n") + 4;
                    int e = part.lastIndexOf("\r\n");
                    fileBytes = Arrays.copyOfRange(data, body.indexOf(part) + s, body.indexOf(part) + e);
                }
            }
        }

        if (pid == null || tokenNumber == null || fileBytes == null) {
            Utils.sendResponse(exchange, 400, "Missing PID, Token Number, or File.");
            return;
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String savedFileName = pid + "_report_" + timestamp + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "");
        
        File file = new File("uploads", savedFileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(fileBytes);
            Database.addReport(pid, savedFileName, Long.parseLong(timestamp));
        }

        // Update status to "Uploaded"
        Database.updateTokenStatus(tokenNumber, "Uploaded");
        
        Utils.sendRedirect(exchange, "/staff");
    }
}