package org.example.server.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.example.protocol.Message;

import java.util.Base64;
import java.util.List;

public class DatabaseManager {
    private static SessionFactory sessionFactory;

    static {
        try {
            sessionFactory = new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static void saveUser(String username) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            UserEntity user = session.get(UserEntity.class, username);
            if (user == null) {
                user = new UserEntity(username);
                session.persist(user);
            }
            tx.commit();
        }
    }

    public static java.util.List<org.example.protocol.UserDTO> getAllUserDTOs() {
        try (Session session = sessionFactory.openSession()) {
            java.util.List<UserEntity> users = session.createQuery("from UserEntity", UserEntity.class).list();
            java.util.List<org.example.protocol.UserDTO> dtos = new java.util.ArrayList<>();
            for (UserEntity u : users) {
                String avatar = u.getAvatarData() != null ? java.util.Base64.getEncoder().encodeToString(u.getAvatarData()) : null;
                dtos.add(new org.example.protocol.UserDTO(u.getUsername(), avatar));
            }
            return dtos;
        }
    }

    public static void saveUserAvatar(String username, byte[] avatarData) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            UserEntity user = session.get(UserEntity.class, username);
            if (user != null) {
                user.setAvatarData(avatarData);
                session.merge(user);
            }
            tx.commit();
        }
    }

    public static void deleteUser(String username) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            UserEntity user = session.get(UserEntity.class, username);
            if (user != null) {
                session.remove(user);
            }
            session.createMutationQuery("delete from MessageEntity where sender = :u or receiver = :u")
                   .setParameter("u", username)
                   .executeUpdate();
            tx.commit();
        }
    }

    public static List<Message> getHistory(String user1, String user2) {
        try (Session session = sessionFactory.openSession()) {
            List<MessageEntity> entities = session.createQuery(
                "from MessageEntity m where (m.sender = :u1 and m.receiver = :u2) or (m.sender = :u2 and m.receiver = :u1) order by m.timestamp asc", 
                MessageEntity.class)
                .setParameter("u1", user1)
                .setParameter("u2", user2)
                .list();
            
            List<Message> result = new java.util.ArrayList<>();
            for (MessageEntity e : entities) {
                try {
                    org.example.protocol.MessageType type = e.getType() != null ? org.example.protocol.MessageType.valueOf(e.getType()) : org.example.protocol.MessageType.TEXT;
                    Message m = new org.example.protocol.Message(type, e.getSender());
                    m.setId(e.getId());
                    m.setReceiver(e.getReceiver());
                    m.setText(e.getText());
                    m.setReplyToId(e.getReplyToId());
                    m.setFileName(e.getFileName());
                    if (e.getFileData() != null) {
                        m.setFileBase64(Base64.getEncoder().encodeToString(e.getFileData()));
                    }
                    m.setTimestamp(e.getTimestamp());
                    result.add(m);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return result;
        }
    }

    public static MessageEntity saveMessage(Message msg) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            MessageEntity entity = new MessageEntity();
            entity.setType(msg.getType().name());
            entity.setSender(msg.getSender());
            entity.setReceiver(msg.getReceiver());
            entity.setText(msg.getText());
            entity.setReplyToId(msg.getReplyToId());
            entity.setTimestamp(msg.getTimestamp());
            if (msg.getFileName() != null && msg.getFileBase64() != null) {
                entity.setFileName(msg.getFileName());
                byte[] data = Base64.getDecoder().decode(msg.getFileBase64());
                entity.setFileData(data);
            }
            session.persist(entity);
            tx.commit();
            return entity;
        }
    }

    public static void deleteMessageById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            MessageEntity entity = session.get(MessageEntity.class, id);
            if (entity != null) {
                session.remove(entity);
            }
            tx.commit();
        }
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}
