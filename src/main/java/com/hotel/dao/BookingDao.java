package com.hotel.dao;

import com.hotel.model.BookingInfo;
import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookingDao {
    public int insertBooking(int guestId, int roomId, LocalDate checkIn, LocalDate checkOut, BookingStatus status) throws SQLException {
        String sql = """
                INSERT INTO bookings(guest_id, room_id, check_in_date, check_out_date, status)
                VALUES (?,?,?,?,?)
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, guestId);
            ps.setInt(2, roomId);
            ps.setString(3, checkIn.toString());
            ps.setString(4, checkOut.toString());
            ps.setString(5, status.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public Booking mapFromRow(ResultSet rs) throws SQLException {
        return new Booking(
                rs.getInt("id"),
                rs.getInt("guest_id"),
                rs.getInt("room_id"),
                LocalDate.parse(rs.getString("check_in_date")),
                LocalDate.parse(rs.getString("check_out_date")),
                BookingStatus.valueOf(rs.getString("status"))
        );
    }

    public List<BookingInfo> findBookingsForGuest(int guestId) throws SQLException {
        String sql = """
                SELECT
                    b.id,
                    g.name AS guest_name,
                    r.id AS room_id,
                    r.room_number,
                    b.check_in_date,
                    b.check_out_date,
                    b.status
                FROM bookings b
                JOIN guests g ON g.id = b.guest_id
                JOIN rooms r ON r.id = b.room_id
                WHERE b.guest_id = ?
                ORDER BY b.check_in_date DESC
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            try (ResultSet rs = ps.executeQuery()) {
                List<BookingInfo> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new BookingInfo(
                            rs.getInt("id"),
                            rs.getString("guest_name"),
                            rs.getInt("room_id"),
                            rs.getString("room_number"),
                            LocalDate.parse(rs.getString("check_in_date")),
                            LocalDate.parse(rs.getString("check_out_date")),
                            BookingStatus.valueOf(rs.getString("status"))
                    ));
                }
                return list;
            }
        }
    }

    public List<BookingInfo> findAllBookings() throws SQLException {
        String sql = """
                SELECT
                    b.id,
                    g.name AS guest_name,
                    r.id AS room_id,
                    r.room_number,
                    b.check_in_date,
                    b.check_out_date,
                    b.status
                FROM bookings b
                JOIN guests g ON g.id = b.guest_id
                JOIN rooms r ON r.id = b.room_id
                ORDER BY b.check_in_date DESC
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<BookingInfo> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new BookingInfo(
                        rs.getInt("id"),
                        rs.getString("guest_name"),
                        rs.getInt("room_id"),
                        rs.getString("room_number"),
                        LocalDate.parse(rs.getString("check_in_date")),
                        LocalDate.parse(rs.getString("check_out_date")),
                        BookingStatus.valueOf(rs.getString("status"))
                ));
            }
            return list;
        }
    }

    public void setBookingStatus(int bookingId, BookingStatus status) throws SQLException {
        String sql = "UPDATE bookings SET status=? WHERE id=?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }
}

