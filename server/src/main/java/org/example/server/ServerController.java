package org.example.server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

import java.util.Set;

public class ServerController {
    @FXML
    private TextArea logArea;
    
    @FXML
    private ListView<String> usersList;

    public void log(String text) {
        Platform.runLater(() -> {
            logArea.appendText(text + "\n");
        });
    }

    public void updateUserList(Set<String> users) {
        Platform.runLater(() -> {
            usersList.getItems().clear();
            usersList.getItems().addAll(users);
        });
    }
}
