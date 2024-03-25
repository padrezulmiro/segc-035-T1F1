package iotserver;

import iohelper.FileHelper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import iotclient.MessageCode;

public class ServerThread extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ServerManager manager;
    private String userID;
    private String deviceID;
    private boolean isRunning;

    public ServerThread(Socket socket) {
        this.socket = socket;
        this.userID = null;
        this.deviceID = null;
        this.isRunning = true;
    }

    public void run() { 
        System.out.println("Accepted connection!");

        try {

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream()); //socket is closed here
            manager = ServerManager.getInstance();
            MessageCode res;

            while(isRunning){
                MessageCode opcode = (MessageCode) in.readObject();
                switch (opcode) {
                    case AU:
                        if (this.userID==null){
                            this.userID = (String)in.readObject();
                        }
                        String pwd = (String)in.readObject();
                        out.writeObject(manager.authenticateUser(userID,pwd).responseCode());
                        break;
                    case AD:
                        String deviceID = (String)in.readObject();
                        res = manager.authenticateDevice(userID,deviceID).responseCode();
                        if(res == MessageCode.OK_DEVID){this.deviceID = deviceID;}
                        out.writeObject(res);
                        break;
                    case TD:
                        String fileName = (String)in.readObject();
                        long fileSize = (long)in.readLong();
                        res = manager.testDevice(fileName, fileSize).responseCode();
                        out.writeObject(res);
                        // System.out.println("Correct " + res + "filename=" + fileName + " size=" + fileSize);
                        break;
                    case CREATE:
                        String domain = (String)in.readObject();
                        res = manager.createDomain(userID,domain).responseCode();
                        out.writeObject(res);
                        break;
                    case ADD:
                        String newUser = (String)in.readObject();
                        domain = (String)in.readObject();
                        res = manager.addUserToDomain(userID, newUser, domain).responseCode();
                        out.writeObject(res);
                        break;
                    case RD:
                        domain = (String)in.readObject();
                        res = manager.registerDeviceInDomain(domain, this.userID, this.deviceID).responseCode();
                        out.writeObject(res);
                        break;
                    case ET:
                        String tempStr = (String)in.readObject();
                        res = manager.registerTemperature(tempStr,this.userID,this.deviceID).responseCode();
                        out.writeObject(res);
                        break;
                    case EI:
                        // input image stream
                        String filename = (String)in.readObject();
                        fileSize = (long)in.readObject();
                        res = manager.registerImage(filename,fileSize,in,this.userID,this.deviceID).responseCode(); 
                        out.writeObject(res);
                        break;
                    case RT:
                        domain = (String)in.readObject();
                        ServerResponse sResponse = manager.getTemperatures(this.userID,domain);
                        //getfilestream write it?
                        res = sResponse.responseCode();
                        out.writeObject(res);
                        if(res==MessageCode.OK){
                            // FileHelper.sendFile(sResponse.filePath(),out);
                            out.writeObject(sResponse.temperatures());
                        }
                        break;
                    case RI:
                        String targetUser = (String)in.readObject();
                        String targetDev = (String)in.readObject();
                        sResponse = manager.getImage(this.userID,targetUser, targetDev);
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
