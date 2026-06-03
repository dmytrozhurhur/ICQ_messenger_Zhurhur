package org.example.protocol;

public enum MessageType {
    AUTH,           // Authentication request/response
    TEXT,           // Normal text message
    FILE,           // File transfer
    DELETE_MESSAGE, // Request/Response to delete a message
    ERROR,          // Error message from server
    CONTACT_LIST,   // List of all contacts
    HISTORY,        // Request/Response for chat history
    VOICE,          // Voice message
    UPDATE_AVATAR,  // Update profile picture
    DELETE_USER     // Delete a user from DB
}
