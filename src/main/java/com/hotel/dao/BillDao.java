package com.hotel.dao;

import com.hotel.model.Bill;
import com.hotel.model.BillLine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BillDao {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public int insertBillWithLines(Bill bill, List<BillLine> lines) throws SQLException {
        String insBill = """
                INSERT INTO bill(created_at, student_id, subtotal_cents, tax_cents, total_cents)
                VALUES (?,?,?,?,?)
                """;
        String insLine = """
                INSERT INTO bill_line(bill_id, menu_item_id, name_snapshot, unit_price_cents, qty, line_total_cents)
                VALUES (?,?,?,?,?,?)
                """;
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                int billId = insertBillWithLines(c, bill, lines);
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

    /**
     * Inserts a bill + its lines using an existing connection (caller manages transaction).
     */
    public int insertBillWithLines(Connection c, Bill bill, List<BillLine> lines) throws SQLException {
        String insBill = """
                INSERT INTO bill(created_at, student_id, subtotal_cents, tax_cents, total_cents)
                VALUES (?,?,?,?,?)
                """;
        String insLine = """
                INSERT INTO bill_line(bill_id, menu_item_id, name_snapshot, unit_price_cents, qty, line_total_cents)
                VALUES (?,?,?,?,?,?)
                """;

        int billId;
        try (PreparedStatement ps = c.prepareStatement(insBill, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bill.getCreatedAt().format(ISO));
            ps.setString(2, blankToNull(bill.getStudentId()));
            ps.setInt(3, bill.getSubtotalCents());
            ps.setInt(4, bill.getTaxCents());
            ps.setInt(5, bill.getTotalCents());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                billId = keys.getInt(1);
            }
        }

        try (PreparedStatement ps = c.prepareStatement(insLine)) {
            for (BillLine line : lines) {
                ps.setInt(1, billId);
                if (line.getMenuItemId() <= 0) {
                    ps.setObject(2, null);
                } else {
                    ps.setInt(2, line.getMenuItemId());
                }
                ps.setString(3, line.getName());
                ps.setInt(4, line.getUnitPriceCents());
                ps.setInt(5, line.getQty());
                ps.setInt(6, line.getLineTotalCents());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        return billId;
    }

    public List<Bill> findByDate(LocalDate date) throws SQLException {
        return findByDateAndGuestName(date, null);
    }

    /**
     * When guestName is non-null, only bills whose stored guest label ({@code student_id}) matches.
     */
    public List<Bill> findByDateAndGuestName(LocalDate date, String guestName) throws SQLException {
        String dayStart = date.atStartOfDay().format(ISO);
        String dayEnd = date.plusDays(1).atStartOfDay().format(ISO);
        boolean filterGuest = guestName != null && !guestName.isBlank();
        String sql = filterGuest ? """
                SELECT id, created_at, student_id, subtotal_cents, tax_cents, total_cents
                FROM bill
                WHERE created_at >= ? AND created_at < ?
                  AND student_id = ?
                ORDER BY created_at DESC
                """ : """
                SELECT id, created_at, student_id, subtotal_cents, tax_cents, total_cents
                FROM bill
                WHERE created_at >= ? AND created_at < ?
                ORDER BY created_at DESC
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dayStart);
            ps.setString(2, dayEnd);
            if (filterGuest) {
                ps.setString(3, guestName.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Bill> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapBill(rs));
                }
                return list;
            }
        }
    }

    public List<Bill> findRecent(int limit) throws SQLException {
        String sql = """
                SELECT id, created_at, student_id, subtotal_cents, tax_cents, total_cents
                FROM bill
                ORDER BY created_at DESC
                LIMIT ?
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Bill> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapBill(rs));
                }
                return list;
            }
        }
    }

    public List<BillLine> findLinesForBill(int billId) throws SQLException {
        String sql = """
                SELECT id, bill_id, menu_item_id, name_snapshot, unit_price_cents, qty, line_total_cents
                FROM bill_line WHERE bill_id = ? ORDER BY id
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                List<BillLine> list = new ArrayList<>();
                while (rs.next()) {
                    Integer mid = rs.getObject("menu_item_id") == null ? 0 : rs.getInt("menu_item_id");
                    BillLine line = new BillLine(
                            rs.getInt("id"),
                            mid,
                            rs.getString("name_snapshot"),
                            rs.getInt("unit_price_cents"),
                            rs.getInt("qty"),
                            rs.getInt("line_total_cents")
                    );
                    list.add(line);
                }
                return list;
            }
        }
    }

    public Totals todayTotals() throws SQLException {
        LocalDate today = LocalDate.now();
        String dayStart = today.atStartOfDay().format(ISO);
        String dayEnd = today.plusDays(1).atStartOfDay().format(ISO);
        String sql = """
                SELECT COUNT(*), COALESCE(SUM(total_cents),0)
                FROM bill
                WHERE created_at >= ? AND created_at < ?
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dayStart);
            ps.setString(2, dayEnd);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Totals(rs.getInt(1), rs.getInt(2));
            }
        }
    }

    public record DailyRevenue(java.time.LocalDate date, int revenueCents) {
    }

    public List<DailyRevenue> findDailyRevenueLastNDays(int days) throws SQLException {
        if (days <= 0) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);

        String dayStart = from.atStartOfDay().format(ISO);
        String dayEnd = today.plusDays(1).atStartOfDay().format(ISO);

        // Using substr(created_at,1,10) to extract yyyy-MM-dd part from ISO_LOCAL_DATE_TIME.
        String sql = """
                SELECT
                    substr(created_at, 1, 10) AS day,
                    COALESCE(SUM(total_cents), 0) AS revenue_cents
                FROM bill
                WHERE created_at >= ? AND created_at < ?
                GROUP BY day
                ORDER BY day ASC
                """;

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dayStart);
            ps.setString(2, dayEnd);

            try (ResultSet rs = ps.executeQuery()) {
                List<DailyRevenue> list = new ArrayList<>();
                while (rs.next()) {
                    LocalDate d = LocalDate.parse(rs.getString("day"));
                    int revenue = rs.getInt("revenue_cents");
                    list.add(new DailyRevenue(d, revenue));
                }
                return list;
            }
        }
    }

    private static Bill mapBill(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setId(rs.getInt("id"));
        b.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"), ISO));
        b.setStudentId(rs.getString("student_id"));
        b.setSubtotalCents(rs.getInt("subtotal_cents"));
        b.setTaxCents(rs.getInt("tax_cents"));
        b.setTotalCents(rs.getInt("total_cents"));
        return b;
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    public record Totals(int billCount, int revenueCents) {
    }
}
