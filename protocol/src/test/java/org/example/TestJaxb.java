package org.example;

import org.example.protocol.Message;
import org.example.protocol.MessageType;
import org.example.protocol.UserDTO;
import org.example.protocol.XmlUtil;

import java.util.ArrayList;
import java.util.List;

public class TestJaxb {
    public static void main(String[] args) throws Exception {
        Message msg = new Message(MessageType.CONTACT_LIST, "Server");
        List<UserDTO> dtos = new ArrayList<>();
        dtos.add(new UserDTO("Denis", null));
        dtos.add(new UserDTO("Dasha", null));
        msg.setContactUsers(dtos);

        String xml = XmlUtil.marshal(msg);
        System.out.println("Marshaled: " + xml);

        Message unmarshaled = XmlUtil.unmarshal(xml);
        System.out.println("Unmarshaled contactUsers: " + unmarshaled.getContactUsers());
        if (unmarshaled.getContactUsers() != null) {
            for (UserDTO u : unmarshaled.getContactUsers()) {
                System.out.println("User: " + u.getUsername());
            }
        }
    }
}
