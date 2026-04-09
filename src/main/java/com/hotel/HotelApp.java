package com.hotel;

import com.hotel.auth.UserSession;
import com.hotel.controller.LoginController;
import com.hotel.controller.MainController;
import com.hotel.controller.SignupController;
import com.hotel.dao.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class HotelApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Database.initialize();

        showLogin(stage);
    }

    private void showLogin(Stage stage) throws Exception {
        FXMLLoader loginLoader = new FXMLLoader(HotelApp.class.getResource("/com/hotel/login.fxml"));
        Scene loginScene = new Scene(loginLoader.load(), 560, 420);
        loginScene.getStylesheets().add(
                Objects.requireNonNull(HotelApp.class.getResource("/css/theme.css")).toExternalForm());

        LoginController loginController = loginLoader.getController();
        loginController.setLoginSuccessAction(session -> {
            try {
                showMain(stage, session);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to open main app after login", ex);
            }
        });
        loginController.setOpenSignupAction(() -> {
            try {
                showSignup(stage);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to open signup screen", ex);
            }
        });

        stage.setTitle("Hotel Management - Login");
        stage.setMinWidth(520);
        stage.setMinHeight(380);
        stage.setScene(loginScene);
        stage.show();
    }

    private void showSignup(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(HotelApp.class.getResource("/com/hotel/signup.fxml"));
        Scene scene = new Scene(loader.load(), 620, 620);
        scene.getStylesheets().add(
                Objects.requireNonNull(HotelApp.class.getResource("/css/theme.css")).toExternalForm());

        SignupController c = loader.getController();
        c.setBackToLoginAction(() -> {
            try {
                showLogin(stage);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to return to login", ex);
            }
        });

        stage.setTitle("Hotel Management - Sign up");
        stage.setMinWidth(580);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    private void showMain(Stage stage, UserSession session) throws Exception {
        FXMLLoader loader = new FXMLLoader(HotelApp.class.getResource("/com/hotel/main.fxml"));
        Scene scene = new Scene(loader.load(), 1040, 680);

        scene.getStylesheets().add(
                Objects.requireNonNull(HotelApp.class.getResource("/css/theme.css")).toExternalForm());

        MainController main = loader.getController();
        main.setStage(stage);
        main.setUserSession(session);
        main.setLogoutAction(() -> {
            try {
                showLogin(stage);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to return to login", ex);
            }
        });
        main.showDashboard();

        stage.setTitle("Hotel Management");
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
