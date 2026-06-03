package org.example.protocol;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "userDTO")
@XmlAccessorType(XmlAccessType.FIELD)
public class UserDTO {
    @XmlElement
    private String username;
    
    @XmlElement
    private String avatarBase64;

    public UserDTO() {}
    
    public UserDTO(String username, String avatarBase64) {
        this.username = username;
        this.avatarBase64 = avatarBase64;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarBase64() { return avatarBase64; }
    public void setAvatarBase64(String avatarBase64) { this.avatarBase64 = avatarBase64; }
}
