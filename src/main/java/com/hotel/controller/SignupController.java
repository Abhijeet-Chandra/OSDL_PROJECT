package com.hotel.controller;

import com.hotel.dao.GuestDao;
import com.hotel.dao.UserDao;
import com.hotel.model.Guest;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignupController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    private final GuestDao guestDao = new GuestDao();
    private final UserDao userDao = new UserDao();

    private Runnable backToLoginAction;

    public void setBackToLoginAction(Runnable backToLoginAction) {
        this.backToLoginAction = backToLoginAction;
    }

    @FXML
    private void onBack() {
        if (backToLoginAction != null) {
            backToLoginAction.run();
        }
    }

    @FXML
    private void onSignup() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();
        String pass2 = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (name.isBlank()) {
            alert(Alert.AlertType.WARNING, "Enter your name.");
            return;
        }
        if (username.isBlank()) {
            alert(Alert.AlertType.WARNING, "Choose a username.");
            return;
        }
        if (pass.isBlank()) {
            alert(Alert.AlertType.WARNING, "Choose a password.");
            return;
        }
        if (!pass.equals(pass2)) {
            alert(Alert.AlertType.WARNING, "Passwords do not match.");
            return;
        }
        if (pass.length() < 4) {
            alert(Alert.AlertType.WARNING, "Password should be at least 4 characters.");
            return;
        }

        Task<Void> t = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int guestId = guestDao.insertGuest(new Guest(0, name, phone.isBlank() ? null : phone));
                userDao.createCustomerUser(username, pass, guestId);
                return null;
            }
        };

        t.setOnSucceeded(e -> {
            alert(Alert.AlertType.INFORMATION, "Account created successfully. You can now log in.");
            if (backToLoginAction != null) {
                backToLoginAction.run();
            }
        });

        t.setOnFailed(e -> {
            Throwable ex = t.getException();
            String m = ex == null ? "Unknown error" : ex.getMessage();
            alert(Alert.AlertType.ERROR, m);
        });

        Thread th = new Thread(t, "signup-thread");
        th.setDaemon(true);
        th.start();
    }

    private static void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}

