package org.example.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.server.db.DatabaseManager;

public class ServerApp extends Application {
    private ServerCore serverCore;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/server_view.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        
        ServerController controller = loader.getController();
        ServerCore.setController(controller);
        
        serverCore = new ServerCore();
        new Thread(serverCore).start();

        stage.setTitle("ICQ Server");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (serverCore != null) {
            serverCore.stop();
        }
        DatabaseManager.shutdown();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
