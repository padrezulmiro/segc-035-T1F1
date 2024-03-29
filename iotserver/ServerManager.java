package iotserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import iohelper.FileHelper;
import iotclient.MessageCode;

public class ServerManager {
    private static volatile ServerManager instance;

    // FIXME change these to private again and getters
    private static Map<String, String> USERS;
    private static Map<String, Domain> DOMAINS; 
    private static Map<String, Device> DEVICES;

    private static ReentrantReadWriteLock rwlDomains;
    private static Lock rlDomains;
    private static Lock wlDomains;
    private static ReentrantReadWriteLock rwlUsers;
    private static Lock rlUsers;
    private static Lock wlUsers;


    private static final String attestationFilePath = "attestation.txt";
    private static final String domainFilePath = "domain.txt";
    private static final String userFilePath = "user.txt";
    private static final String imageDirectoryPath = "./img/";
    private static final String temperatureDirectoryPath = "./temp/";
    private static final String clientFileName = "IoTDevice.jar";
    private static Long clientFileSize;

    // user/domain files
    private static File userRecord;
    private static File domainRecord;

    private ServerManager(){
        // check if the files exists. if not, create the files
        USERS = new HashMap<>();
        DOMAINS = new HashMap<>();
        DEVICES = new HashMap<>();
        // else: read from the files and populate DOMAINS/DEVICES

        rwlDomains = new ReentrantReadWriteLock(true);
        rlDomains = rwlDomains.readLock();
        wlDomains = rwlDomains.writeLock();

        rwlUsers = new ReentrantReadWriteLock(true);
        rlUsers = rwlUsers.readLock();
        wlUsers = rwlUsers.writeLock();
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

                    new File(imageDirectoryPath).mkdirs();
                    new File(temperatureDirectoryPath).mkdirs();

                    //register attestation value
                    File clientFile = new File(attestationFilePath);
                    BufferedReader cFileReader = new BufferedReader(new FileReader(clientFile));
                    clientFileSize = Long.parseLong(cFileReader.readLine());
                    cFileReader.close();

                    userRecord = initializeFile(userFilePath);
                    readUsersFile();

                    domainRecord = initializeFile(domainFilePath);
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
    public ServerResponse createDomain(String clientUID, String domainName){
        wlDomains.lock();
        try {
            if (domainExists(domainName)) {
                return new ServerResponse(MessageCode.NOK);
            }
            Domain domain = new Domain(domainName, clientUID);
            ServerManager.DOMAINS.put(domainName, domain);
            updateDomainsFile();

            return new ServerResponse(MessageCode.OK);
        } finally {
            wlDomains.unlock();
        }
    }

    public ServerResponse addUserToDomain(String ownderUID, String newUserID, String domainName) {
        wlDomains.lock();
        try {
            if (!domainExists(domainName)) {
                return new ServerResponse(MessageCode.NODM);
            }

            Domain domain = DOMAINS.get(domainName);

            rlUsers.lock();
            try {
                if (!userExists(newUserID)) {
                    return new ServerResponse(MessageCode.NOUSER);
                }
            } finally {
                rlUsers.unlock();
            }

            if (!domain.isOwner(ownderUID)) {
                return new ServerResponse(MessageCode.NOPERM);
            }

            if (domain.registerUser(newUserID)) {
                // update DOMAINS
                ServerManager.DOMAINS.replace(domain.getName(), domain);
                updateDomainsFile();
                return new ServerResponse(MessageCode.OK);
            } else {
                return new ServerResponse(MessageCode.USEREXISTS);
            }
        } finally {
            wlDomains.unlock();
        }
    }

    // devID being ID
    public ServerResponse registerDeviceInDomain(String domainName, String userId, String devId) {
        wlDomains.lock();
        try {
            if (!domainExists(domainName)) {
                return new ServerResponse(MessageCode.NODM);
            }

            Domain domain = DOMAINS.get(domainName);
            if (!domain.isRegistered(userId)) {
                return new ServerResponse(MessageCode.NOPERM);
            }

            String fullID = fullID(userId, devId);
            if (domain.isDeviceRegistered(fullID)) {
                return new ServerResponse(MessageCode.DEVICEEXISTS);
            }
            String fullDevId = fullID(userId, devId);
            ServerManager.DEVICES.get(fullDevId).registerInDomain(domainName);
            domain.registerDevice(fullDevId);
            updateDomainsFile();
            return new ServerResponse(MessageCode.OK);
        } finally {
            wlDomains.unlock();
        }
    }


    public ServerResponse registerTemperature(String temperatureString, String userId, String devId) {
        wlDomains.lock();
        try {
            float temperature;
            String fullDevId = fullID(userId, devId);
            try {
                temperature = Float.parseFloat(temperatureString);
            } catch (NumberFormatException e) {
                return new ServerResponse(MessageCode.NOK);
            }
            ServerManager.DEVICES.get(fullDevId).registerTemperature(temperature);
            updateDomainsFile();
            return new ServerResponse(MessageCode.OK);
        } finally {
            wlDomains.unlock();
        }
    }

    public ServerResponse registerImage(String filename, long filesize, ObjectInputStream in, String userId, String devId){
        wlDomains.lock();
        try {
            String fullDevId = fullID(userId, devId);
            String fullImgPath = imageDirectoryPath + filename;

            FileHelper.receiveFile(filesize, fullImgPath, in);

            ServerManager.DEVICES.get(fullDevId).registerImage(fullImgPath);
            return new ServerResponse(MessageCode.OK);
        } finally {
            wlDomains.unlock();
        }
    }

    public ServerResponse getTemperatures(String user, String domainName) throws IOException {
        rlDomains.lock();
        try {
            Domain dom = ServerManager.DOMAINS.get(domainName);
            if (dom == null) {
                return new ServerResponse(MessageCode.NODM);
            }

            if (dom.isRegistered(user)) {
                Map<String, Float> temps = getTempList(dom);
                return new ServerResponse(MessageCode.OK, temps);
            }

            return new ServerResponse(MessageCode.NOPERM);
        } finally {
            rlDomains.unlock();
        }
    }

    public ServerResponse getImage(String user,String targetUserId, String targetDevId) {
        rlDomains.lock();
        try {
            String targetDevFullId = fullID(targetUserId, targetDevId);
            Device dev = DEVICES.get(targetDevFullId);
            if (dev == null) {
                return new ServerResponse(MessageCode.NOID);
            }

            String filepath = dev.getImagePath();
            if (filepath == null) {
                return new ServerResponse(MessageCode.NODATA);
            }

            // if it's the device's own image, return file
            if (user.equals(targetUserId)) {
                return new ServerResponse(MessageCode.OK, filepath);
            }

            // if user isnt target device + does not exist in any of the target domain,
            // return NOPERM
            for (String dom : dev.getDomains()) {
                Domain domain = DOMAINS.get(dom);
                if (domain.isRegistered(user)) {
                    return new ServerResponse(MessageCode.OK, filepath);
                }
            }
            return new ServerResponse(MessageCode.NOPERM);

        } finally {
            rlDomains.unlock();
        }
    }

    private boolean domainExists(String domainName) {
        return DOMAINS.containsKey(domainName);
    }

    private Map<String,Float> getTempList(Domain domain){
        Set<String> devices = domain.getDevices();
        Map<String,Float> tempMap = new HashMap<String,Float>();
        for (String userStr : devices){
            Device dev = DEVICES.get(userStr);
            tempMap.put(userStr, dev.getTemperature());
        }
        return tempMap;
    }

    /*
     * I/O FILE HANDLING====================================================================================================================
     */

    public static File initializeFile(String filename) throws IOException{
        File fileCreated = new File(filename);
        if(!fileCreated.exists()) {
            fileCreated.createNewFile();
            System.out.println("File created: " + fileCreated.getName());
        }
        return fileCreated;
    }

    //should be called every time DOMAIN is changed
    private boolean updateDomainsFile(){
        // writes updated DOMAINS to file
        StringBuilder sb = new StringBuilder();
        for (Domain domain : ServerManager.DOMAINS.values()) {
            sb.append(domain.toString());
        }

        try (PrintWriter pw = new PrintWriter(domainRecord)) {
            pw.write(sb.toString());
            pw.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;
    }

    public boolean updateUsersFile() throws IOException{
        BufferedWriter userWriter = new BufferedWriter(new FileWriter(userRecord));
        for(Map.Entry<String,String> entry : USERS.entrySet()){
           userWriter.write(entry.getKey()+":"+entry.getValue()+System.getProperty ("line.separator"));
           userWriter.flush();
        }
        userWriter.close();
        return true; 
    }

    private static void readDomainsFile() throws IOException{
        final char SP = ':';
        final char TAB = '\t';

        BufferedReader reader = new BufferedReader(new FileReader(domainRecord));
        String[] lines = (String[]) reader.lines().toArray(String[]::new);
        reader.close();

        String currentDomainName = null;
        String currentOwner = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean isDomainLine = line.charAt(0) != TAB;
            String[] tokens = Utils.split(line, SP);

            if (isDomainLine) {
                currentDomainName = tokens[0];
                currentOwner = tokens[1];
                instance.populateDomain(tokens);
            } else {
                instance.populateDevices(tokens, currentDomainName, currentOwner);
            }
        }
    }

    private void populateDomain(String[] tokens) {
        String domainName = tokens[0];
        String owner = tokens[1];

        Domain domain = new Domain(domainName, owner);
        for (int j = 2; j < tokens.length; j++) {
            String user = tokens[j];
            domain.registerUser(user);
        }

        ServerManager.DOMAINS.put(domainName, domain);
    }
    
    private void populateDevices(String[] tokens, String domainName,
                                 String owner) {
        final char SP = ':';

        String uid = tokens[0];
        String did = tokens[1];
        String temperature = tokens[2];
        String imagePath = tokens[3];
        String fullId = uid + SP + did;

        Device device = ServerManager.DEVICES.containsKey(fullId) ?
            ServerManager.DEVICES.get(fullId) : new Device(fullId);

        if (!ServerManager.DEVICES.containsKey(fullId)) {
            ServerManager.DEVICES.put(device.fullId(), device);
        }

        Domain domain = ServerManager.DOMAINS.get(domainName);
        device.registerInDomain(domainName);
        domain.registerDevice(fullId);

        if (!temperature.equals("")) {
            device.registerTemperature(Float.parseFloat(temperature));
        }

        if (!imagePath.equals("")) {device.registerImage(imagePath);}
    }

    private static void readUsersFile() throws IOException{
        BufferedReader userReader = new BufferedReader(new FileReader(userRecord));
        // why would this empty it all.
        // ref: https://stackoverflow.com/questions/17244713/using-filewriter-and-bufferedwriter-clearing-file-for-some-reason
        String line;
        while (( line = userReader.readLine()) != null) {
            String[]id = line.split(":");
            USERS.put(id[0],id[1]);
        }
        userReader.close();
    }

    /*
     * UTILITY==================================================================
     */

    public static Device getDevice(String fullId){
        return ServerManager.DEVICES.get(fullId);
    }

    private static String fullID(String userId, String devId){
        return (userId+":"+devId);
    }

    /*
     *AUTHENTICATION====================================================================================================================
     */

    public void registerUser(String user, String pwd) throws IOException{
        USERS.put(user, pwd);
        updateUsersFile();
    }

    public void registerDevice(String fullDevId, Device dev) throws IOException{
        dev.goOnline();
        DEVICES.put(fullDevId,dev);
    }

    public ServerResponse authenticateUser(String user, String pwd) throws IOException {
        rlUsers.lock();
        try {
            if (userExists(user)) {
                if (USERS.get(user).equals(pwd)) {
                    return new ServerResponse(MessageCode.OK_USER);
                }
                return new ServerResponse(MessageCode.WRONG_PWD);
            }
        } finally {
            rlUsers.unlock();
        }

        wlUsers.lock();
        try {
            registerUser(user, pwd);
            return new ServerResponse(MessageCode.OK_NEW_USER);
        } finally {
            wlUsers.unlock();
        }
    }

    private boolean userExists(String userID) {
        return USERS.containsKey(userID);
    }

    public void disconnectDevice(String userID, String devID){
        wlDomains.lock();
        try {
            String fullID = fullID(userID, devID);
            DEVICES.get(fullID).goOffline();
        } finally {
            wlDomains.unlock();
        }
    }

    //assumes userId exists
    public ServerResponse authenticateDevice(String userId, String devId)throws IOException{
        wlDomains.lock();
        wlUsers.lock();
        try {
            String fullDevId = fullID(userId, devId);
            if (DEVICES.containsKey(fullDevId)) {
                Device dev = DEVICES.get(fullDevId);
                System.out.println("devid:" + fullDevId);
                if (dev.isOnline()) {
                    System.out.println("dev is online");
                    return new ServerResponse(MessageCode.NOK_DEVID);
                } else {
                    registerDevice(fullDevId, dev);
                    return new ServerResponse(MessageCode.OK_DEVID);
                }
            }
            Device dev = new Device(fullDevId);
            registerDevice(fullDevId, dev);
            return new ServerResponse(MessageCode.OK_DEVID);
        } finally {
            wlDomains.unlock();
            wlUsers.unlock();
        }
    }

    public ServerResponse testDevice(String devFileName, long devFileSize)throws IOException{
        if (devFileName.equals(clientFileName) && devFileSize==clientFileSize){
            return new ServerResponse(MessageCode.OK_TESTED);
        }
        return new ServerResponse(MessageCode.NOK_TESTED);
    }
}
