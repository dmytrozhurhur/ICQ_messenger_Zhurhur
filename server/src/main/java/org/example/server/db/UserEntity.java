package org.example.server.db;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Lob;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    private String username;

    private byte[] avatarData;

    public UserEntity() {}
    public UserEntity(String username) { this.username = username; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public byte[] getAvatarData() { return avatarData; }
    public void setAvatarData(byte[] avatarData) { this.avatarData = avatarData; }
}
