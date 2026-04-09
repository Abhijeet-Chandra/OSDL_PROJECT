package com.hotel.controller;

import com.hotel.auth.UserSession;
import com.hotel.dao.BillDao;
import com.hotel.dao.GuestDao;
import com.hotel.model.Bill;
import com.hotel.model.BillLine;
import com.hotel.model.Guest;
import com.hotel.model.Money;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class HistoryController implements Initializable, UserSessionAware {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private Label pageTitleLabel;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TableView<Bill> billTable;

    @FXML
    private TableColumn<Bill, Integer> colBillId;

    @FXML
    private TableColumn<Bill, String> colTime;

    @FXML
    private TableColumn<Bill, String> colStudent;

    @FXML
    private TableColumn<Bill, String> colSub;

    @FXML
    private TableColumn<Bill, String> colTax;

    @FXML
    private TableColumn<Bill, String> colTot;

    private final BillDao billDao = new BillDao();
    private final GuestDao guestDao = new GuestDao();

    private UserSession userSession;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        datePicker.setValue(LocalDate.now());

        colBillId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTime.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(TIME_FMT.format(cd.getValue().getCreatedAt())));
        colStudent.setCellValueFactory(cd -> {
            String s = cd.getValue().getStudentId();
            return new javafx.beans.property.SimpleStringProperty(s == null ? "—" : s);
        });
        colSub.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(Money.format(cd.getValue().getSubtotalCents())));
        colTax.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(Money.format(cd.getValue().getTaxCents())));
        colTot.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(Money.format(cd.getValue().getTotalCents())));
    }

    @Override
    public void setUserSession(UserSession session) {
        this.userSession = session;
        if (pageTitleLabel != null) {
            pageTitleLabel.setText(session != null && session.isCustomer() ? "My bills" : "Bill history");
        }
        onRefresh();
    }

    @FXML
    private void onRefresh() {
        LocalDate d = datePicker.getValue();
        if (d == null) {
            d = LocalDate.now();
            datePicker.setValue(d);
        }
        try {
            String guestName = resolveGuestFilterName();
            List<Bill> list = billDao.findByDateAndGuestName(d, guestName);
            billTable.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    private String resolveGuestFilterName() throws SQLException {
        if (userSession == null || !userSession.isCustomer() || userSession.linkedGuestId() == null) {
            return null;
        }
        Guest g = guestDao.findById(userSession.linkedGuestId());
        return g != null ? g.getName() : null;
    }

    @FXML
    private void onShowDetails() {
        Bill b = billTable.getSelectionModel().getSelectedItem();
        if (b == null) {
            alert(Alert.AlertType.WARNING, "Select a bill.");
            return;
        }
        try {
            List<BillLine> lines = billDao.findLinesForBill(b.getId());
            StringBuilder sb = new StringBuilder();
            sb.append("Bill #").append(b.getId()).append("\n");
            sb.append("Time: ").append(TIME_FMT.format(b.getCreatedAt())).append("\n");
            sb.append("Guest: ").append(b.getStudentId() == null ? "—" : b.getStudentId()).append("\n\n");
            for (BillLine line : lines) {
                sb.append(line.getName())
                        .append("  x").append(line.getQty())
                        .append("  @ ").append(Money.format(line.getUnitPriceCents()))
                        .append("  = ").append(Money.format(line.getLineTotalCents()))
                        .append("\n");
            }
            sb.append("\nSubtotal: ").append(Money.format(b.getSubtotalCents()));
            sb.append("\nTax: ").append(Money.format(b.getTaxCents()));
            sb.append("\nTotal: ").append(Money.format(b.getTotalCents()));

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setHeaderText("Bill lines");
            a.setContentText(sb.toString());
            a.getDialogPane().setMinWidth(420);
            a.showAndWait();
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    private static void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
