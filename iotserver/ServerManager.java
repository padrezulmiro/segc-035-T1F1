package iotserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import iotclient.MessageCode;

public class ServerManager {
    private static volatile ServerManager instance;

    public static Map<String, String> USERS;
    public static Map<String, Domain> DOMAINS;
    public static Map<String, Device> DEVICES;

    private static final String domainFilePath = "domain.txt";
    private static final String userFilePath = "user.txt";

    //TODO: fill this in?
    private static String clientFileName;
    private static Long clientFileSize;

    // user/domain files
    private static File userRecord;
    // private static Scanner userScanner;
    private static BufferedReader userReader;
    private static BufferedWriter userWriter;

    private static File domainRecord;
    // private static Scanner domainScanner;
    private static BufferedWriter domainWriter;

    

    private ServerManager(){
        // check if the files exists. if not, create the files
        USERS = new HashMap<>();
        DOMAINS = new HashMap<>();
        DEVICES = new HashMap<>();
        // else: read from the files and populate DOMAINS/DEVICES
    }

    public static ServerManager getInstance(){
        // thread calls this to get the db
        ServerManager res = instance;
        if(res != null){
            return res;
        }

        synchronized(ServerManager.class) {
            if (instance == null) {
                try {
                    instance = new ServerManager();

                    File clientFile = new File("./bin/iotclient/IoTDevice.class");
                    // File clientDataFile = new File(clientDataPath);
                    clientFileName = clientFile.getName();
                    clientFileSize = clientFile.length();
                    // FileWriter wr = new FileWriter(clientDataFile);
                    // wr.write(clientFile.getName());
                    // wr.write( Long.toString(clientFile.length()));
                    // wr.close();

                    userRecord = initializeFile(userFilePath);
                    userReader = new BufferedReader(new FileReader(userRecord));
                    userWriter = new BufferedWriter(new FileWriter(userRecord,true));
                    // why would this empty it all.
                    // ref: https://stackoverflow.com/questions/17244713/using-filewriter-and-bufferedwriter-clearing-file-for-some-reason
                    readUsersFile();
                    userReader.close();
                    
                    domainRecord = initializeFile(domainFilePath);
                    domainWriter = new BufferedWriter(new FileWriter(domainRecord,true));
                    readDomainsFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return instance;
        }
    }
    
    /*
     * CLIENT COMMANDS====================================================================================================================
     */
    synchronized public ServerResponse createDomain(String clientUID, String domainName){
        if (domainExists(domainName)) {
            return new ServerResponse(MessageCode.NOK);
        }
        Domain domain = new Domain(domainName, clientUID);
        ServerManager.DOMAINS.put(domainName, domain); 
        updateDomainsFile();

        return new ServerResponse(MessageCode.OK);

    }

    synchronized public ServerResponse addUserToDomain(String ownerUId, String newUserId, String domainName) {
        if (!domainExists(domainName)) {
            return new ServerResponse(MessageCode.NODM);
        }

        Domain domain = DOMAINS.get(domainName);

        if (!domain.isOwner(ownerUId)) {
            return new ServerResponse(MessageCode.NOPERM);
        }

        if (domain.registerUser(newUserId)) {
            // update DOMAINS
            ServerManager.DOMAINS.replace(domain.getName(),domain);
            updateDomainsFile();
            return new ServerResponse(MessageCode.OK);
        } else {
            // XXX is this the correct implementation?
            return new ServerResponse(MessageCode.USEREXISTS);
        }
    }

    // devID being ID
    synchronized public ServerResponse registerDeviceInDomain(String userId, String domainName, String devId) {
        if (!domainExists(domainName)) {
            return new ServerResponse(MessageCode.NODM);
        }

        Domain domain = DOMAINS.get(domainName);
        if (!domain.isRegistered(userId)) {
            return new ServerResponse(MessageCode.NOPERM);
        }

        String fullDevId = userId + ":" + devId;
        ServerManager.DEVICES.get(fullDevId).registerInDomain(domainName); // XXX: i dont like this :|
        domain.registerDevice(fullDevId);
        ServerManager.DOMAINS.replace(domainName,domain);
        updateDomainsFile();
        return new ServerResponse(MessageCode.OK);
    }


    synchronized public ServerResponse registerTemperature(String temperatureString, String device) {
        float temperature;
        try {
            temperature = Float.parseFloat(temperatureString);
        } catch (NumberFormatException e) {
            return new ServerResponse(MessageCode.NOK);
        }

        ServerManager.DEVICES.get(device).registerTemperature(temperature);
        return new ServerResponse(MessageCode.OK);
    }

    synchronized public ServerResponse registerImage(InputStream imageStream) {
        throw new UnsupportedOperationException();
    }

    synchronized public ServerResponse getTemperatures(String domainName) {
        throw new UnsupportedOperationException();
    }

    synchronized public ServerResponse getImage(String targetUserId, String targetDevId) {
        throw new UnsupportedOperationException();
    }

    synchronized private boolean domainExists(String domainName) {
        return DOMAINS.containsKey(domainName);
    }

    /*
     * I/O FILE HANDLING====================================================================================================================
     */

    public static File initializeFile(String filename) throws IOException{
        File fileCreated = new File(filename);
        if(!fileCreated.exists() ){
            fileCreated.createNewFile();
            System.out.println("File created: " + fileCreated.getName());
        }
        // System.out.println("generated new file:" + fileCreated.getName());
        // if (!fileCreated.exists() && fileCreated.createNewFile()) {
        //     System.out.println("File created: " + fileCreated.getName());
        // }
        return fileCreated;
    }

    //should be called every time DOMAIN is changed
    synchronized public boolean updateDomainsFile(){
        // writes updated DOMAINS to file

        return true;
    }

    synchronized public boolean updateUsersFile() throws IOException{
        // writes updated USERS to file
        // userWriter.write(user+":"+pwd+"\n");
        // userWriter.flush();
        for(Map.Entry<String,String> entry : USERS.entrySet()){
           userWriter.write(entry.getKey()+":"+entry.getValue());
           userWriter.flush();
        }
        return true; 
    }

    synchronized public static void readDomainsFile() throws IOException{
        final String SP = ":";
        final char TAB = '\t';

        BufferedReader reader = new BufferedReader(new FileReader(domainRecord));
        String[] lines = (String[]) reader.lines().toArray();
        reader.close();

        String currentDomainName = null;
        String currentOwner = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean isDomainLine = line.charAt(0) != TAB;

            String[] tokens = line.split(SP);
            if (isDomainLine) {
                currentDomainName = tokens[0];
                currentOwner = tokens[1];
                instance.createDomain(currentOwner, currentDomainName);
            } else {
                String uid = tokens[0];
                String did = tokens[1];
                String fullId = uid + SP + did;
                String temperature = tokens[2];
                String imagePath = tokens[3];

                Device device = ServerManager.DEVICES.containsKey(fullId) ?
                    ServerManager.DEVICES.get(fullId) :
                    new Device(fullId);
                ServerManager.DEVICES.put(device.fullId(), device);

                instance.addUserToDomain(currentOwner, uid, currentDomainName);
                instance.registerDeviceInDomain(uid, currentDomainName, did);
                if (!temperature.equals("")) {
                    instance.registerTemperature(temperature, fullId);
                }
                if (!imagePath.equals("")) {
                    FileInputStream stream = new FileInputStream(imagePath);
                    instance.registerImage(stream);
                    stream.close();
                }
            }
        }
    }
    
    synchronized public static void readUsersFile() throws IOException{
        String line;
        while (( line = userReader.readLine()) != null) {
            String[]id = line.split(":");
            USERS.put(id[0],id[1]);
        }
    }

    /*
     *AUTHENTICATION====================================================================================================================
     */

    synchronized public void registerUser(String user, String pwd) throws IOException{
        USERS.put(user, pwd);
        updateUsersFile();
    }

    synchronized public void registerDevice(String fullDevId, Device dev) throws IOException{
        dev.goOnline();
        DEVICES.put(fullDevId,dev);
    }

    public synchronized ServerResponse authenticateUser(String user, String pwd)throws IOException{

        if (userExists(user)){
            if(USERS.get(user).equals(pwd)){
                return  new ServerResponse(MessageCode.OK_USER);
            }
                return  new ServerResponse(MessageCode.WRONG_PWD);
        }
        registerUser(user,pwd);
        return  new ServerResponse(MessageCode.OK_NEW_USER);
    }

    synchronized private boolean userExists(String userID) {
        return USERS.containsKey(userID);
    }

    //assumes userId exists
    public synchronized ServerResponse authenticateDevice(String userId, String devId)throws IOException{
        String fullDevId = userId + ":" + devId;
        if(DEVICES.containsKey(fullDevId)){
            Device dev = DEVICES.get(fullDevId);
            System.out.println("devid:" + fullDevId);
            if(dev.isOnline()){
                System.out.println("dev is online");
                return new ServerResponse(MessageCode.NOK_DEVID);
            }else{
                registerDevice(fullDevId,dev);
                return new ServerResponse(MessageCode.OK_DEVID);
            }
        }
        Device dev = new Device(fullDevId);
        registerDevice(fullDevId,dev);
        return new ServerResponse(MessageCode.OK_DEVID);
    }

    public synchronized ServerResponse testDevice(String devFileName, long devFileSize)throws IOException{
        if (devFileName.equals(clientFileName) && devFileSize==clientFileSize){
            return new ServerResponse(MessageCode.OK_TESTED);
        }
        return new ServerResponse(MessageCode.NOK_TESTED);
    }
}
