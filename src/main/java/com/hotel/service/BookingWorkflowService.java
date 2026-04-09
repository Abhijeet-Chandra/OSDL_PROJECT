package com.hotel.service;

import com.hotel.dao.BillDao;
import com.hotel.dao.Database;
import com.hotel.model.Bill;
import com.hotel.model.BillLine;
import com.hotel.model.BookingStatus;
import com.hotel.model.RoomStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

/**
 * Booking workflow with real synchronization value:
 * prevents concurrent double-booking for the same room id.
 */
public class BookingWorkflowService {

    private final RoomLockRegistry locks = RoomLockRegistry.getInstance();
    private final BillDao billDao = new BillDao();

    public int bookRoomAndCreateBill(
            Integer existingGuestIdOrNull,
            String guestName,
            String guestPhone,
            int roomId,
            LocalDate checkIn,
            LocalDate checkOut,
            Bill bill,
            List<BillLine> lines
    ) throws SQLException {

        Object roomLock = locks.lockForRoom(roomId);

        // synchronized block: only one booking per room at a time.
        synchronized (roomLock) {
            try (Connection c = Database.getConnection()) {
                c.setAutoCommit(false);
                try {
                    assertRoomAvailable(c, roomId);

                    int guestId = existingGuestIdOrNull != null
                            ? existingGuestIdOrNull
                            : insertGuest(c, guestName, guestPhone);

                    insertBooking(c, guestId, roomId, checkIn, checkOut, BookingStatus.CHECKED_IN);
                    setRoomStatus(c, roomId, RoomStatus.OCCUPIED);

                    int billId = billDao.insertBillWithLines(c, bill, lines);
                    c.commit();
                    return billId;
                } catch (SQLException e) {
                    c.rollback();
                    throw e;
                } finally {
                    c.setAutoCommit(true);
                }
            }
        }
    }

    private static void assertRoomAvailable(Connection c, int roomId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT status FROM rooms WHERE id=?")) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Room not found.");
                }
                String status = rs.getString("status");
                if (!RoomStatus.AVAILABLE.name().equalsIgnoreCase(status)) {
                    throw new SQLException("Room is not available (current status: " + status + ").");
                }
            }
        }
    }

    private static int insertGuest(Connection c, String name, String phone) throws SQLException {
        String sql = "INSERT INTO guests(name, phone) VALUES (?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            if (phone == null || phone.isBlank()) {
                ps.setObject(2, null);
            } else {
                ps.setString(2, phone.trim());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static void insertBooking(Connection c, int guestId, int roomId, LocalDate in, LocalDate out, BookingStatus status) throws SQLException {
        String sql = """
                INSERT INTO bookings(guest_id, room_id, check_in_date, check_out_date, status)
                VALUES (?,?,?,?,?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, guestId);
            ps.setInt(2, roomId);
            ps.setString(3, in.toString());
            ps.setString(4, out.toString());
            ps.setString(5, status.name());
            ps.executeUpdate();
        }
    }

    private static void setRoomStatus(Connection c, int roomId, RoomStatus status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE rooms SET status=? WHERE id=?")) {
            ps.setString(1, status.name());
            ps.setInt(2, roomId);
            ps.executeUpdate();
        }
    }
}

