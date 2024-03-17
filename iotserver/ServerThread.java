package iotserver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Scanner;

import iotclient.MessageCode;

public class ServerThread extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    
    // user/domain files
    private File userRecord;
    private Scanner userScanner;
    private FileWriter userWriter;

    private File domainRecord;
    private Scanner domainScanner;
    private FileWriter domainWriter;

    public ServerThread(Socket socket) {
        this.socket = socket;
        
    }

    public void run() { 
        System.out.println("Accepted connection!");

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream()); //socket is closed here

            userRecord = initializeFile("user.txt");
            userScanner = new Scanner(userRecord);
            userWriter = new FileWriter(userRecord);

            domainRecord = initializeFile("domain.txt");
            domainScanner = new Scanner(domainRecord);
            domainWriter = new FileWriter(domainRecord);

            while(true){
                MessageCode opcode = (MessageCode) in.readObject();
                switch (opcode) {
                    case AU:
                        String user = (String)in.readObject();
                        String pwd = (String)in.readObject();
                        out.writeObject(userAuth(user,pwd));
                        break;
                    case AD:
                        String deviceID = (String)in.readObject();
                        break;
                    case CREATE:
                        break;
                    case ADD:
                        break;
                    case RD:
                        break;
                    case ET:
                        break;
                    case EI:
                        break;
                    case RT:
                        break;
                    case RI:
                        break;
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private void authenticateUser(){
        
    }

    private void writeMessage(MessageCode message) throws IOException {
        out.writeObject(message);
    }

    private MessageCode readMessage()throws IOException, ClassNotFoundException {
        MessageCode recievedCode = (MessageCode)in.readObject();
        return recievedCode;
    }
    
    private File initializeFile(String filename) throws IOException{
        File fileCreated = new File(filename);
        if (fileCreated.createNewFile()) {
            System.out.println("File created: " + fileCreated.getName());
        }
        return fileCreated;
    }

    private synchronized MessageCode userAuth(String user, String pwd) throws IOException{
        while(userScanner.hasNextLine()){
            String[] userpair = userScanner.nextLine().split(":");
            if(userpair[0].equals(user)){
                if (userpair[1].equals(pwd)){
                    return MessageCode.OK_USER;
                }else{
                    return MessageCode.WRONG_PWD;
                }
            }
        }
        userWriter.write(user+":"+pwd+"\n");
        return MessageCode.OK_NEW_USER;
    }
}
