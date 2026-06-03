package org.example.server;

import org.example.protocol.Message;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.example.server.db.DatabaseManager;
import org.example.protocol.MessageType;
import java.util.List;

public class ServerCore implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ServerCore.class.getName());
    private static final int PORT = 8081;
    private static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static ServerController controller;
    private boolean running = true;
    private ServerSocket serverSocket;

    public static void setController(ServerController ctrl) {
        controller = ctrl;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
            logToGUI("Server started on port " + PORT);
            while (running) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            if (running) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {}
    }

    public static void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        updateActiveUsers();
    }

    public static void removeClient(ClientHandler handler) {
        if (handler.getUsername() != null) {
            clients.remove(handler.getUsername());
            updateActiveUsers();
            logToGUI(handler.getUsername() + " disconnected.");
        }
    }

    public static void broadcastToAll(Message msg) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(msg);
        }
    }

    public static ClientHandler getClient(String username) {
        if (username == null) return null;
        return clients.get(username);
    }

    public static Set<String> getActiveUsers() {
        return clients.keySet();
    }

    public static void broadcastContactList() {
        Message msg = new Message(MessageType.CONTACT_LIST, "Server");
        StringBuilder sb = new StringBuilder();
        try {
            List<org.example.protocol.UserDTO> users = DatabaseManager.getAllUserDTOs();
            for (org.example.protocol.UserDTO u : users) {
                sb.append(u.getUsername()).append("|").append(u.getAvatarBase64() == null ? "null" : u.getAvatarBase64()).append(";;");
            }
        } catch (Exception e) {
            e.printStackTrace();
            for (String u : clients.keySet()) {
                sb.append(u).append("|null;;");
            }
        }
        msg.setText(sb.toString());
        broadcastToAll(msg);
    }

    public static void logToGUI(String text) {
        if (controller != null) {
            controller.log(text);
        } else {
            System.out.println(text);
        }
    }

    private static void updateActiveUsers() {
        if (controller != null) {
            controller.updateUserList(clients.keySet());
        }
    }
}
