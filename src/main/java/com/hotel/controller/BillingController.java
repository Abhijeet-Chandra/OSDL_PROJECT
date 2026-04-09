package com.hotel.controller;

import com.hotel.auth.UserSession;
import com.hotel.dao.GuestDao;
import com.hotel.dao.MenuItemDao;
import com.hotel.dao.RoomDao;
import com.hotel.model.Bill;
import com.hotel.model.BillLine;
import com.hotel.model.Guest;
import com.hotel.model.Room;
import com.hotel.model.MenuItem;
import com.hotel.model.Money;
import com.hotel.service.BillingCalculator;
import com.hotel.service.BookingWorkflowService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.concurrent.Task;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class BillingController implements Initializable, UserSessionAware {

    @FXML
    private ComboBox<MenuItem> itemCombo;

    @FXML
    private Spinner<Integer> qtySpinner;

    @FXML
    private TableView<BillLine> cartTable;

    @FXML
    private TableColumn<BillLine, String> colLineName;

    @FXML
    private TableColumn<BillLine, String> colUnit;

    @FXML
    private TableColumn<BillLine, Integer> colQty;

    @FXML
    private TableColumn<BillLine, String> colLineTotal;

    @FXML
    private TextField guestNameField;

    @FXML
    private TextField guestPhoneField;

    @FXML
    private ComboBox<Room> roomCombo;

    @FXML
    private DatePicker checkInDatePicker;

    @FXML
    private DatePicker checkOutDatePicker;

    @FXML
    private Label nightsLabel;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label taxLabel;

    @FXML
    private Label totalLabel;

    private final ObservableList<BillLine> cart = FXCollections.observableArrayList();
    private final MenuItemDao menuItemDao = new MenuItemDao();
    private final RoomDao roomDao = new RoomDao();
    private final GuestDao guestDao = new GuestDao();
    private final BookingWorkflowService bookingWorkflowService = new BookingWorkflowService();

    private UserSession userSession;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        cartTable.setItems(cart);

        colLineName.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getName()));
        colUnit.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(Money.format(cd.getValue().getUnitPriceCents())));
        colQty.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleIntegerProperty(cd.getValue().getQty()).asObject());
        colLineTotal.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(Money.format(cd.getValue().getLineTotalCents())));

        checkInDatePicker.setValue(LocalDate.now());
        checkOutDatePicker.setValue(LocalDate.now().plusDays(1));

        refreshItems();
        refreshRooms();

        // Keep totals up-to-date with selected booking details + cart extras.
        roomCombo.valueProperty().addListener((obs, o, n) -> recalcTotals());
        checkInDatePicker.valueProperty().addListener((obs, o, n) -> recalcTotals());
        checkOutDatePicker.valueProperty().addListener((obs, o, n) -> recalcTotals());

        recalcTotals();
    }

    @Override
    public void setUserSession(UserSession session) {
        this.userSession = session;
        if (session != null && session.isCustomer() && session.linkedGuestId() != null) {
            try {
                Guest g = guestDao.findById(session.linkedGuestId());
                if (g != null) {
                    guestNameField.setText(g.getName());
                    guestPhoneField.setText(g.getPhone() != null ? g.getPhone() : "");
                    guestNameField.setDisable(true);
                    guestPhoneField.setDisable(true);
                    return;
                }
            } catch (SQLException e) {
                alert(Alert.AlertType.ERROR, e.getMessage());
            }
        }
        guestNameField.setDisable(false);
        guestPhoneField.setDisable(false);
    }

    private void refreshItems() {
        try {
            List<MenuItem> items = menuItemDao.findActive();
            itemCombo.setItems(FXCollections.observableArrayList(items));
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    private void refreshRooms() {
        try {
            List<Room> rooms = roomDao.findAvailableRooms();
            roomCombo.setItems(FXCollections.observableArrayList(rooms));
            roomCombo.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onAddLine() {
        MenuItem m = itemCombo.getSelectionModel().getSelectedItem();
        if (m == null) {
            alert(Alert.AlertType.WARNING, "Select a menu item.");
            return;
        }
        int qty = qtySpinner.getValue() != null ? qtySpinner.getValue() : 1;
        if (qty < 1) {
            qty = 1;
        }

        for (BillLine line : cart) {
            if (line.getMenuItemId() == m.getId()) {
                line.setQty(line.getQty() + qty);
                line.recomputeLineTotal();
                cartTable.refresh();
                recalcTotals();
                return;
            }
        }

        BillLine line = new BillLine(null, m.getId(), m.getName(), m.getPriceCents(), qty, m.getPriceCents() * qty);
        cart.add(line);
        recalcTotals();
    }

    @FXML
    private void onClearCart() {
        cart.clear();
        // Keep booking inputs so the user can quickly add multiple bills/extras.
        recalcTotals();
    }

    @FXML
    private void onSaveBill() {
        if (userSession != null && !userSession.isCustomer()) {
            alert(Alert.AlertType.INFORMATION,
                    "Booking is available only for customers/guests.\nAdmin accounts are for managing rooms and services.");
            return;
        }
        String guestName = guestNameField.getText() == null ? "" : guestNameField.getText().trim();
        if (guestName.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Enter guest name.");
            return;
        }
        String guestPhone = guestPhoneField.getText();

        Room room = roomCombo.getSelectionModel().getSelectedItem();
        if (room == null) {
            alert(Alert.AlertType.WARNING, "Select a room.");
            return;
        }

        LocalDate in = checkInDatePicker.getValue();
        LocalDate out = checkOutDatePicker.getValue();
        if (in == null || out == null) {
            alert(Alert.AlertType.WARNING, "Select check-in and check-out dates.");
            return;
        }

        long nightsLong = ChronoUnit.DAYS.between(in, out);
        if (nightsLong < 1) {
            alert(Alert.AlertType.WARNING, "Check-out must be after check-in.");
            return;
        }
        int nights = (int) nightsLong;

        // Capture mutable UI state before entering background thread.
        List<BillLine> extrasSnapshot = List.copyOf(cart);
        final int[] totalHolderCents = new int[1];

        Task<Integer> saveTask = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                // Build room-stay line item (persisted into bill_lines with service id = NULL).
                BillLine roomStay = new BillLine(null, 0,
                        "Room stay (" + nights + " night" + (nights == 1 ? "" : "s") + ")",
                        room.getDailyRateCents(),
                        nights,
                        room.getDailyRateCents() * nights);

                List<BillLine> lines = new ArrayList<>();
                lines.add(roomStay);
                lines.addAll(extrasSnapshot);

                int sub = BillingCalculator.subtotalCents(lines);
                int tax = BillingCalculator.taxCents(sub);
                int total = BillingCalculator.totalCents(sub, tax);
                totalHolderCents[0] = total;

                Integer existingGuestId = (userSession != null && userSession.isCustomer())
                        ? userSession.linkedGuestId()
                        : null;

                Bill bill = new Bill();
                bill.setCreatedAt(LocalDateTime.now());
                bill.setStudentId(guestName); // column name kept from template; semantics = guest
                bill.setSubtotalCents(sub);
                bill.setTaxCents(tax);
                bill.setTotalCents(total);

                return bookingWorkflowService.bookRoomAndCreateBill(
                        existingGuestId,
                        guestName,
                        guestPhone,
                        room.getId(),
                        in,
                        out,
                        bill,
                        lines
                );
            }
        };

        saveTask.setOnSucceeded(e -> {
            int billId = saveTask.getValue();
            alert(Alert.AlertType.INFORMATION, "Bill #" + billId + " saved. Total: " + Money.format(totalHolderCents[0]));
            onClearCart();
            refreshRooms();
        });

        saveTask.setOnFailed(e -> {
            String msg = saveTask.getException() == null ? "Unknown error" : saveTask.getException().getMessage();
            alert(Alert.AlertType.ERROR, msg);
        });

        Thread t = new Thread(saveTask, "save-bill-thread");
        t.setDaemon(true);
        t.start();
    }

    private List<BillLine> linesFromCartPreview() {
        List<BillLine> lines = new ArrayList<>();
        BillLine roomStay = computeRoomStayLineOrNull();
        if (roomStay != null) {
            lines.add(roomStay);
        }
        lines.addAll(cart);
        return lines;
    }

    private BillLine computeRoomStayLineOrNull() {
        Room room = roomCombo.getSelectionModel().getSelectedItem();
        LocalDate in = checkInDatePicker.getValue();
        LocalDate out = checkOutDatePicker.getValue();
        if (room == null || in == null || out == null) {
            return null;
        }
        long nightsLong = ChronoUnit.DAYS.between(in, out);
        if (nightsLong < 1) {
            return null;
        }
        int nights = (int) nightsLong;
        return new BillLine(null, 0,
                "Room stay (" + nights + " night" + (nights == 1 ? "" : "s") + ")",
                room.getDailyRateCents(),
                nights,
                room.getDailyRateCents() * nights);
    }

    private void recalcTotals() {
        List<BillLine> lines = linesFromCartPreview();
        int sub = BillingCalculator.subtotalCents(lines);
        int tax = BillingCalculator.taxCents(sub);
        int total = BillingCalculator.totalCents(sub, tax);

        int nights = computeNightsOrZero();
        nightsLabel.setText(nights <= 0 ? "—" : String.valueOf(nights));

        subtotalLabel.setText(Money.format(sub));
        taxLabel.setText(Money.format(tax));
        totalLabel.setText(Money.format(total));
    }

    private int computeNightsOrZero() {
        Room room = roomCombo.getSelectionModel().getSelectedItem();
        if (room == null) {
            return 0;
        }
        LocalDate in = checkInDatePicker.getValue();
        LocalDate out = checkOutDatePicker.getValue();
        if (in == null || out == null) {
            return 0;
        }
        long nightsLong = ChronoUnit.DAYS.between(in, out);
        if (nightsLong < 1) {
            return 0;
        }
        return (int) nightsLong;
    }

    private static void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
