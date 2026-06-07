package org.example.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.example.protocol.Message;
import org.example.protocol.MessageType;
import org.example.protocol.UserDTO;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;

public class ChatController {
    @FXML private VBox messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField inputField;
    @FXML private HBox replyBox;
    @FXML private Label replyLabel;
    @FXML private Label chatTitle;
    
    @FXML private ListView<UserDTO> contactsList;
    @FXML private Button attachBtn;
    @FXML private Button sendBtn;
    @FXML private Label myNameLabel;
    @FXML private Label myAvatarLabel;
    @FXML private ImageView myAvatarView;
    private Message replyingTo = null;
    private UserDTO currentContact = null;

    private Set<String> pinnedUsers = new HashSet<>();
    private Map<String, Image> avatarCache = new HashMap<>();
    private ObservableList<UserDTO> observableContacts = FXCollections.observableArrayList();
    private List<UserDTO> allRawContacts = new ArrayList<>();

    @FXML
    public void initialize() {
        NetworkClient.getInstance().setChatController(this);
        messagesContainer.heightProperty().addListener((obs, oldVal, newVal) -> scrollPane.setVvalue(1.0));
        replyBox.setVisible(false);
        replyBox.setManaged(false);

        contactsList.setItems(observableContacts);
        contactsList.setCellFactory(lv -> new ContactCell());

        contactsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.getUsername().equals(NetworkClient.getInstance().getUsername())) {
                currentContact = newVal;
                chatTitle.setText(currentContact.getUsername());
                inputField.setDisable(false);
                attachBtn.setDisable(false);
                sendBtn.setDisable(false);
                inputField.setPromptText("Write a message...");
                
                Message req = new Message(MessageType.HISTORY, NetworkClient.getInstance().getUsername());
                req.setReceiver(currentContact.getUsername());
                NetworkClient.getInstance().sendMessage(req);
            }
        });

        String myName = NetworkClient.getInstance().getUsername();
        if (myName != null && !myName.isEmpty()) {
            myNameLabel.setText(myName);
            myAvatarLabel.setText(myName.substring(0, 1).toUpperCase());
        }

        // Запит контактів при відкритті вікна, щоб уникнути втрати
        Message reqList = new Message(MessageType.CONTACT_LIST, NetworkClient.getInstance().getUsername());
        NetworkClient.getInstance().sendMessage(reqList);
    }

    @FXML
    public void uploadAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(myNameLabel.getScene().getWindow());
        if (file != null) {
            try {
                if (file.length() > 2 * 1024 * 1024) {
                    Alert a = new Alert(Alert.AlertType.ERROR, "Avatar size must be <= 2MB");
                    a.show();
                    return;
                }
                byte[] data = Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(data);
                Message msg = new Message(MessageType.UPDATE_AVATAR, NetworkClient.getInstance().getUsername());
                msg.setFileBase64(base64);
                NetworkClient.getInstance().sendMessage(msg);
                
                Image img = new Image(new ByteArrayInputStream(data));
                myAvatarView.setImage(img);
                myAvatarLabel.setVisible(false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateContacts(List<UserDTO> contacts) {
        Platform.runLater(() -> {
            try {
                if (contacts == null) {
                    new Alert(Alert.AlertType.ERROR, "Помилка JAXB: список контактів null!").show();
                    return;
                }
                List<UserDTO> mutableContacts = new ArrayList<>(contacts);
                String myName = NetworkClient.getInstance().getUsername();
                mutableContacts.removeIf(u -> u.getUsername().equals(myName));
                
                for (UserDTO u : contacts) {
                    if (u.getAvatarBase64() != null) {
                        byte[] decoded = Base64.getDecoder().decode(u.getAvatarBase64());
                        Image img = new Image(new ByteArrayInputStream(decoded));
                        avatarCache.put(u.getUsername(), img);
                        if (u.getUsername().equals(myName)) {
                            myAvatarView.setImage(img);
                            myAvatarLabel.setVisible(false);
                        }
                    }
                }

                allRawContacts = new ArrayList<>(mutableContacts);
                refreshContactsList();

                UserDTO selected = contactsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    for (UserDTO u : observableContacts) {
                        if (u.getUsername().equals(selected.getUsername())) {
                            contactsList.getSelectionModel().select(u);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Помилка оновлення контактів: " + e.toString());
                alert.show();
                e.printStackTrace();
            }
        });
    }

    private void refreshContactsList() {
        allRawContacts.sort((u1, u2) -> {
            boolean p1 = pinnedUsers.contains(u1.getUsername());
            boolean p2 = pinnedUsers.contains(u2.getUsername());
            if (p1 && !p2) return -1;
            if (!p1 && p2) return 1;
            return u1.getUsername().compareToIgnoreCase(u2.getUsername());
        });
        observableContacts.setAll(allRawContacts);
    }

    public void loadHistory(List<Message> history, String contact) {
        Platform.runLater(() -> {
            if (currentContact != null && contact.equals(currentContact.getUsername())) {
                messagesContainer.getChildren().clear();
                if (history != null) {
                    for (Message m : history) {
                        addMessage(m);
                    }
                }
                // Force JavaFX layout pass to ensure the first loaded message is instantly visible
                messagesContainer.layout();
                scrollPane.layout();
                scrollPane.setVvalue(1.0);
            }
        });
    }

    public void receiveMessage(Message msg) {
        Platform.runLater(() -> {
            boolean isMine = msg.getSender().equals(NetworkClient.getInstance().getUsername());
            String otherPerson = isMine ? msg.getReceiver() : msg.getSender();
            
            if (currentContact == null || !currentContact.getUsername().equals(otherPerson)) {
                for (UserDTO u : observableContacts) {
                    if (u.getUsername().equals(otherPerson)) {
                        contactsList.getSelectionModel().select(u);
                        return; // Selection listener will trigger history load which includes this message
                    }
                }
            }

            if (currentContact != null) {
                boolean belongsToCurrentChat = 
                    (msg.getSender().equals(currentContact.getUsername()) && msg.getReceiver().equals(NetworkClient.getInstance().getUsername())) ||
                    (msg.getSender().equals(NetworkClient.getInstance().getUsername()) && msg.getReceiver().equals(currentContact.getUsername()));
                
                if (belongsToCurrentChat) {
                    addMessage(msg);
                }
            }
        });
    }

    @FXML
    public void onSend() {
        if (currentContact == null) return;
        
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty()) return;
        
        Message msg = new Message(MessageType.TEXT, NetworkClient.getInstance().getUsername());
        msg.setText(text);
        msg.setReceiver(currentContact.getUsername());
        
        if (replyingTo != null) {
            msg.setReplyToId(replyingTo.getId());
            cancelReply();
        }
        NetworkClient.getInstance().sendMessage(msg);
        inputField.clear();
    }

    @FXML
    public void onAttachFile() {
        if (currentContact == null) return;
        
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(inputField.getScene().getWindow());
        if (file != null) {
            try {
                if (file.length() > 5 * 1024 * 1024) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "File size must be <= 5MB");
                    alert.show();
                    return;
                }
                byte[] data = Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(data);
                
                Message msg = new Message(MessageType.FILE, NetworkClient.getInstance().getUsername());
                msg.setFileName(file.getName());
                msg.setFileBase64(base64);
                msg.setReceiver(currentContact.getUsername());
                
                NetworkClient.getInstance().sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addMessage(Message msg) {
        HBox row = new HBox();
        boolean isMine = msg.getSender().equals(NetworkClient.getInstance().getUsername());
        row.setAlignment(isMine ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.CENTER_LEFT);

        VBox messageBox = new VBox(5);
        messageBox.setUserData(msg.getId()); 
        
        messageBox.getStyleClass().add("message-box");
        if (isMine) {
            messageBox.getStyleClass().add("message-mine");
        } else {
            messageBox.getStyleClass().add("message-other");
        }

        Label senderLabel = new Label(msg.getSender());
        senderLabel.getStyleClass().add("message-sender");
        messageBox.getChildren().add(senderLabel);

        if (msg.getReplyToId() != null) {
            String replyText = findMessageTextById(msg.getReplyToId());
            if (replyText == null) {
                replyText = "Message ID " + msg.getReplyToId();
            }
            Label replyIndicator = new Label("Відповідь: " + replyText);
            replyIndicator.getStyleClass().add("message-reply");
            messageBox.getChildren().add(replyIndicator);
        }

        if (msg.getType() == MessageType.FILE) {
            String fname = msg.getFileName().toLowerCase();
            if (fname.endsWith(".png") || fname.endsWith(".jpg") || fname.endsWith(".jpeg") || fname.endsWith(".gif")) {
                try {
                    byte[] data = Base64.getDecoder().decode(msg.getFileBase64());
                    Image img = new Image(new ByteArrayInputStream(data));
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(200);
                    iv.setPreserveRatio(true);
                    messageBox.getChildren().add(iv);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Button downloadBtn = new Button("Завантажити " + msg.getFileName());
            downloadBtn.getStyleClass().add("btn-download");
            downloadBtn.setOnAction(e -> downloadFile(msg));
            messageBox.getChildren().add(downloadBtn);
        } else {
            Text content = new Text(msg.getText());
            content.getStyleClass().add("message-content");
            content.setWrappingWidth(300);
            messageBox.getChildren().add(content);
        }

        ContextMenu contextMenu = new ContextMenu();
        MenuItem replyItem = new MenuItem("Відповісти");
        replyItem.setOnAction(e -> setReplyTo(msg));
        contextMenu.getItems().add(replyItem);

        if (isMine) {
            MenuItem deleteItem = new MenuItem("Видалити");
            deleteItem.setOnAction(e -> requestDelete(msg.getId()));
            contextMenu.getItems().add(deleteItem);
        }
        
        messageBox.setOnContextMenuRequested(e -> contextMenu.show(messageBox, e.getScreenX(), e.getScreenY()));

        if (!isMine) {
            // Avatar
            ImageView avatarView = new ImageView();
            avatarView.setFitWidth(30);
            avatarView.setFitHeight(30);
            Circle clip = new Circle(15, 15, 15);
            avatarView.setClip(clip);
            Image avatar = avatarCache.get(msg.getSender());
            Circle bgCircle = new Circle(15, javafx.scene.paint.Color.web("#4a76a8"));
            Label letter = new Label(msg.getSender().substring(0, 1).toUpperCase());
            letter.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            StackPane stack = new StackPane(bgCircle, letter);
            
            if (avatar != null) {
                avatarView.setImage(avatar);
                stack.getChildren().add(avatarView);
            }
            
            HBox bubbleWithAvatar = new HBox(10, stack, messageBox);
            bubbleWithAvatar.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
            row.getChildren().add(bubbleWithAvatar);
        } else {
            row.getChildren().add(messageBox);
        }

        messagesContainer.getChildren().add(row);
    }

    public void deleteMessage(Long id) {
        Platform.runLater(() -> {
            messagesContainer.getChildren().removeIf(row -> {
                if (row instanceof HBox) {
                    for (javafx.scene.Node child : ((HBox)row).getChildren()) {
                        if (child instanceof VBox && child.getUserData() != null && child.getUserData().equals(id)) {
                            return true;
                        }
                        if (child instanceof HBox) { // avatar wrapper
                            for (javafx.scene.Node subChild : ((HBox)child).getChildren()) {
                                if (subChild instanceof VBox && subChild.getUserData() != null && subChild.getUserData().equals(id)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;
            });
        });
    }

    private String findMessageTextById(Long id) {
        for (javafx.scene.Node row : messagesContainer.getChildren()) {
            if (row instanceof HBox) {
                for (javafx.scene.Node child : ((HBox)row).getChildren()) {
                    if (child instanceof VBox && child.getUserData() != null && child.getUserData().equals(id)) {
                        return extractTextFromMessageBox((VBox)child);
                    }
                    if (child instanceof HBox) { // avatar wrapper
                        for (javafx.scene.Node subChild : ((HBox)child).getChildren()) {
                            if (subChild instanceof VBox && subChild.getUserData() != null && subChild.getUserData().equals(id)) {
                                return extractTextFromMessageBox((VBox)subChild);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private String extractTextFromMessageBox(VBox messageBox) {
        for (javafx.scene.Node n : messageBox.getChildren()) {
            if (n instanceof Text) {
                return ((Text)n).getText();
            } else if (n instanceof Button && ((Button)n).getText().startsWith("Завантажити")) {
                return "[Фото/Файл]";
            } else if (n instanceof ImageView) {
                return "[Фото]";
            }
        }
        return "Повідомлення";
    }

    private void requestDelete(Long id) {
        Message msg = new Message(MessageType.DELETE_MESSAGE, NetworkClient.getInstance().getUsername());
        msg.setId(id);
        NetworkClient.getInstance().sendMessage(msg);
    }

    private void setReplyTo(Message msg) {
        this.replyingTo = msg;
        replyLabel.setText("Відповідь: " + (msg.getType() == MessageType.FILE ? msg.getFileName() : msg.getText()));
        replyBox.setVisible(true);
        replyBox.setManaged(true);
    }

    @FXML
    public void cancelReply() {
        this.replyingTo = null;
        replyBox.setVisible(false);
        replyBox.setManaged(false);
    }

    private void downloadFile(Message msg) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(msg.getFileName());
        File file = fileChooser.showSaveDialog(inputField.getScene().getWindow());
        if (file != null) {
            try {
                byte[] data = Base64.getDecoder().decode(msg.getFileBase64());
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // --- Custom ListCell for Contacts ---
    class ContactCell extends ListCell<UserDTO> {
        public ContactCell() {
            super();
            // Context Menu
            ContextMenu contextMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Видалити користувача з БД");
            deleteItem.setStyle("-fx-text-fill: red;");
            deleteItem.setOnAction(e -> {
                UserDTO u = getItem();
                if (u != null) {
                    Message m = new Message(MessageType.DELETE_USER, NetworkClient.getInstance().getUsername());
                    m.setText(u.getUsername());
                    NetworkClient.getInstance().sendMessage(m);
                }
            });
            contextMenu.getItems().add(deleteItem);
            this.setContextMenu(contextMenu);
        }

        @Override
        protected void updateItem(UserDTO item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null && !empty) {
                HBox box = new HBox(10);
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                StackPane avatarPane = new StackPane();
                Circle circle = new Circle(15, javafx.scene.paint.Color.web("#4a76a8"));
                Label letter = new Label(item.getUsername().substring(0, 1).toUpperCase());
                letter.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                ImageView imgView = new ImageView();
                imgView.setFitWidth(30); imgView.setFitHeight(30);
                imgView.setClip(new Circle(15, 15, 15));
                if (avatarCache.containsKey(item.getUsername())) {
                    imgView.setImage(avatarCache.get(item.getUsername()));
                    avatarPane.getChildren().addAll(circle, imgView);
                } else {
                    avatarPane.getChildren().addAll(circle, letter);
                }
                Label name = new Label(item.getUsername() + (pinnedUsers.contains(item.getUsername()) ? " 📌" : ""));
                box.getChildren().addAll(avatarPane, name);
                setGraphic(box);
                setText(null);
            } else {
                setText(null);
                setGraphic(null);
            }
        }
    }
}
