package com.hotel.dao;

import com.hotel.model.MenuItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuItemDao {

    public List<MenuItem> findAll() throws SQLException {
        String sql = "SELECT id, name, category, price_cents, active FROM menu_item ORDER BY category, name";
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<MenuItem> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    }

    public List<MenuItem> findActive() throws SQLException {
        String sql = "SELECT id, name, category, price_cents, active FROM menu_item WHERE active = 1 ORDER BY category, name";
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<MenuItem> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    }

    public Optional<MenuItem> findById(int id) throws SQLException {
        String sql = "SELECT id, name, category, price_cents, active FROM menu_item WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public int insert(MenuItem m) throws SQLException {
        String sql = "INSERT INTO menu_item(name, category, price_cents, active) VALUES (?,?,?,?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCategory());
            ps.setInt(3, m.getPriceCents());
            ps.setInt(4, m.isActive() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void update(MenuItem m) throws SQLException {
        String sql = "UPDATE menu_item SET name=?, category=?, price_cents=?, active=? WHERE id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getCategory());
            ps.setInt(3, m.getPriceCents());
            ps.setInt(4, m.isActive() ? 1 : 0);
            ps.setInt(5, m.getId());
            ps.executeUpdate();
        }
    }

    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM menu_item WHERE id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static MenuItem map(ResultSet rs) throws SQLException {
        return new MenuItem(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getInt("price_cents"),
                rs.getInt("active") == 1
        );
    }
}
