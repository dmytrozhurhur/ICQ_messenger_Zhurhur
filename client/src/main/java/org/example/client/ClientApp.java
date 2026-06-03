package org.example.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        showAuthWindow();
    }

    public static void showAuthWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/auth.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 400, 250);
            primaryStage.setTitle("ICQ Login");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showChatWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/chat.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 800, 600);
            primaryStage.setTitle("ICQ Chat - " + NetworkClient.getInstance().getUsername());
            primaryStage.setScene(scene);
            primaryStage.show();
            ChatController controller = loader.getController();
            NetworkClient.getInstance().setChatController(controller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        if (NetworkClient.getInstance() != null) {
            NetworkClient.getInstance().disconnect();
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
