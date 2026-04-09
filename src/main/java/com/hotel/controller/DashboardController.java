package com.hotel.controller;

import com.hotel.auth.UserSession;
import com.hotel.dao.BillDao;
import com.hotel.dao.RoomDao;
import com.hotel.model.Money;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable, UserSessionAware {

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label billsTodayLabel;

    @FXML
    private Label revenueTodayLabel;

    @FXML
    private Label availableRoomsLabel;

    @FXML
    private PieChart roomPieChart;

    @FXML
    private BarChart<String, Number> revenueBarChart;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private UserSession userSession;

    private final BillDao billDao = new BillDao();
    private final RoomDao roomDao = new RoomDao();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @Override
    public void setUserSession(UserSession session) {
        this.userSession = session;
        refresh();
    }

    private void refresh() {
        if (pageTitleLabel != null) {
            pageTitleLabel.setText("Admin dashboard");
        }
        try {
            BillDao.Totals t = billDao.todayTotals();
            billsTodayLabel.setText(String.valueOf(t.billCount()));
            revenueTodayLabel.setText(Money.format(t.revenueCents()));

            var counts = roomDao.roomCounts();
            availableRoomsLabel.setText(String.valueOf(counts.available()));

            roomPieChart.getData().clear();
            roomPieChart.getData().add(new PieChart.Data("Available", counts.available()));
            roomPieChart.getData().add(new PieChart.Data("Occupied", counts.occupied()));
            roomPieChart.getData().add(new PieChart.Data("Maintenance", counts.maintenance()));

            loadRevenueChart();
        } catch (SQLException e) {
            billsTodayLabel.setText("—");
            revenueTodayLabel.setText("Error");
            if (availableRoomsLabel != null) {
                availableRoomsLabel.setText("—");
            }
            if (roomPieChart != null) {
                roomPieChart.getData().clear();
            }
            if (revenueBarChart != null) {
                revenueBarChart.getData().clear();
            }
        }
    }

    private void loadRevenueChart() throws SQLException {
        List<BillDao.DailyRevenue> daily = billDao.findDailyRevenueLastNDays(7);
        revenueBarChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (BillDao.DailyRevenue d : daily) {
            double revenueRs = d.revenueCents() / 100.0;
            series.getData().add(new XYChart.Data<>(d.date().format(DAY_FMT), revenueRs));
        }
        revenueBarChart.getData().add(series);
    }
}
