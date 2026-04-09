package com.hotel.controller;

import com.hotel.auth.UserSession;
import com.hotel.model.UserRole;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Objects;

public class MainController {

    @FXML
    private StackPane contentPane;

    @FXML
    private Label lblAccount;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnRooms;

    @FXML
    private Button btnBookings;

    @FXML
    private Button btnBilling;

    @FXML
    private Button btnExtras;

    @FXML
    private Button btnHistory;

    private Stage stage;
    private UserSession session;
    private Runnable logoutAction;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }

    public void setUserSession(UserSession session) {
        this.session = session;
        applyNavForRole();
    }

    public UserSession getSession() {
        return session;
    }

    public void setLogoutAction(Runnable logoutAction) {
        this.logoutAction = logoutAction;
    }

    private void applyNavForRole() {
        if (lblAccount == null || session == null) {
            return;
        }
        lblAccount.setText(session.username() + "  •  " + friendlyRole(session.role()));
        boolean customer = session.isCustomer();
        setNavVisible(btnRooms, !customer);
        setNavVisible(btnExtras, !customer);
        btnBookings.setText(customer ? "My bookings" : "Bookings");
        btnBilling.setText(customer ? "Book a room" : "Billing");
        btnHistory.setText(customer ? "My bills" : "Bill history");

        // Keep admin focused on inventory/audit, not booking.
        setNavVisible(btnBilling, customer);
    }

    private static void setNavVisible(Button b, boolean visible) {
        b.setVisible(visible);
        b.setManaged(visible);
    }

    private static String friendlyRole(UserRole role) {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case ADMIN -> "Admin";
            case CUSTOMER -> "Guest";
        };
    }

    @FXML
    private void onLogout() {
        if (logoutAction != null) {
            logoutAction.run();
        }
    }

    @FXML
    public void showDashboard() {
        if (session != null && session.isCustomer()) {
            load("/com/hotel/customer-dashboard.fxml");
        } else {
            load("/com/hotel/dashboard.fxml");
        }
    }

    @FXML
    public void showMenu() {
        load("/com/hotel/menu.fxml");
    }

    @FXML
    public void showBilling() {
        if (session != null && !session.isCustomer()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "Only customers/guests can book rooms.\n\nAdmin can manage Rooms/Extras and view Bookings/History.");
            a.setHeaderText("Booking disabled for admin");
            a.showAndWait();
            return;
        }
        load("/com/hotel/billing.fxml");
    }

    @FXML
    public void showHistory() {
        load("/com/hotel/history.fxml");
    }

    @FXML
    public void showBookings() {
        load("/com/hotel/bookings.fxml");
    }

    @FXML
    public void showExtras() {
        load("/com/hotel/extras.fxml");
    }

    private void load(String resource) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(resource)));
            Node node = loader.load();
            Object controller = loader.getController();
            applySession(controller);
            if (controller instanceof CustomerDashboardController cdc) {
                cdc.setMainController(this);
            }
            contentPane.getChildren().setAll(node);
        } catch (Exception e) {
            // If FXMLLoader fails, the UI would otherwise "stick" on the previous tab.
            e.printStackTrace();

            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }

            String msg = (e.getMessage() == null || e.getMessage().isBlank()) ? "" : e.getMessage();
            String rootMsg = (root.getMessage() == null || root.getMessage().isBlank()) ? "" : root.getMessage();

            javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "Failed to open screen:\n" + resource
                            + "\n\nException: " + e.getClass().getName()
                            + (msg.isBlank() ? "" : ("\nMessage: " + msg))
                            + "\n\nRoot cause: " + root.getClass().getName()
                            + (rootMsg.isBlank() ? "" : ("\nRoot message: " + rootMsg))
            );
            a.setHeaderText("Navigation error");
            a.showAndWait();
        }
    }

    private void applySession(Object controller) {
        if (session != null && controller instanceof UserSessionAware aware) {
            aware.setUserSession(session);
        }
    }
}
