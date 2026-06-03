package org.example.protocol;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@XmlRootElement(name = "message")
@XmlAccessorType(XmlAccessType.FIELD)
public class Message implements Serializable {
    
    @XmlElement
    private Long id;
    
    @XmlElement(required = true)
    private MessageType type;
    
    @XmlElement(required = true)
    private String sender;
    
    @XmlElement
    private String receiver;
    
    @XmlElement
    private String text;
    
    @XmlElement
    private Long replyToId;
    
    @XmlElement
    private String fileName;
    
    @XmlElement
    private String fileBase64;
    
    @XmlElementWrapper(name = "contactUsers")
    @XmlElement(name = "user")
    private java.util.List<UserDTO> contactUsers;
    
    @XmlElementWrapper(name = "historyMessages")
    @XmlElement(name = "historyMessage")
    private java.util.List<Message> historyMessages;
    
    @XmlElement
    private Date timestamp;

    public Message() {
        this.timestamp = new Date();
    }
    
    public Message(MessageType type, String sender) {
        this.type = type;
        this.sender = sender;
        this.timestamp = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Long getReplyToId() { return replyToId; }
    public void setReplyToId(Long replyToId) { this.replyToId = replyToId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileBase64() { return fileBase64; }
    public void setFileBase64(String fileBase64) { this.fileBase64 = fileBase64; }

    public java.util.List<UserDTO> getContactUsers() { return contactUsers; }
    public void setContactUsers(java.util.List<UserDTO> contactUsers) { this.contactUsers = contactUsers; }

    public java.util.List<Message> getHistoryMessages() { return historyMessages; }
    public void setHistoryMessages(java.util.List<Message> historyMessages) { this.historyMessages = historyMessages; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
