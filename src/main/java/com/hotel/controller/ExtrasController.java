package com.hotel.controller;

import com.hotel.dao.MenuItemDao;
import com.hotel.model.MenuItem;
import com.hotel.model.Money;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ExtrasController implements Initializable {

    @FXML
    private TableView<MenuItem> table;

    @FXML
    private TableColumn<MenuItem, Integer> colId;

    @FXML
    private TableColumn<MenuItem, String> colName;

    @FXML
    private TableColumn<MenuItem, String> colCategory;

    @FXML
    private TableColumn<MenuItem, String> colPrice;

    @FXML
    private TableColumn<MenuItem, String> colActive;

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> categoryCombo;

    @FXML
    private TextField priceField;

    @FXML
    private CheckBox activeCheck;

    private final MenuItemDao menuItemDao = new MenuItemDao();
    private final ObservableList<MenuItem> items = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        categoryCombo.getItems().addAll("Meals", "Snacks", "Beverages", "Laundry", "Parking");
        categoryCombo.getSelectionModel().selectFirst();
        activeCheck.setSelected(true);

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colPrice.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(Money.format(cd.getValue().getPriceCents())));
        colActive.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().isActive() ? "Yes" : "No"));

        table.setItems(items);

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, item) -> {
            if (item != null) {
                nameField.setText(item.getName());
                categoryCombo.setValue(item.getCategory());
                priceField.setText(String.format("%.2f", item.getPriceCents() / 100.0));
                activeCheck.setSelected(item.isActive());
            }
        });

        onRefresh();
    }

    @FXML
    private void onRefresh() {
        try {
            List<MenuItem> list = menuItemDao.findAll();
            items.setAll(list);
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onAdd() {
        try {
            MenuItem m = readFormWithoutId();
            menuItemDao.insert(m);
            onClearForm();
            onRefresh();
        } catch (SQLException | IllegalArgumentException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        MenuItem sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert(Alert.AlertType.WARNING, "Select a row to update.");
            return;
        }
        try {
            MenuItem m = readFormWithoutId();
            m.setId(sel.getId());
            menuItemDao.update(m);
            onRefresh();
        } catch (SQLException | IllegalArgumentException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        MenuItem sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alert(Alert.AlertType.WARNING, "Select a row to delete.");
            return;
        }
        try {
            menuItemDao.deleteById(sel.getId());
            onClearForm();
            onRefresh();
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    private void onClearForm() {
        nameField.clear();
        priceField.clear();
        activeCheck.setSelected(true);
        categoryCombo.getSelectionModel().selectFirst();
        table.getSelectionModel().clearSelection();
    }

    private MenuItem readFormWithoutId() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Name is required.");
        }
        String cat = categoryCombo.getValue();
        if (cat == null || cat.isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }

        int cents = Money.parseRupeesToCents(priceField.getText());
        if (cents <= 0) {
            throw new IllegalArgumentException("Enter a valid price.");
        }

        return new MenuItem(0, name, cat.trim(), cents, activeCheck.isSelected());
    }

    private static void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}

