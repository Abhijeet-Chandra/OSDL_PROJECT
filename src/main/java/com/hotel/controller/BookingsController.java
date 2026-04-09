package com.hotel.controller;

import com.hotel.auth.UserSession;
import com.hotel.dao.BookingDao;
import com.hotel.dao.RoomDao;
import com.hotel.model.BookingInfo;
import com.hotel.model.BookingStatus;
import com.hotel.model.RoomStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import javafx.concurrent.Task;

public class BookingsController implements Initializable, UserSessionAware {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    private Label pageTitleLabel;

    @FXML
    private TableView<BookingInfo> bookingTable;

    @FXML
    private TableColumn<BookingInfo, Integer> colBookingId;

    @FXML
    private TableColumn<BookingInfo, String> colGuest;

    @FXML
    private TableColumn<BookingInfo, String> colRoom;

    @FXML
    private TableColumn<BookingInfo, String> colCheckIn;

    @FXML
    private TableColumn<BookingInfo, String> colCheckOut;

    @FXML
    private TableColumn<BookingInfo, String> colStatus;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private Button checkOutButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button refreshButton;

    private final BookingDao bookingDao = new BookingDao();
    private final RoomDao roomDao = new RoomDao();

    private final ObservableList<BookingInfo> bookings = FXCollections.observableArrayList();

    private UserSession userSession;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        bookingTable.setItems(bookings);

        colBookingId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colCheckIn.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getCheckInDate().format(DATE_FMT)));
        colCheckOut.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getCheckOutDate().format(DATE_FMT)));
        colStatus.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatus().name()));

        statusFilter.getItems().add("ALL");
        for (BookingStatus s : BookingStatus.values()) {
            statusFilter.getItems().add(s.name());
        }
        statusFilter.getSelectionModel().select(BookingStatus.CHECKED_IN.name());

        statusFilter.valueProperty().addListener((obs, o, n) -> refresh());

        bookingTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateButtons(n));
        updateButtons(null);

        // When buttons are clicked in FXML, keep them styled but enable/disable dynamically.
        checkOutButton.setDisable(true);
        cancelButton.setDisable(true);
        refreshButton.setDisable(false);
    }

    @Override
    public void setUserSession(UserSession session) {
        this.userSession = session;
        boolean customer = session != null && session.isCustomer() && session.linkedGuestId() != null;
        if (pageTitleLabel != null) {
            pageTitleLabel.setText(customer ? "My bookings" : "Bookings");
        }
        if (customer) {
            statusFilter.getSelectionModel().select("ALL");
            statusFilter.setDisable(true);
            checkOutButton.setVisible(false);
            checkOutButton.setManaged(false);
            cancelButton.setVisible(false);
            cancelButton.setManaged(false);
            colGuest.setVisible(false);
            colGuest.setMaxWidth(0);
        } else {
            statusFilter.setDisable(false);
            checkOutButton.setVisible(true);
            checkOutButton.setManaged(true);
            cancelButton.setVisible(true);
            cancelButton.setManaged(true);
            colGuest.setVisible(true);
            colGuest.setPrefWidth(220.0);
        }
        refresh();
    }

    private void updateButtons(BookingInfo selected) {
        if (selected == null) {
            checkOutButton.setDisable(true);
            cancelButton.setDisable(true);
            return;
        }
        boolean checkedIn = selected.getStatus() == BookingStatus.CHECKED_IN;
        checkOutButton.setDisable(!checkedIn);
        cancelButton.setDisable(!checkedIn);
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void refresh() {
        try {
            List<BookingInfo> list;
            if (userSession != null && userSession.isCustomer() && userSession.linkedGuestId() != null) {
                list = bookingDao.findBookingsForGuest(userSession.linkedGuestId());
            } else {
                list = bookingDao.findAllBookings();
            }
            String filter = statusFilter.getValue();
            bookings.setAll("ALL".equals(filter) ? list : list.stream().filter(b -> b.getStatus().name().equals(filter)).toList());
        } catch (SQLException e) {
            bookings.clear();
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onCheckOut() {
        BookingInfo selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Select a booking.");
            return;
        }
        if (selected.getStatus() != BookingStatus.CHECKED_IN) {
            alert(Alert.AlertType.WARNING, "Only CHECKED_IN bookings can be checked out.");
            return;
        }

        Task<Void> t = new Task<>() {
            @Override
            protected Void call() throws Exception {
                bookingDao.setBookingStatus(selected.getId(), BookingStatus.CHECKED_OUT);
                roomDao.setStatus(selected.getRoomId(), RoomStatus.AVAILABLE);
                return null;
            }
        };

        t.setOnSucceeded(e -> {
            refresh();
            updateButtons(bookingTable.getSelectionModel().getSelectedItem());
            alert(Alert.AlertType.INFORMATION, "Checked out room " + selected.getRoomNumber());
        });

        t.setOnFailed(e -> alert(Alert.AlertType.ERROR, String.valueOf(t.getException().getMessage())));

        Thread th = new Thread(t, "booking-checkout-thread");
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void onCancelBooking() {
        BookingInfo selected = bookingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Select a booking.");
            return;
        }
        if (selected.getStatus() != BookingStatus.CHECKED_IN) {
            alert(Alert.AlertType.WARNING, "Only active bookings can be cancelled in this demo.");
            return;
        }

        Task<Void> t = new Task<>() {
            @Override
            protected Void call() throws Exception {
                bookingDao.setBookingStatus(selected.getId(), BookingStatus.CANCELLED);
                roomDao.setStatus(selected.getRoomId(), RoomStatus.AVAILABLE);
                return null;
            }
        };

        t.setOnSucceeded(e -> {
            refresh();
            updateButtons(bookingTable.getSelectionModel().getSelectedItem());
            alert(Alert.AlertType.INFORMATION, "Booking cancelled.");
        });

        t.setOnFailed(e -> alert(Alert.AlertType.ERROR, String.valueOf(t.getException().getMessage())));

        Thread th = new Thread(t, "booking-cancel-thread");
        th.setDaemon(true);
        th.start();
    }

    private static void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}

