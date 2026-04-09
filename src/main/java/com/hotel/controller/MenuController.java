package com.hotel.controller;

import com.hotel.dao.RoomDao;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.Money;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class MenuController implements Initializable {

    @FXML
    private TableView<Room> table;

    @FXML
    private TableColumn<Room, String> colRoomNumber;

    @FXML
    private TableColumn<Room, String> colRoomType;

    @FXML
    private TableColumn<Room, Integer> colCapacity;

    @FXML
    private TableColumn<Room, String> colDailyRate;

    @FXML
    private TableColumn<Room, String> colStatus;

    @FXML
    private TextField roomNumberField;

    @FXML
    private TextField roomTypeField;

    @FXML
    private TextField capacityField;

    @FXML
    private TextField dailyRateField;

    @FXML
    private ComboBox<RoomStatus> statusCombo;

    private final RoomDao roomDao = new RoomDao();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusCombo.getItems().addAll(RoomStatus.values());
        statusCombo.getSelectionModel().select(RoomStatus.AVAILABLE);

        colRoomNumber.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colRoomType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colDailyRate.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(Money.format(cd.getValue().getDailyRateCents())));
        colStatus.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatus().name()));

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, item) -> {
            if (item != null) {
                roomNumberField.setText(item.getRoomNumber());
                roomTypeField.setText(item.getRoomType());
                capacityField.setText(String.valueOf(item.getCapacity()));
                dailyRateField.setText(String.format("%.2f", item.getDailyRateCents() / 100.0));
                statusCombo.setValue(item.getStatus());
            }
        });

        onRefresh();
    }

    @FXML
    private void onRefresh() {
        try {
            List<Room> list = roomDao.findAll();
            table.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        try {
            Room r = readFormWithoutId();
            roomDao.insert(r);
            onClearForm();
            onRefresh();
        } catch (SQLException | IllegalArgumentException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        Room sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert(Alert.AlertType.WARNING, "Select a row to update.");
            return;
        }
        try {
            Room r = readFormWithoutId();
            r.setId(sel.getId());
            roomDao.update(r);
            onRefresh();
        } catch (SQLException | IllegalArgumentException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        Room sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert(Alert.AlertType.WARNING, "Select a row to delete.");
            return;
        }
        try {
            roomDao.deleteById(sel.getId());
            onClearForm();
            onRefresh();
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onClearForm() {
        roomNumberField.clear();
        roomTypeField.clear();
        capacityField.clear();
        dailyRateField.clear();
        statusCombo.getSelectionModel().select(RoomStatus.AVAILABLE);
        table.getSelectionModel().clearSelection();
    }

    private Room readFormWithoutId() {
        String roomNumber = roomNumberField.getText() == null ? "" : roomNumberField.getText().trim();
        if (roomNumber.isEmpty()) {
            throw new IllegalArgumentException("Room number is required.");
        }

        String type = roomTypeField.getText() == null ? "" : roomTypeField.getText().trim();
        if (type.isEmpty()) {
            throw new IllegalArgumentException("Room type is required.");
        }

        int cap;
        try {
            cap = Integer.parseInt(capacityField.getText() == null ? "" : capacityField.getText().trim());
        } catch (Exception e) {
            cap = 0;
        }
        if (cap < 1) {
            throw new IllegalArgumentException("Capacity must be >= 1.");
        }

        int dailyCents = Money.parseRupeesToCents(dailyRateField.getText());
        if (dailyCents <= 0) {
            throw new IllegalArgumentException("Enter a valid daily rate.");
        }

        RoomStatus status = statusCombo.getValue();
        if (status == null) {
            status = RoomStatus.AVAILABLE;
        }

        return new Room(0, roomNumber, type, cap, dailyCents, status);
    }

    private static void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
