package com.lab.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date; // <-- This MUST be java.util.Date for formatting
import java.util.List;
import java.util.Map;

public class DoctorHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String html = Utils.readFile("doctor_dashboard.html");
        
        // --- UPDATED: Safer parsing logic to fix the crash ---
        String query = exchange.getRequestURI().getQuery();
        String filterDate = null;
        String searchPid = null;

        if (query != null) {
            for (String param : query.split("&")) {
                String[] parts = param.split("="); // Split into key and value
                
                // Check if a value actually exists (parts.length > 1)
                if (parts.length > 1) { 
                    String key = parts[0];
                    String value = parts[1];
                    
                    if (key.equals("filter_date") && !value.isEmpty()) {
                        filterDate = value;
                    }
                    if (key.equals("search_pid") && !value.isEmpty()) {
                        searchPid = value;
                    }
                }
                // If parts.length is 1 (e.g., "filter_date="), it's ignored,
                // which prevents the crash.
            }
        }

        // Call the flexible DB method
        List<Map<String, String>> reports = Database.getReports(filterDate, searchPid);

        // Replace placeholders to "remember" filter values
        html = html.replace("{{FILTER_DATE}}", (filterDate != null ? Utils.escapeHtml(filterDate) : ""));
        html = html.replace("{{SEARCH_PID}}", (searchPid != null ? Utils.escapeHtml(searchPid) : ""));
        // --- END OF UPDATE ---
        
        
        StringBuilder rows = new StringBuilder();
        if (reports.isEmpty()) {
            rows.append("<tr><td colspan='5'>No reports found for the selected filters.</td></tr>");
        } else {
            int idx = 1;
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a");
            for (Map<String, String> report : reports) {
                String pid = report.get("pid");
                String fname = report.get("filename");
                String timestamp = report.get("timestamp");
                
                // Use java.util.Date here for formatting
                String uploadedAt = sdf.format(new java.util.Date(Long.parseLong(timestamp))); 

                rows.append("<tr>");
                rows.append("<td>").append(idx++).append("</td>");
                rows.append("<td>").append(Utils.escapeHtml(pid)).append("</td>");
                rows.append("<td>").append(Utils.escapeHtml(fname.substring(fname.lastIndexOf('_') + 1))).append("</td>");
                rows.append("<td class='timestamp'>").append(uploadedAt).append("</td>");
                rows.append("<td>")
                    .append("<a href='/uploads/").append(fname).append("' target='_blank' class='view'>View</a> | ")
                    .append("<a href='/uploads/").append(fname).append("' download class='view'>Download</a>")
                    .append("</td>");
                rows.append("</tr>");
            }
        }
        
        html = html.replace("{{REPORT_ROWS}}", rows.toString());
        Utils.sendResponse(exchange, 200, html);
    }
}