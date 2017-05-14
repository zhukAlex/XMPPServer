/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xmppserver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.stage.Stage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/**
 *
 * @author Алексей
 */
public class XMPPServer extends Application implements Serializable{
    
    private static final long serialVersionUID = 1155176272028312155L;
    private static ArrayList<User> users = new ArrayList<User>();
    private static ArrayList<String> usersName = new ArrayList<String>();
    private static ArrayList<Thread> threads = new ArrayList<Thread>();
    DataInputStream in;
    DataOutputStream out;
    private String lastLogin = null;
    private final int MAXUSERCOUNT = 2;
    
    private void initTest() throws IOException{
        User user1 = addUser("login1", "password");
        User user2 = addUser("login2", "password");
        User user3 = addUser("login3", "password");
        
        if(user1 != null && user2 != null && user3 != null){
            user1.addFriend( user2.getLogin() );
            user1.addFriend( user3.getLogin() );

            user2.addFriend( user1.getLogin() );
            user2.addFriend( user3.getLogin() );

            user3.addFriend( user1.getLogin() );
            user3.addFriend( user2.getLogin() );
            
            user1.serialize();
            user2.serialize();
            user3.serialize();
        }
    }
    
    @Override
    public void start(Stage primaryStage) throws IOException, IOException, FileNotFoundException, ClassNotFoundException, TransformerConfigurationException {
        int port = 51000;
        initTest();
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        Socket socket = null; 
        try{
            System.out.print("ip: ");
            String ip = "localhost";
            
            ServerSocket ss = new ServerSocket((port), 0, InetAddress.getByName(ip)); 
            System.out.println("Server wait..."); 
            while(true){
                
                socket = ss.accept();   
                socket.setSoTimeout(0);
                InputStream sin = socket.getInputStream();
                OutputStream sout = socket.getOutputStream();
                in = new DataInputStream(sin);
                out = new DataOutputStream(sout);
                if( users.size() < MAXUSERCOUNT){
                    if( auth( XML.readMessage(socket) ) ){

                        System.out.println(lastLogin + "создан на сервере.");

                        User user = searchUser(lastLogin);
                        sendXml( authSuccess(user) );
                        user.setSocket(socket);
                        Thread thread = new Thread( user );
                        thread.start();
                        threads.add(thread);
                        usersName.add(lastLogin);
                        users.add(user);
                    }
                } else{
                    sendXml( errorCount() );
                }       
            }
        } catch (UnknownHostException ex) {
            Logger.getLogger(XMPPServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(XMPPServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JDOMException ex) {
            Logger.getLogger(XMPPServer.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
    }
    
    private Document errorCount(){
        Document doc = new Document();
        Element element = new Element("Stream", Namespace.getNamespace("XMPP 1.0"));
    
        element.addContent(new Element("errorCount", Namespace.getNamespace("XMPP 1.0")));
            
        doc.addContent(element);
        
        return doc;
    }
    
    private boolean auth(Document doc) throws IOException, FileNotFoundException, ClassNotFoundException{
        boolean result = false;
        Element root = doc.getRootElement();
        if( root.getChildText("auth", Namespace.getNamespace("XMPP 1.0")) != null)
            if( verifyUser(root.getAttributeValue("login"), root.getAttributeValue("password")) ){
                lastLogin = root.getAttributeValue("login");
                result = true;
            }  
            else 
                sendXml( authError() );
        
        return result;
    }
    public void stopThread(String login){
        int index = searchIndexUser(login);
        
        if(index != -1){
            usersName.remove(index);
            users.remove(index);
        }   
    }

    public void sendMessage(String message, String to, String from) throws IOException{
        int index = searchIndexUser(to);
        if(index != -1){
            users.get(index).send(message, to, from);
            users.get( searchIndexUser(from) ).sendAck();
        }   
        else users.get( searchIndexUser(from) ).sendError();
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    private boolean verifyUser(Document doc) throws IOException, JDOMException, TransformerConfigurationException{
        boolean result = false;
        
        Element root = doc.getRootElement();
        System.out.println( root.getChildText("from", Namespace.getNamespace("XMPP 1.0")) );
        return result;
    }
    
    private Document authSuccess(User user){
        Document doc = new Document();
        Element element = new Element("Stream", Namespace.getNamespace("XMPP 1.0"));
    
        element.addContent(new Element("authSuccess", Namespace.getNamespace("XMPP 1.0")));
        
        ArrayList <String> friends = user.getFriends();
        for (String s : friends)
            element.addContent( new Element("friend", Namespace.getNamespace("XMPP 1.0")).setText(s) );
            
        doc.addContent(element);
        
        return doc;
    }
    
    private Document authError(){
        Document doc = new Document();
        Element element = new Element("Stream", Namespace.getNamespace("XMPP 1.0"));
    
        element.addContent(new Element("authError", Namespace.getNamespace("XMPP 1.0")));
        doc.addContent(element);
        
        return doc;
    }
    
    public void requestOnline(User user) throws IOException{
       
    }
    
    
    private void sendXml(Document doc) throws FileNotFoundException, IOException{ 
        byte[] array = XML.documentToString(doc).getBytes("UTF-8");
        out.writeInt(array.length);
        for(byte a : array)
            out.write(a);
        out.flush();
      
        System.out.println(XML.documentToString(doc));   
    }
    
    private User addUser(String login, String password) throws IOException{
        User user;
        if( !new File("Users/" + login + ".dat").exists() ){
            user = new User(login, password, this);
            user.serialize();
        }
        else 
            user = null;
        
        return user;
    }
    
    private User searchUser(String login) throws IOException, FileNotFoundException, ClassNotFoundException{
        User user;
        if( new File("Users/" + login + ".dat").exists() ){
            user = User.UserRead(login);
        }
        else 
            user = null;
        
        return user;
    }
    
    private int searchIndexUser(String login){
        int index = -1;
        int i = 0;
        
        for(String s : usersName){
            if( s.equals(login) ){
                index = i;
                break;
            }
            i++;
        }
        
        return index;
    }
    
    private boolean verifyUser(String login, String password) throws FileNotFoundException, IOException, ClassNotFoundException{
        User user;
        boolean result = false;
        
        if( new File("Users/" + login + ".dat").exists() ){
            result = User.UserRead(login).verifyPassword(password);
        }
        
        return result;
    }
    
    private boolean addFriend(User user, String login){
        boolean result = false;
        
        if( new File("Users/" + login + ".dat").exists() && !user.searchFriend(login) ){
            user.addFriend(login);
            result = true;
        }
        
        return result;
    }
    
    private boolean deleteFriend(User user, String login){
        boolean result = false;
        
        if( user.searchFriend(login) ){
            user.deleteFriend(login);
            result = true;
        }
        
        return result;
    }
    
    public ArrayList<String> getOnlineForUser(User user){
        ArrayList<String> friendsOnline = new ArrayList<String>();
        
        for(String u : usersName){
            if( user.searchFriend(u) ){
                friendsOnline.add(u);
            }
        }
        
        return friendsOnline;
    }
    
}
