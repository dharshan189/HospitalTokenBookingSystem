package com.lab.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Date; 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {

    // --- !! UPDATE THESE !! ---
    private static final String DB_NAME = "labdb"; 
    private static final String DB_USER = "postgres"; 
    private static final String DB_PASS = "2006"; // <-- CHANGE THIS
    // --------------------------
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/" + DB_NAME;

    public static void initialize() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            String sql1 = "CREATE TABLE IF NOT EXISTS patients (pid TEXT PRIMARY KEY, password TEXT NOT NULL);";

            // Ensure 'status' column exists
            String sql2 = "CREATE TABLE IF NOT EXISTS tokens (" +
                          "  token_number TEXT PRIMARY KEY," +
                          "  pid TEXT," +
                          "  name TEXT," +
                          "  test_type TEXT," +
                          "  venue TEXT," +
                          "  scheduled_time TEXT," +
                          "  mobile TEXT," +
                          "  place TEXT," +
                          "  blood_group TEXT," +
                          "  prescription_file TEXT," +
                          "  booking_date DATE," +
                          "  status TEXT" + 
                          ");";

            String sql3 = "CREATE TABLE IF NOT EXISTS reports (report_id SERIAL PRIMARY KEY, pid TEXT, filename TEXT, upload_timestamp BIGINT);";

            stmt.execute(sql1);
            stmt.execute(sql2);
            stmt.execute(sql3);

            // Safe migration for existing tables
            try {
                stmt.execute("ALTER TABLE tokens ADD COLUMN IF NOT EXISTS status TEXT;");
            } catch (Exception e) { /* Ignore if exists */ }

            stmt.execute("INSERT INTO patients (pid, password) VALUES ('dharshanss', 'staff123') ON CONFLICT (pid) DO NOTHING;");
            stmt.execute("INSERT INTO patients (pid, password) VALUES ('doctor1', 'doc123') ON CONFLICT (pid) DO NOTHING;");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Login & Patient Methods (Unchanged) ---
    public static boolean checkLogin(String pid, String password) {
        String sql = "SELECT password FROM patients WHERE pid = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pid);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getString("password").equals(password);
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
    
    public static boolean patientExists(String pid) {
        String sql = "SELECT 1 FROM patients WHERE pid = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pid);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static boolean registerPatient(String pid, String password) {
        String sql = "INSERT INTO patients(pid, password) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pid);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
    
    public static boolean checkDuplicateToken(String name, String mobile, String bloodGroup, java.sql.Date bookingDate) {
        String sql = "SELECT 1 FROM tokens WHERE " +
                     "((name = ? AND mobile = ?) OR (name = ? AND blood_group = ?) OR (mobile = ? AND blood_group = ?)) " +
                     "AND booking_date = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name); pstmt.setString(2, mobile);
            pstmt.setString(3, name); pstmt.setString(4, bloodGroup);
            pstmt.setString(5, mobile); pstmt.setString(6, bloodGroup);
            pstmt.setDate(7, bookingDate);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); 
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static int countTokensByTestAndDate(String testType, java.sql.Date date) {
        String sql = "SELECT COUNT(*) FROM tokens WHERE test_type = ? AND booking_date = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, testType);
            pstmt.setDate(2, date);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        } catch (Exception e) { e.printStackTrace(); return 0; }
    }

    // --- Token Methods ---
    public static void addToken(String[] tokenData, java.sql.Date bookingDate) {
        // Set default status to 'Pending'
        String sql = "INSERT INTO tokens (token_number, pid, name, test_type, venue, scheduled_time, mobile, place, blood_group, prescription_file, booking_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending')";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < tokenData.length; i++) pstmt.setString(i + 1, tokenData[i]);
            pstmt.setDate(11, bookingDate);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- NEW: Update Status instead of Deleting ---
    public static void updateTokenStatus(String tokenNumber, String status) {
        String sql = "UPDATE tokens SET status = ? WHERE token_number = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, tokenNumber);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- UPDATED: Get Tokens with Status ---
    public static List<String[]> getTokensByDate(java.sql.Date date) {
        List<String[]> tokens = new ArrayList<>();
        String sql = "SELECT * FROM tokens WHERE booking_date = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDate(1, date);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tokens.add(new String[]{
                    rs.getString("token_number"), rs.getString("pid"), rs.getString("name"),
                    rs.getString("test_type"), rs.getString("venue"), rs.getString("scheduled_time"),
                    rs.getString("mobile"), rs.getString("place"), rs.getString("blood_group"),
                    rs.getString("prescription_file"), rs.getString("booking_date"),
                    rs.getString("status") // <-- ADDED STATUS (Index 11)
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return tokens;
    }

    // --- Report Methods (Unchanged) ---
    public static void addReport(String pid, String filename, long timestamp) {
        String sql = "INSERT INTO reports(pid, filename, upload_timestamp) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pid);
            pstmt.setString(2, filename);
            pstmt.setLong(3, timestamp);
            pstmt.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static List<String[]> getReports(String pid) {
        List<String[]> reports = new ArrayList<>();
        String sql = "SELECT filename, upload_timestamp FROM reports WHERE pid = ? ORDER BY upload_timestamp DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pid);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reports.add(new String[]{rs.getString("filename"), String.valueOf(rs.getLong("upload_timestamp"))});
            }
        } catch (Exception e) { e.printStackTrace(); }
        return reports;
    }
    
    public static List<Map<String, String>> getReports(String filterDate, String searchPid) {
        List<Map<String, String>> reports = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        
        String sql = "SELECT pid, filename, upload_timestamp FROM reports";
        
        if (filterDate != null && !filterDate.isEmpty()) {
            sql += " WHERE CAST(to_timestamp(upload_timestamp / 1000) AS DATE) = ?";
            params.add(java.sql.Date.valueOf(filterDate));
        }
        if (searchPid != null && !searchPid.isEmpty()) {
            sql += (params.isEmpty() ? " WHERE" : " AND") + " pid = ?";
            params.add(searchPid);
        }
        
        sql += " ORDER BY pid, upload_timestamp DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, String> report = new HashMap<>();
                report.put("pid", rs.getString("pid"));
                report.put("filename", rs.getString("filename"));
                report.put("timestamp", String.valueOf(rs.getLong("upload_timestamp")));
                reports.add(report);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return reports;
    }
}