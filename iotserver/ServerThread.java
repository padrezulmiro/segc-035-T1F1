package iotserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import iotclient.MessageCode;
import iohelper.FileHelper;

public class ServerThread extends Thread {
    private static final String IMAGE_DIR_PATH = "./output/server/img/";

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ServerManager manager;
    private String userID;
    private String deviceID;
    private boolean isRunning;

    public ServerThread(Socket socket, String keystorePath, String keystorePwd,
            String apiKey) {
        this.socket = socket;
        this.userID = null;
        this.deviceID = null;
        this.isRunning = true;
    }

    public void run() {
        System.out.println("Accepted connection!");

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            manager = ServerManager.getInstance();

            while(isRunning){
                MessageCode opcode = (MessageCode) in.readObject();
                switch (opcode) {
                    case AU:
                        authUser();
                        break;
                    case AD:
                        authDevice();
                        break;
                    case TD:
                        attestClient();
                        break;
                    case CREATE:
                        createDomain();
                        break;
                    case ADD:
                        addUserToDomain();
                        break;
                    case RD:
                        registerDeviceInDomain();
                        break;
                    case ET:
                        registerTemperature();
                        break;
                    case EI:
                        registerImage(userID,deviceID);
                        break;
                    case RT:
                        getTemperatures();
                        break;
                    case RI:
                        getImage();
                        break;
                    case STOP:
                        stopThread();
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

    private void stopThread() {
        System.out.println("Client quits. killing thread");
        manager.disconnectDevice(this.userID, this.deviceID);
        isRunning = false;
    }

    private void getImage() throws IOException, ClassNotFoundException {
        String targetUser = (String)in.readObject();
        String targetDev = (String)in.readObject();
        ServerResponse sr = manager.getImage(this.userID,targetUser,targetDev,IMAGE_DIR_PATH);
        MessageCode rCode=sr.responseCode();
        // Send code to client
        out.writeObject(rCode);
        // Send file (if aplicable)
        if( rCode == MessageCode.OK){
            out.writeObject(sr.encryptedDomainKey());
            FileHelper.sendFile(sr.filePath(), out);
        }
    }

    private void getTemperatures() throws IOException, ClassNotFoundException {
        String domain = (String) in.readObject();
        ServerResponse sr = manager.getTemperatures(this.userID,domain);
        MessageCode res = sr.responseCode();
        out.writeObject(res);
        if(res==MessageCode.OK){
            // FileHelper.sendFile(sResponse.filePath(),out);
            out.writeObject(sr); 
        }
    }

    private void registerImage(String devUID, String devDID)
                throws IOException, ClassNotFoundException {
        String filename = devUID + "_" + devDID + ".jpg"; //(String)in.readObject(); // screw this
        long fileSize = (long)in.readObject();
        String fullImgPath = IMAGE_DIR_PATH + filename;

        FileHelper.receiveFile(fileSize, fullImgPath, in);

        MessageCode res = manager
            .registerImage(filename, this.userID, this.deviceID)
            .responseCode();
        out.writeObject(res);
    }

    private void registerTemperature() throws IOException, ClassNotFoundException {
        String tempStr = (String) in.readObject();
        float temperature;
        try {
            temperature = Float.parseFloat(tempStr);
        } catch (NumberFormatException e) {
            out.writeObject(new ServerResponse(MessageCode.NOK));
            out.flush();
            return;
        }

        MessageCode res = manager
            .registerTemperature(temperature, this.userID, this.deviceID)
            .responseCode();
        out.writeObject(res);
        out.flush();
    }

    private void registerDeviceInDomain() throws IOException, ClassNotFoundException {
        String domain = (String)in.readObject();
        MessageCode res = manager.registerDeviceInDomain(domain, this.userID, this.deviceID).responseCode();
        out.writeObject(res);
    }

    private void addUserToDomain() throws IOException, ClassNotFoundException {
        String newUser = (String)in.readObject();
        String domain = (String)in.readObject();
        String enDomkey = (String)in.readObject();
        MessageCode res = manager.addUserToDomain(userID, newUser, domain, enDomkey).responseCode();
        out.writeObject(res);
    }

    private void createDomain() throws IOException, ClassNotFoundException {
        String domain = (String)in.readObject();
        MessageCode res = manager.createDomain(userID,domain).responseCode();
        out.writeObject(res);
    }

    private void attestClient() throws IOException, ClassNotFoundException {
        String fileName = (String)in.readObject();
        long fileSize = (long)in.readLong();
        MessageCode res = manager
            .attestClient(fileName, fileSize)
            .responseCode();
        out.writeObject(res);
        // System.out.println("Correct " + res + "filename=" + fileName + " size=" + fileSize);
    }

    private void authDevice() throws IOException, ClassNotFoundException {
        String deviceID = (String)in.readObject();
        MessageCode res = manager.authenticateDevice(userID,deviceID).responseCode();
        if(res == MessageCode.OK_DEVID){this.deviceID = deviceID;}
        out.writeObject(res);
    }

    private void authUser() throws IOException, ClassNotFoundException {
        if (this.userID==null){
            this.userID = (String) in.readObject();
        }
        String pwd = (String) in.readObject();
        out.writeObject(manager.authenticateUser(userID).responseCode());
    }
}
