package com.lab.app;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.io.File;
import java.io.IOException;

public class LabApp {

    public static void main(String[] args) throws IOException {
        
        // 1. Create 'uploads' folder if it doesn't exist
        new File("uploads").mkdirs();

        // 2. Initialize the database (creates tables)
        Database.initialize();
        
        // 3. Create the server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // 4. Set all the handlers (Controllers)
        server.createContext("/", new LoginHandler());
        server.createContext("/patient", new PatientHandler());
        server.createContext("/staff", new StaffHandler());
        server.createContext("/doctor", new DoctorHandler());
        server.createContext("/uploads/", new FileServerHandler()); // Serves files
        
        server.setExecutor(null); // Use the default executor
        server.start();
        
        System.out.println("✅ Server running at http://localhost:8080/");
        System.out.println("   Database:  jdbc:postgresql://localhost:5432/" + "labdb");
        System.out.println("   Uploads: " + new File("uploads").getAbsolutePath());
    }
}