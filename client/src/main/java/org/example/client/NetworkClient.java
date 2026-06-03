package org.example.client;

import org.example.protocol.Message;
import org.example.protocol.MessageType;
import org.example.protocol.XmlUtil;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private String username;
    private ChatController chatController;
    private AuthController authController;
    private java.util.List<org.example.protocol.UserDTO> lastContacts;

    private NetworkClient() {}

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public void setChatController(ChatController controller) {
        this.chatController = controller;
        if (this.lastContacts != null) {
            controller.updateContacts(this.lastContacts);
        }
    }

    public void setAuthController(AuthController controller) {
        this.authController = controller;
    }

    public String getUsername() {
        return username;
    }

    public void connect(String host, int port, String username) throws Exception {
        this.username = username;
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

        Message authMsg = new Message(MessageType.AUTH, username);
        sendMessage(authMsg);

        new Thread(() -> {
            try {
                String xmlLine;
                while ((xmlLine = in.readLine()) != null) {
                    if (xmlLine.trim().isEmpty()) continue;
                    Message msg = XmlUtil.unmarshal(xmlLine);
                    processMessage(msg);
                }
            } catch (Exception e) {
                System.out.println("Disconnected from server.");
            }
        }).start();
    }

    private void processMessage(Message msg) {
        Platform.runLater(() -> {
            if (msg.getType() == MessageType.AUTH && "Server".equals(msg.getSender()) && "OK".equals(msg.getText())) {
                if (authController != null) {
                    authController.onAuthSuccess();
                }
            } else if (msg.getType() == MessageType.TEXT || msg.getType() == MessageType.FILE) {
                if (chatController != null) {
                    chatController.receiveMessage(msg);
                }
            } else if (msg.getType() == MessageType.DELETE_MESSAGE) {
                if (chatController != null) {
                    chatController.deleteMessage(msg.getId());
                }
            } else if (msg.getType() == MessageType.CONTACT_LIST) {
                String text = msg.getText();
                java.util.List<org.example.protocol.UserDTO> parsed = new java.util.ArrayList<>();
                if (text != null && !text.isEmpty()) {
                    String[] parts = text.split(";;");
                    for (String p : parts) {
                        if (p.isEmpty()) continue;
                        String[] up = p.split("\\|", -1);
                        if (up.length >= 2) {
                            String name = up[0];
                            String av = up[1].equals("null") ? null : up[1];
                            parsed.add(new org.example.protocol.UserDTO(name, av));
                        }
                    }
                }
                this.lastContacts = parsed;
                if (chatController != null) {
                    chatController.updateContacts(this.lastContacts);
                }
            } else if (msg.getType() == MessageType.HISTORY) {
                if (chatController != null) {
                    java.util.List<Message> parsedHistory = new java.util.ArrayList<>();
                    if (msg.getText() != null && !msg.getText().trim().isEmpty()) {
                        String[] parts = msg.getText().split("\\|\\|\\|");
                        for (String p : parts) {
                            if (p.trim().isEmpty()) continue;
                            String[] mParts = p.split("::", -1);
                            if (mParts.length >= 6) {
                                MessageType type = MessageType.valueOf(mParts[0]);
                                Message m = new Message(type, mParts[1]);
                                m.setReceiver(mParts[2]);
                                if (!mParts[3].isEmpty()) m.setText(new String(java.util.Base64.getDecoder().decode(mParts[3]), java.nio.charset.StandardCharsets.UTF_8));
                                if (!mParts[4].isEmpty()) m.setFileName(new String(java.util.Base64.getDecoder().decode(mParts[4]), java.nio.charset.StandardCharsets.UTF_8));
                                if (!mParts[5].isEmpty()) m.setFileBase64(mParts[5]);
                                parsedHistory.add(m);
                            }
                        }
                    }
                    chatController.loadHistory(parsedHistory, msg.getReceiver());
                }
            }
        });
    }

    public void sendMessage(Message msg) {
        try {
            msg.setSender(this.username);
            String xml = XmlUtil.marshal(msg);
            out.write(xml);
            out.newLine();
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (Exception e) {}
    }
}
