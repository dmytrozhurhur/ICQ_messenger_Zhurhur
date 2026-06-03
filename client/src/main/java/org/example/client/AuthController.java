package org.example.client;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.application.Platform;

public class AuthController {
    @FXML private TextField hostField;
    @FXML private TextField usernameField;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        NetworkClient.getInstance().setAuthController(this);
    }

    @FXML
    public void onLogin() {
        String host = hostField.getText();
        String username = usernameField.getText();
        if (username == null || username.trim().isEmpty()) {
            errorLabel.setText("Username is required");
            return;
        }
        try {
            NetworkClient.getInstance().connect(host, 8081, username);
        } catch (Exception e) {
            errorLabel.setText("Connection failed: " + e.getMessage());
        }
    }

    public void onAuthSuccess() {
        Platform.runLater(() -> {
            ClientApp.showChatWindow();
        });
    }
}
