package com.hotel.dao;

import com.hotel.auth.UserSession;
import com.hotel.model.UserRole;
import com.hotel.util.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UserDao {

    public Optional<UserSession> authenticate(String username, String password) throws SQLException {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        String sql = """
                SELECT id, username, password_hash, role, guest_id
                FROM users
                WHERE lower(username) = lower(?)
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String storedHash = rs.getString("password_hash");
                String attemptHash = PasswordHasher.sha256Hex(password == null ? "" : password);
                if (!storedHash.equalsIgnoreCase(attemptHash)) {
                    return Optional.empty();
                }
                int id = rs.getInt("id");
                String u = rs.getString("username");
                UserRole role = UserRole.valueOf(rs.getString("role"));
                int gid = rs.getInt("guest_id");
                boolean hasGuest = !rs.wasNull();
                return Optional.of(new UserSession(id, u, role, hasGuest ? gid : null));
            }
        }
    }

    public int createCustomerUser(String username, String plainPassword, int guestId) throws SQLException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username required.");
        }
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password required.");
        }
        if (guestId <= 0) {
            throw new IllegalArgumentException("Guest profile required.");
        }

        // Pre-check for friendly error (unique constraint is still the source of truth).
        try (Connection c = Database.getConnection();
             PreparedStatement chk = c.prepareStatement("SELECT 1 FROM users WHERE lower(username) = lower(?)")) {
            chk.setString(1, username.trim());
            try (ResultSet rs = chk.executeQuery()) {
                if (rs.next()) {
                    throw new IllegalStateException("Username already exists.");
                }
            }
        }

        String sql = """
                INSERT INTO users(username, password_hash, role, guest_id)
                VALUES (?,?,?,?)
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username.trim());
            ps.setString(2, PasswordHasher.sha256Hex(plainPassword));
            ps.setString(3, UserRole.CUSTOMER.name());
            ps.setInt(4, guestId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }
}
