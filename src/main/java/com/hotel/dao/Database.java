package com.hotel.dao;

import com.hotel.model.UserRole;
import com.hotel.util.PasswordHasher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String JDBC_URL;

    static {
        try {
            Path dir = Path.of(System.getProperty("user.dir"), "data");
            Files.createDirectories(dir);
            Path hotelDb = dir.resolve("hotel.db");
            Path legacyDb = dir.resolve("cafe" + "teria.db");

            // Compatibility: migrate legacy DB filename if present.
            if (!Files.exists(hotelDb) && Files.exists(legacyDb)) {
                try {
                    Files.move(legacyDb, hotelDb);
                } catch (Exception moveEx) {
                    // Fallback: copy instead of move.
                    Files.copy(legacyDb, hotelDb);
                }
            }

            JDBC_URL = "jdbc:sqlite:" + hotelDb.toAbsolutePath();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(JDBC_URL);
        try (var st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        c.setAutoCommit(true);
        return c;
    }

    public static void initialize() {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS menu_item (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        price_cents INTEGER NOT NULL,
                        active INTEGER NOT NULL DEFAULT 1
                    )
                    """);

            // Hotel domain tables
            st.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        room_number TEXT NOT NULL UNIQUE,
                        room_type TEXT NOT NULL,
                        capacity INTEGER NOT NULL,
                        daily_rate_cents INTEGER NOT NULL,
                        status TEXT NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS guests (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        phone TEXT
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS bookings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        guest_id INTEGER NOT NULL,
                        room_id INTEGER NOT NULL,
                        check_in_date TEXT NOT NULL,
                        check_out_date TEXT NOT NULL,
                        status TEXT NOT NULL,
                        FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE CASCADE,
                        FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        role TEXT NOT NULL,
                        guest_id INTEGER,
                        FOREIGN KEY (guest_id) REFERENCES guests(id) ON DELETE SET NULL
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS bill (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        created_at TEXT NOT NULL,
                        student_id TEXT,
                        subtotal_cents INTEGER NOT NULL,
                        tax_cents INTEGER NOT NULL,
                        total_cents INTEGER NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS bill_line (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        bill_id INTEGER NOT NULL,
                        menu_item_id INTEGER,
                        name_snapshot TEXT NOT NULL,
                        unit_price_cents INTEGER NOT NULL,
                        qty INTEGER NOT NULL,
                        line_total_cents INTEGER NOT NULL,
                        FOREIGN KEY (bill_id) REFERENCES bill(id) ON DELETE CASCADE
                    )
                    """);
            seedIfEmpty(st);
            seedRoomsIfEmpty(st);
            seedUsersIfEmpty(c);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database", e);
        }
    }

    private static void seedUsersIfEmpty(Connection c) throws SQLException {
        try (Statement chk = c.createStatement();
             ResultSet rs = chk.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }

        int guestId;
        try (PreparedStatement pg = c.prepareStatement(
                "INSERT INTO guests(name, phone) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
            pg.setString(1, "Demo Guest");
            pg.setString(2, "9000000001");
            pg.executeUpdate();
            try (ResultSet keys = pg.getGeneratedKeys()) {
                keys.next();
                guestId = keys.getInt(1);
            }
        }

        String insUser = """
                INSERT INTO users(username, password_hash, role, guest_id)
                VALUES (?,?,?,?)
                """;
        try (PreparedStatement pu = c.prepareStatement(insUser)) {
            pu.setString(1, "admin");
            pu.setString(2, PasswordHasher.sha256Hex("admin123"));
            pu.setString(3, UserRole.ADMIN.name());
            pu.setObject(4, null);
            pu.executeUpdate();

            pu.setString(1, "guest");
            pu.setString(2, PasswordHasher.sha256Hex("guest123"));
            pu.setString(3, UserRole.CUSTOMER.name());
            pu.setInt(4, guestId);
            pu.executeUpdate();
        }
    }

    private static void seedIfEmpty(Statement st) throws SQLException {
        var rs = st.executeQuery("SELECT COUNT(*) FROM menu_item");
        rs.next();
        if (rs.getInt(1) > 0) {
            return;
        }
        st.executeUpdate("""
                INSERT INTO menu_item(name, category, price_cents, active) VALUES
                ('Masala Dosa', 'Meals', 8000, 1),
                ('Idli (2)', 'Meals', 4000, 1),
                ('Samosa', 'Snacks', 2000, 1),
                ('Tea', 'Beverages', 1500, 1),
                ('Coffee', 'Beverages', 2500, 1)
                """);
    }

    private static void seedRoomsIfEmpty(Statement st) throws SQLException {
        var rs = st.executeQuery("SELECT COUNT(*) FROM rooms");
        rs.next();
        if (rs.getInt(1) > 0) {
            return;
        }
        st.executeUpdate("""
                INSERT INTO rooms(room_number, room_type, capacity, daily_rate_cents, status) VALUES
                ('101', 'Single', 1, 350000, 'AVAILABLE'),
                ('102', 'Single', 1, 350000, 'AVAILABLE'),
                ('201', 'Double', 2, 520000, 'AVAILABLE'),
                ('202', 'Double', 2, 520000, 'AVAILABLE'),
                ('301', 'Family', 4, 850000, 'AVAILABLE')
                """);
    }
}
