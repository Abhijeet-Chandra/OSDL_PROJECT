package com.hotel.controller;

import com.hotel.auth.UserSession;
import com.hotel.dao.UserDao;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.function.Consumer;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private final UserDao userDao = new UserDao();

    private Consumer<UserSession> loginSuccessAction;
    private Runnable openSignupAction;

    public void setLoginSuccessAction(Consumer<UserSession> loginSuccessAction) {
        this.loginSuccessAction = loginSuccessAction;
    }

    public void setOpenSignupAction(Runnable openSignupAction) {
        this.openSignupAction = openSignupAction;
    }

    @FXML
    private void onSignup() {
        if (openSignupAction != null) {
            openSignupAction.run();
        }
    }

    @FXML
    private void onLogin() {
        String user = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();

        Task<Optional<UserSession>> task = new Task<>() {
            @Override
            protected Optional<UserSession> call() throws Exception {
                return userDao.authenticate(user, pass);
            }
        };

        task.setOnSucceeded(e -> {
            Optional<UserSession> result = task.getValue();
            if (result.isPresent()) {
                if (loginSuccessAction != null) {
                    loginSuccessAction.accept(result.get());
                }
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR,
                        "Invalid username or password.\n\nDemo accounts:\n"
                                + "• admin / admin123 (admin dashboard)\n\n"
                                + "Tip: use Sign up to create a guest account.");
                a.setHeaderText("Login failed");
                a.showAndWait();
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String m = ex == null ? "Unknown error" : ex.getMessage();
            Alert a = new Alert(Alert.AlertType.ERROR, "Could not verify login:\n" + m);
            a.setHeaderText("Login error");
            a.showAndWait();
        });

        Thread th = new Thread(task, "login-auth-thread");
        th.setDaemon(true);
        th.start();
    }

    @FXML
    private void onClear() {
        usernameField.clear();
        passwordField.clear();
        usernameField.requestFocus();
    }

    @FXML
    private void onExit() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close();
    }
}
