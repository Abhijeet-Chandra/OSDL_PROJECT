package com.hotel.dao;

import com.hotel.model.Room;
import com.hotel.model.RoomStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomDao {

    public record RoomCounts(int available, int occupied, int maintenance) {
    }

    public RoomCounts roomCounts() throws SQLException {
        String sql = """
                SELECT
                    SUM(CASE WHEN status = ? THEN 1 ELSE 0 END) AS available_cnt,
                    SUM(CASE WHEN status = ? THEN 1 ELSE 0 END) AS occupied_cnt,
                    SUM(CASE WHEN status = ? THEN 1 ELSE 0 END) AS maintenance_cnt
                FROM rooms
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, RoomStatus.AVAILABLE.name());
            ps.setString(2, RoomStatus.OCCUPIED.name());
            ps.setString(3, RoomStatus.MAINTENANCE.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new RoomCounts(rs.getInt("available_cnt"), rs.getInt("occupied_cnt"), rs.getInt("maintenance_cnt"));
            }
        }
    }

    public List<Room> findAll() throws SQLException {
        String sql = "SELECT id, room_number, room_type, capacity, daily_rate_cents, status FROM rooms ORDER BY room_number";
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Room> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    }

    public List<Room> findAvailableRooms() throws SQLException {
        String sql = "SELECT id, room_number, room_type, capacity, daily_rate_cents, status FROM rooms WHERE status = ? ORDER BY room_number";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, RoomStatus.AVAILABLE.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<Room> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public Optional<Room> findById(int id) throws SQLException {
        String sql = "SELECT id, room_number, room_type, capacity, daily_rate_cents, status FROM rooms WHERE id = ?";
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

    public int insert(Room room) throws SQLException {
        String sql = "INSERT INTO rooms(room_number, room_type, capacity, daily_rate_cents, status) VALUES (?,?,?,?,?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setInt(3, room.getCapacity());
            ps.setInt(4, room.getDailyRateCents());
            ps.setString(5, room.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void update(Room room) throws SQLException {
        String sql = """
                UPDATE rooms
                SET room_number=?, room_type=?, capacity=?, daily_rate_cents=?, status=?
                WHERE id=?
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, room.getRoomNumber());
            ps.setString(2, room.getRoomType());
            ps.setInt(3, room.getCapacity());
            ps.setInt(4, room.getDailyRateCents());
            ps.setString(5, room.getStatus().name());
            ps.setInt(6, room.getId());
            ps.executeUpdate();
        }
    }

    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM rooms WHERE id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void setStatus(int roomId, RoomStatus status) throws SQLException {
        String sql = "UPDATE rooms SET status=? WHERE id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, roomId);
            ps.executeUpdate();
        }
    }

    private static Room map(ResultSet rs) throws SQLException {
        RoomStatus status;
        try {
            status = RoomStatus.valueOf(rs.getString("status"));
        } catch (Exception ignored) {
            status = RoomStatus.AVAILABLE;
        }

        return new Room(
                rs.getInt("id"),
                rs.getString("room_number"),
                rs.getString("room_type"),
                rs.getInt("capacity"),
                rs.getInt("daily_rate_cents"),
                status
        );
    }
}

