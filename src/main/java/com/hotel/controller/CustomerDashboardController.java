package com.hotel.controller;

import com.hotel.auth.UserSession;
import com.hotel.dao.BookingDao;
import com.hotel.dao.GuestDao;
import com.hotel.model.BookingInfo;
import com.hotel.model.BookingStatus;
import com.hotel.model.Guest;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class CustomerDashboardController implements Initializable, UserSessionAware {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label profileLabel;

    @FXML
    private Label activeStaysLabel;

    @FXML
    private Label nextStayLabel;

    private UserSession session;
    private MainController mainController;

    private final BookingDao bookingDao = new BookingDao();
    private final GuestDao guestDao = new GuestDao();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void setUserSession(UserSession session) {
        this.session = session;
        refresh();
    }

    private void refresh() {
        if (session == null || welcomeLabel == null) {
            return;
        }
        welcomeLabel.setText("Welcome, " + session.username());
        if (session.linkedGuestId() == null) {
            profileLabel.setText("No guest profile linked to this account.");
            activeStaysLabel.setText("—");
            nextStayLabel.setText("Book a stay from the Book room tab when available.");
            return;
        }
        try {
            Guest g = guestDao.findById(session.linkedGuestId());
            String name = g != null ? g.getName() : "Guest";
            String phone = g != null && g.getPhone() != null ? g.getPhone() : "—";
            profileLabel.setText(name + " · " + phone);

            List<BookingInfo> mine = bookingDao.findBookingsForGuest(session.linkedGuestId());
            long active = mine.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN
                            || b.getStatus() == BookingStatus.RESERVED)
                    .count();
            activeStaysLabel.setText(String.valueOf(active));

            nextStayLabel.setText(summarizeNext(mine));
        } catch (SQLException e) {
            profileLabel.setText("Could not load profile.");
            activeStaysLabel.setText("—");
            nextStayLabel.setText(e.getMessage());
        }
    }

    private static String summarizeNext(List<BookingInfo> mine) {
        BookingInfo next = mine.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED
                        && b.getStatus() != BookingStatus.CHECKED_OUT)
                .findFirst()
                .orElse(null);
        if (next == null) {
            return "No upcoming stay on file. Use Book room to reserve.";
        }
        return "Room " + next.getRoomNumber() + " · " + next.getStatus()
                + " · " + next.getCheckInDate().format(DATE_FMT) + " → "
                + next.getCheckOutDate().format(DATE_FMT);
    }

    @FXML
    private void onBookRoom() {
        if (mainController != null) {
            mainController.showBilling();
        }
    }

    @FXML
    private void onMyBookings() {
        if (mainController != null) {
            mainController.showBookings();
        }
    }

    @FXML
    private void onMyBills() {
        if (mainController != null) {
            mainController.showHistory();
        }
    }
}
