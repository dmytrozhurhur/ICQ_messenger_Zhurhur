package org.example.server;

import org.example.protocol.Message;
import org.example.protocol.MessageType;
import org.example.protocol.XmlUtil;
import org.example.server.db.DatabaseManager;
import org.example.server.db.MessageEntity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.logging.Logger;

import java.util.List;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String xmlLine;
            while ((xmlLine = in.readLine()) != null) {
                if (xmlLine.trim().isEmpty()) continue;
                Message msg = XmlUtil.unmarshal(xmlLine);
                processMessage(msg);
            }
        } catch (Exception e) {
            LOGGER.info("Client disconnected: " + username);
        } finally {
            ServerCore.removeClient(this);
            try { socket.close(); } catch (Exception e) {}
        }
    }

    private void processMessage(Message msg) {
        try {
            switch (msg.getType()) {
                case AUTH:
                    this.username = msg.getSender();
                    try {
                        DatabaseManager.saveUser(username);
                    } catch (Exception e) {
                        ServerCore.logToGUI("DB ERROR (saveUser): " + e.getMessage());
                    }
                    ServerCore.addClient(this.username, this);
                    ServerCore.logToGUI(username + " connected.");
                    // send confirmation
                    Message authOk = new Message(MessageType.AUTH, "Server");
                    authOk.setText("OK");
                    sendMessage(authOk);
                    
                    ServerCore.broadcastContactList();
                    break;
                case TEXT:
                case FILE:
                case VOICE:
                    try {
                        MessageEntity savedEntity = DatabaseManager.saveMessage(msg);
                        msg.setId(savedEntity.getId());
                    } catch (Exception e) {
                        ServerCore.logToGUI("DB ERROR (saveMessage): " + e.getMessage());
                        msg.setId(System.currentTimeMillis()); // Fake ID if DB fails
                    }
                    ServerCore.logToGUI(msg.getSender() + " -> " + msg.getReceiver() + ": " + (msg.getType() == MessageType.FILE ? "[FILE] " + msg.getFileName() : msg.getText()));
                    
                    // Send to sender
                    sendMessage(msg);
                    
                    // Send to receiver if online
                    ClientHandler receiverHandler = ServerCore.getClient(msg.getReceiver());
                    if (receiverHandler != null && !msg.getReceiver().equals(msg.getSender())) {
                        receiverHandler.sendMessage(msg);
                    }
                    break;
                case HISTORY:
                    try {
                        List<Message> history = DatabaseManager.getHistory(this.username, msg.getReceiver());
                        Message histResp = new Message(MessageType.HISTORY, "Server");
                        histResp.setReceiver(msg.getReceiver());
                        StringBuilder sb = new StringBuilder();
                        for (Message m : history) {
                            String idStr = m.getId() == null ? "" : m.getId().toString();
                            String type = m.getType() != null ? m.getType().name() : "TEXT";
                            String sender = m.getSender() != null ? m.getSender() : "";
                            String receiver = m.getReceiver() != null ? m.getReceiver() : "";
                            String textBase64 = m.getText() == null ? "" : java.util.Base64.getEncoder().encodeToString(m.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            String fileNameBase64 = m.getFileName() == null ? "" : java.util.Base64.getEncoder().encodeToString(m.getFileName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            String fileData = m.getFileBase64() == null ? "" : m.getFileBase64();
                            String replyToIdStr = m.getReplyToId() == null ? "" : m.getReplyToId().toString();

                            sb.append(idStr).append("::")
                              .append(type).append("::")
                              .append(sender).append("::")
                              .append(receiver).append("::")
                              .append(textBase64).append("::")
                              .append(fileNameBase64).append("::")
                              .append(fileData).append("::")
                              .append(replyToIdStr).append("|||");
                        }
                        histResp.setText(sb.toString());
                        sendMessage(histResp);
                    } catch (Exception e) {
                        ServerCore.logToGUI("DB ERROR (getHistory): " + e.getMessage());
                        Message histResp = new Message(MessageType.HISTORY, "Server");
                        histResp.setReceiver(msg.getReceiver());
                        histResp.setText("");
                        sendMessage(histResp);
                    }
                    break;
                case DELETE_MESSAGE:
                    DatabaseManager.deleteMessageById(msg.getId());
                    ServerCore.logToGUI(msg.getSender() + " deleted message ID " + msg.getId());
                    ServerCore.broadcastToAll(msg); 
                    break;
                case UPDATE_AVATAR:
                    if (msg.getFileBase64() != null) {
                        byte[] data = java.util.Base64.getDecoder().decode(msg.getFileBase64());
                        DatabaseManager.saveUserAvatar(this.username, data);
                        ServerCore.broadcastContactList();
                    }
                    break;
                case DELETE_USER:
                    String userToDelete = msg.getText();
                    DatabaseManager.deleteUser(userToDelete);
                    ClientHandler h = ServerCore.getClient(userToDelete);
                    if (h != null) {
                        try { h.socket.close(); } catch(Exception e){}
                    }
                    ServerCore.broadcastContactList();
                    break;
                case CONTACT_LIST:
                    Message contactMsg = new Message(MessageType.CONTACT_LIST, "Server");
                    StringBuilder sb = new StringBuilder();
                    try {
                        List<org.example.protocol.UserDTO> users = DatabaseManager.getAllUserDTOs();
                        for (org.example.protocol.UserDTO u : users) {
                            sb.append(u.getUsername()).append("|").append(u.getAvatarBase64() == null ? "null" : u.getAvatarBase64()).append(";;");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        for (String u : ServerCore.getActiveUsers()) {
                            sb.append(u).append("|null;;");
                        }
                    }
                    contactMsg.setText(sb.toString());
                    sendMessage(contactMsg);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message msg) {
        try {
            String xml = XmlUtil.marshal(msg);
            out.write(xml);
            out.newLine();
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }
}
