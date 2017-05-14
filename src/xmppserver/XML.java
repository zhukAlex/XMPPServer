/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xmppserver;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import javax.xml.transform.TransformerConfigurationException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 *
 * @author Алексей
 */
public class XML {
    public static String documentToString(Document doc){
        XMLOutputter serializer = new XMLOutputter();
        return serializer.outputString(doc);
    }
    
    public static Document createDocument(byte [] a) throws JDOMException, IOException, TransformerConfigurationException {
        SAXBuilder builder = new SAXBuilder();
        InputStream stream = new ByteArrayInputStream(a);
        Document doc = builder.build(stream);
        return doc;
    }
    
    public static Document readMessage(Socket socket)throws JDOMException, IOException, TransformerConfigurationException{
        InputStream sin = socket.getInputStream();
        DataInputStream in = new DataInputStream(sin);
       
        int size = 0;
        size = in.readInt();
        byte[] array = new byte[size];
        for(int i = 0; i < size; i++)
            array[i] = in.readByte();
        return XML.createDocument(array);
    }
        
}
