
package chatapp;

import java.sql.*;
import java.util.Scanner;

public class ChatApp {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "ChatAppDB";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Yanka123@";
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Create database and tables if they don't exist
            setupDatabase(connection);
            
            // Now connect to the specific database
            try (Connection appConnection = DriverManager.getConnection(DB_URL + DB_NAME, DB_USER, DB_PASSWORD)) {
                while (true) {
                    System.out.println("\n1. Create Account\n2. Send Message\n3. View Messages\n4. Exit");
                    System.out.print("Choose an option: ");
                    int choice = scanner.nextInt();
                    scanner.nextLine();

                    switch (choice) {
                        case 1 -> createAccount(appConnection);
                        case 2 -> sendMessage(appConnection);
                        case 3 -> viewMessages(appConnection);
                        case 4 -> {
                            System.out.println("Exiting...");
                            return;
                        }
                        default -> System.out.println("Invalid choice!");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

private static void setupDatabase(Connection rootConnection) throws SQLException {
    try (Statement stmt = rootConnection.createStatement()) {
        // Create database if it doesn't exist
        stmt.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
        
        // Switch to the newly created database
        stmt.execute("USE " + DB_NAME);

        // Create Users table if it doesn't exist
        stmt.execute("CREATE TABLE IF NOT EXISTS Users (" +
                "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(100) NOT NULL, " +
                "phone_number VARCHAR(20) NOT NULL)");

        // Create Messages table if it doesn't exist
        stmt.execute("CREATE TABLE IF NOT EXISTS Messages (" +
                "message_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "sender_id INT NOT NULL, " +
                "receiver_id INT NOT NULL, " +
                "payload TEXT NOT NULL, " +
                "is_read BOOLEAN DEFAULT FALSE, " +
                "sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (sender_id) REFERENCES Users(user_id), " +
                "FOREIGN KEY (receiver_id) REFERENCES Users(user_id))");

        System.out.println("Database setup complete.");
    }
}


    private static void createAccount(Connection connection) throws SQLException {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        System.out.print("Enter South African phone number (+27xxxxxxxxx): ");
        String phoneNumber = scanner.nextLine();

        if (!phoneNumber.matches("\\+27\\d{9}")) {
            System.out.println("Invalid SA phone number format.");
            return;
        }

        String sql = "INSERT INTO Users (username, password, phone_number) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, phoneNumber);
            pstmt.executeUpdate();
            System.out.println("Account created successfully!");
        }
    }

    private static void sendMessage(Connection connection) throws SQLException {
        System.out.print("Enter sender username: ");
        String sender = scanner.nextLine();

        System.out.print("Enter receiver username: ");
        String receiver = scanner.nextLine();

        int senderId = getUserId(connection, sender);
        int receiverId = getUserId(connection, receiver);

        if (senderId == -1 || receiverId == -1) {
            System.out.println("Sender or receiver does not exist");
            return;
        }

        System.out.print("Enter message: ");
        String payload = scanner.nextLine();

        String sql = "INSERT INTO Messages (sender_id, receiver_id, payload) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            pstmt.setString(3, payload);
            pstmt.executeUpdate();
            System.out.println("Message sent successfully!");
        }
    }

    private static void viewMessages(Connection connection) throws SQLException {
        System.out.print("Enter your username to view messages: ");
        String username = scanner.nextLine();
        int userId = getUserId(connection, username);

        if (userId == -1) {
            System.out.println("User not found!");
            return;
        }

        String sql = "SELECT m.message_id, u.username as sender, m.payload, m.is_read " +
                     "FROM Messages m JOIN Users u ON m.sender_id = u.user_id " +
                     "WHERE m.receiver_id = ? ORDER BY m.sent_at DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            boolean hasMessages = false;
            while (rs.next()) {
                hasMessages = true;
                System.out.println("\nFrom: " + rs.getString("sender"));
                System.out.println("Message: " + rs.getString("payload"));
                System.out.println("Status: " + (rs.getBoolean("is_read") ? "Read" : "Unread"));
                
                markAsRead(connection, rs.getInt("message_id"));
            }

            if (!hasMessages) {
                System.out.println("No messages found.");
            }
        }
    }

    private static int getUserId(Connection connection, String username) throws SQLException {
        String sql = "SELECT user_id FROM Users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("user_id") : -1;
        }
    }

    private static void markAsRead(Connection connection, int messageId) throws SQLException {
        String sql = "UPDATE Messages SET is_read = TRUE WHERE message_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, messageId);
            pstmt.executeUpdate();
        }
    }
}