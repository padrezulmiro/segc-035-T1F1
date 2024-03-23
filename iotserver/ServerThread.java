package iotserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


import iotclient.MessageCode;

public class ServerThread extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ServerManager manager;
    private String user = null;
    private String deviceID = null;
    private boolean isRunning = true;

    public ServerThread(Socket socket) {
        this.socket = socket;
        
    }

    public void run() { 
        System.out.println("Accepted connection!");

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream()); //socket is closed here
            manager = ServerManager.getInstance();

            while(isRunning){
                MessageCode opcode = (MessageCode) in.readObject();
                switch (opcode) {
                    case AU:
                        if (this.user==null){
                            this.user = (String)in.readObject();
                        }
                        String pwd = (String)in.readObject();
                        out.writeObject(manager.authenticateUser(user,pwd).responseCode());
                        break;
                    case AD:
                        String deviceID = (String)in.readObject();
                        out.writeObject(manager.authenticateDevice(user,deviceID).responseCode());
                        break;
                    case TD:
                        String fileName = (String)in.readObject();
                        Long fileSize = null;//in.readLong();
                        System.out.println("Correct");
                        out.writeObject(manager.testDevice(fileName, fileSize).responseCode());
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
                    case STOP:
                        System.out.println("Client quits. killing thread");
                        isRunning = false;
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
}
