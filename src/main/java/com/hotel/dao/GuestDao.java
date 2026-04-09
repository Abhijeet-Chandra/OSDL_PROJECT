package com.hotel.dao;

import com.hotel.model.Guest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GuestDao {

    public Guest findById(int id) throws SQLException {
        String sql = "SELECT id, name, phone FROM guests WHERE id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Guest(rs.getInt("id"), rs.getString("name"), rs.getString("phone"));
            }
        }
    }

    public int insertGuest(Guest guest) throws SQLException {
        String sql = "INSERT INTO guests(name, phone) VALUES (?,?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, guest.getName());
            ps.setString(2, guest.getPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }
}

