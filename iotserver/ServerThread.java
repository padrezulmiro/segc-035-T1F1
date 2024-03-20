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

    public ServerThread(Socket socket) {
        this.socket = socket;
        
    }

    public void run() { 
        System.out.println("Accepted connection!");

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream()); //socket is closed here
            manager = ServerManager.getInstance();

            while(true){
                MessageCode opcode = (MessageCode) in.readObject();
                switch (opcode) {
                    case AU:
                        String user = (String)in.readObject();
                        String pwd = (String)in.readObject();
                        out.writeObject(manager.authenticateUser(user,pwd));
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
}
