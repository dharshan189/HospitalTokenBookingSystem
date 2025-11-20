package com.lab.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class FileServerHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath().replace("/uploads/", "");
        if (path.contains("..") || path.startsWith("/")) {
            Utils.sendResponse(exchange, 400, "Bad Request");
            return;
        }
        
        File file = new File("uploads", path);
        
        if (!file.exists() || !file.getCanonicalPath().startsWith(new File("uploads").getCanonicalPath())) {
            Utils.sendResponse(exchange, 404, "File Not Found");
            return;
        }

        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null) contentType = "application/octet-stream";
        
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, file.length());
        
        try (OutputStream os = exchange.getResponseBody()) {
            Files.copy(file.toPath(), os);
        }
    }
}