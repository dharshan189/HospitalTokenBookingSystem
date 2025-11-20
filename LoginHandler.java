package com.lab.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;

public class LoginHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            // Serve the login page
            String html = Utils.readFile("login.html");
            
            // Clear the error message placeholder on a fresh page load
            html = html.replace("{{ERROR_MESSAGE}}", ""); 
            
            Utils.sendResponse(exchange, 200, html);
            
        } else if ("POST".equalsIgnoreCase(method)) {
            // Handle form submission
            Map<String, String> params = Utils.parseFormData(exchange.getRequestBody());

            // Handle New Patient Registration
            if (params.containsKey("newpatient")) {
                registerNewPatient(exchange);
                return;
            }

            // Handle Login
            String formType = params.getOrDefault("formType", "");
            switch (formType) {
                case "patient":
                    handleLogin(exchange, params.get("pid"), params.get("ppass"), "/patient?pid=" + params.get("pid"));
                    break;
                case "staff":
                    handleLogin(exchange, params.get("uname"), params.get("upass"), "/staff");
                    break;
                case "doctor":
                    handleLogin(exchange, params.get("dname"), params.get("dpass"), "/doctor");
                    break;
                default:
                    Utils.sendResponse(exchange, 400, "Invalid form submission.");
            }
        }
    }

    private void handleLogin(HttpExchange exchange, String user, String pass, String redirectUrl) throws IOException {
        if (user == null || pass == null) {
            Utils.sendResponse(exchange, 400, "Bad Request");
            return;
        }

        if (Database.checkLogin(user, pass)) {
            Utils.sendRedirect(exchange, redirectUrl);
        } else {
            // Instead of an alert, we reload the page with an inline error
            String response = Utils.readFile("login.html");
            
            // Fill the placeholder with the error message
            response = response.replace("{{ERROR_MESSAGE}}", "Invalid User ID or Password."); 
            
            Utils.sendResponse(exchange, 401, response);
        }
    }

    private void registerNewPatient(HttpExchange exchange) throws IOException {
        String newPid = Utils.generatePatientID();
        String newPass = Utils.generatePatientPassword(newPid);
        
        if (Database.registerPatient(newPid, newPass)) {
            String response = "<html><body style='font-family:Segoe UI; text-align:center; background:#e8f7e4;'>"
                + "<h2>New Patient Registered Successfully!</h2>"
                + "<h3>Your Patient ID: <b style='color:green;'>" + newPid + "</b></h3>"
                + "<h3>Your Password: <b style='color:blue;'>" + newPass + "</b></h3>"
                + "<a href='/' style='padding:10px 20px; background:#333; color:white; text-decoration:none;'>Back to Login</a>"
                + "</body></html>";
            Utils.sendResponse(exchange, 200, response);
        } else {
            Utils.sendResponse(exchange, 500, "Error during registration.");
        }
    }
}