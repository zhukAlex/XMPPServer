/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xmppserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerConfigurationException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;

/**
 *
 * @author Алексей
 */
public class User implements Serializable, Runnable {
    private static final long serialVersionUID = 3464971772192366239L;
    private boolean state = true;
    private String login;
    private String password;
    private ArrayList<String> friends;
    private InputStream sin;
    private OutputStream sout;
    private DataInputStream in;
    private DataOutputStream out;
    private Socket socket;
    private XMPPServer server;
    
    public User(String login, String password, XMPPServer server){
        this.login = login;
        this.password = password;
        this.server = server;
        this.friends = new ArrayList<String>();
    }
    
    public String getLogin(){
        return login;
    }
    
    public void setSocket(Socket socket) throws IOException{
        this.socket = socket;
        sin = socket.getInputStream();
        sout = socket.getOutputStream();
        in = new DataInputStream(sin);
        out = new DataOutputStream(sout);
    }
    
    public ArrayList<String> getFriends(){
        return friends;
    }
    
    private Document sendMessage(String message, String to, String from){
        Document doc = new Document();
        Element element = new Element("Stream", Namespace.getNamespace("XMPP 1.0"));
    
        element.addContent(new Element("message", Namespace.getNamespace("XMPP 1.0")).setText(message));
        element.setAttribute("from", from);
        element.setAttribute("to", to);
        doc.addContent(element);
        
        return doc;
    }
    
    private void sendXml(Document doc) throws FileNotFoundException, IOException{ 
        byte[] array = XML.documentToString(doc).getBytes("UTF-8");
        out.writeInt(array.length);
        for(byte a : array)
            out.write(a);
        out.flush();
      
        System.out.println(XML.documentToString(doc));   
    }
    
    public void send(String message, String to, String from) throws IOException{
        sendXml( sendMessage(message, to, from) );
    }
    
    public void sendError() throws IOException{
        Document doc = new Document();
        Element element = new Element("Stream", Namespace.getNamespace("XMPP 1.0"));
    
        element.addContent(new Element("error", Namespace.getNamespace("XMPP 1.0")));
        doc.addContent(element);
        
        sendXml(doc);
    }
    
    public void read() throws JDOMException, IOException, IOException, TransformerConfigurationException, FileNotFoundException, ClassNotFoundException{
        parseMessage( XML.readMessage(socket) );
    }
    
    
    private void parseMessage(Document doc) throws IOException, FileNotFoundException, ClassNotFoundException{
        Element root = doc.getRootElement();
        String text = null;
        String from = null;
        String to = null;

        if( root.getChildText("message", Namespace.getNamespace("XMPP 1.0")) != null ){
            from = root.getAttributeValue("from");
            to = root.getAttributeValue("to");
            text = root.getChildText(text,  Namespace.getNamespace("XMPP 1.0")); 
            server.sendMessage(text, to, from);
        }   
        else if( root.getChildText("close", Namespace.getNamespace("XMPP 1.0")) != null )
            state = false;
        else if( root.getChildText("online", Namespace.getNamespace("XMPP 1.0")) != null )
            sendXml( online(this) );
    }
    
    public void addFriend(String login){
        friends.add(login);
    }
    
    public boolean searchFriend(String login){
        return friends.contains(login);
    }
    
    public void deleteFriend(String login){
        friends.remove(login);
    }
    
    public void searchFriend(){
        
    }
    
    public boolean verifyPassword(String password){
        return this.password.equals(password);
    }
    
    public void serialize() throws FileNotFoundException, IOException{
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("Users/" + login + ".dat"));
        out.writeObject( this );
        out.close();
    }
    
    public static User UserRead(String login) throws FileNotFoundException, IOException, ClassNotFoundException{
        ObjectInputStream in = new ObjectInputStream( new FileInputStream("Users/" + login + ".dat") );
        User user = (User)in.readObject();
        in.close();
        return user;
    }

    @Override
    public void run() {
        state = true;
        while(state){ 
            try {
                read();
            } catch (JDOMException ex) {
                state = false;
                
                //  Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                state = false;               
// Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerConfigurationException ex) {
                state = false;
// Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                state = false;
//Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
        server.stopThread(login);
    }

    void sendAck() throws IOException {
        Document doc = new Document();
        Element element = new Element("Stream", Namespace.getNamespace("XMPP 1.0"));
    
        element.addContent(new Element("ack", Namespace.getNamespace("XMPP 1.0")));
        doc.addContent(element);
        
        sendXml(doc);
    }
    
    private Document online(User user){
        Document doc = new Document();
        Element element = new Element("Stream", Namespace.getNamespace("XMPP 1.0"));
    
        element.addContent(new Element("online", Namespace.getNamespace("XMPP 1.0")));
        
        ArrayList <String> friends = server.getOnlineForUser(user);
        
        if(!friends.isEmpty())
            for (String s : friends)
                element.addContent( new Element("friend", Namespace.getNamespace("XMPP 1.0")).setText(s) );
            
        
        doc.addContent(element);
        System.out.println(XML.documentToString(doc));
        return doc;
    }
    
    
}
