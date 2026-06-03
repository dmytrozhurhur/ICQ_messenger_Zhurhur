package org.example.protocol;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.io.StringWriter;

public class XmlUtil {
    
    private static JAXBContext context;
    
    static {
        try {
            context = JAXBContext.newInstance(Message.class, UserDTO.class);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
    
    public static String marshal(Message message) throws JAXBException {
        Marshaller marshaller = context.createMarshaller();
        // Do not format output so it stays on a single line (easier to read via socket readLine)
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(message, writer);
        return writer.toString();
    }
    
    public static Message unmarshal(String xml) throws JAXBException {
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StringReader reader = new StringReader(xml);
        return (Message) unmarshaller.unmarshal(reader);
    }
}
