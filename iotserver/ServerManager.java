package iotserver;
import iohelper.FileHelper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import iotclient.MessageCode;

public class ServerManager {
    private static volatile ServerManager instance;

    private static Map<String, String> USERS;
    private static Map<String, Domain> DOMAINS; 
    private static Map<String, Device> DEVICES;

    private static final String domainFilePath = "domain.txt";
    private static final String userFilePath = "user.txt";
    private static final String imageDirectoryPath = "./img/";
    private static final String temperatureDirectoryPath = "./temp/";

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
    private static BufferedReader domainReader;
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

                    new File(imageDirectoryPath).mkdirs();
                    new File(temperatureDirectoryPath).mkdirs();
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
                    // userWriter = new BufferedWriter(new FileWriter(userRecord,true));
                    // why would this empty it all.
                    // ref: https://stackoverflow.com/questions/17244713/using-filewriter-and-bufferedwriter-clearing-file-for-some-reason
                    readUsersFile();
                    userReader.close();
                    
                    domainRecord = initializeFile(domainFilePath);
                    domainReader = new BufferedReader(new FileReader(domainRecord));
                    domainWriter = new BufferedWriter(new FileWriter(domainRecord,true));
                    readDomainsFile();
                    domainReader.close();
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
        System.out.println(Arrays.asList(DOMAINS)); //print debug domain set, feel free to delete
        updateDomainsFile();

        return new ServerResponse(MessageCode.OK);

    }

    synchronized public ServerResponse addUserToDomain(String ownderUID, String newUserID, String domainName) {
        if (!domainExists(domainName)) {
            return new ServerResponse(MessageCode.NODM);
        }

        Domain domain = DOMAINS.get(domainName);

        if (!domain.isOwner(ownderUID)) {
            return new ServerResponse(MessageCode.NOPERM);
        }

        if (domain.registerUser(newUserID)) {
            // update DOMAINS
            ServerManager.DOMAINS.replace(domain.getName(),domain);
            updateDomainsFile();
            return new ServerResponse(MessageCode.OK);
        } else {
            return new ServerResponse(MessageCode.USEREXISTS);
        }
    }

    // devID being ID
    synchronized public ServerResponse registerDeviceInDomain(String domainName, String userId, String devId) {
        if (!domainExists(domainName)) {
            return new ServerResponse(MessageCode.NODM);
        }

        Domain domain = DOMAINS.get(domainName);
        if (!domain.isRegistered(userId)) {
            return new ServerResponse(MessageCode.NOPERM);
        }

        String fullDevId = userId + ":" + devId;
        ServerManager.DEVICES.get(fullDevId).registerInDomain(domainName);
        domain.registerDevice(fullDevId);
        ServerManager.DOMAINS.replace(domainName,domain);
        updateDomainsFile();
        return new ServerResponse(MessageCode.OK);
    }


    synchronized public ServerResponse registerTemperature(String temperatureString, String userId, String devId) {
        float temperature;
        String fullDevId = userId + ":" + devId;
        try {
            temperature = Float.parseFloat(temperatureString);
        } catch (NumberFormatException e) {
            return new ServerResponse(MessageCode.NOK);
        }
        ServerManager.DEVICES.get(fullDevId).registerTemperature(temperature);
        return new ServerResponse(MessageCode.OK);
    }

    synchronized public ServerResponse registerImage(String filename, long filesize, ObjectInputStream in, String userId, String devId){
        String fullDevId = userId + ":" + devId;
        String fullImgPath = imageDirectoryPath+filename;

        // try {
        //     // set up file generation
        //     File f = new File(fullImgPath);
        //     FileOutputStream fout = new FileOutputStream(f);
        //     OutputStream fileOutput = new BufferedOutputStream(fout);
            
        //     // set up buffered data reading
        //     InputStream input = new BufferedInputStream(imageStream);
        //     int totalBytesRead = 0;
        //     byte[] buffer = new byte[1024];
        //     // writing to drive
        //     while (filesize > totalBytesRead){
        //         int bytesRead = input.read(buffer,0,1024);
        //         totalBytesRead += bytesRead;
        //         fileOutput.write(buffer,0,bytesRead);
        //         fileOutput.flush();
        //     }
        //     fileOutput.close();
        //     fout.close();            
        // } catch (Exception e) {
        //     return new ServerResponse(MessageCode.NOK);
        // }

        FileHelper.receiveFile(filesize, fullImgPath, in);

        ServerManager.DEVICES.get(fullDevId).registerImage(fullImgPath);
        return new ServerResponse(MessageCode.OK);
    }

    synchronized public ServerResponse getTemperatures(String user, String domainName) throws IOException {
        Domain dom = ServerManager.DOMAINS.get(domainName);
        if (dom == null){
            return new ServerResponse(MessageCode.NODM); 
        }
        if (dom.isRegistered(user)){
            Map<String,Float> temps = getTempList(dom);
            // // write into File
            // String tempFilePath = temperatureDirectoryPath+"temps_" + domainName + ".txt";
            // File tempFile = new File(tempFilePath);
            // BufferedWriter tempWriter = new BufferedWriter(new FileWriter(tempFile));

            // for(Entry<String, Float> entry : temps.entrySet()){
            //     tempWriter.write(entry.getKey() + ":" + entry.getValue() +System.getProperty ("line.separator"));
            //     tempWriter.flush();

            // tempWriter.close();

            // // FileHelper.sendFile(tempFilePath, out);
            // return new ServerResponse(MessageCode.OK,tempFilePath);
            return new ServerResponse(MessageCode.OK,temps);
        }
        return new ServerResponse(MessageCode.NOPERM);
    }

    synchronized public ServerResponse getImage(String user,String targetUserId, String targetDevId) {
        String targetDevFullId = targetUserId + ":" + targetDevId;
        Device dev = DEVICES.get(targetDevFullId);
        if (dev == null){
            return new ServerResponse(MessageCode.NOID);
        }

        String filepath = dev.getFilepath();
        if (filepath == null){
            return new ServerResponse(MessageCode.NODATA);
        }

        // if it's the device's own image, return file
        if (user.equals(targetUserId)){
            return new ServerResponse(MessageCode.OK,filepath);
        }

        //if user isnt target device + does not exist in any of the target domain, return NOPERM
        for (String dom : dev.getDomains()){
            Domain domain = DOMAINS.get(dom);
            if (domain.isRegistered(user)){
                return new ServerResponse(MessageCode.OK,filepath);
            }
        }
        return new ServerResponse(MessageCode.NOPERM);

    }

    synchronized private boolean domainExists(String domainName) {
        return DOMAINS.containsKey(domainName);
    }

    synchronized private Map<String,Float> getTempList(Domain domain){
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
        userWriter = new BufferedWriter(new FileWriter(userRecord));
        for(Map.Entry<String,String> entry : USERS.entrySet()){
           userWriter.write(entry.getKey()+":"+entry.getValue()+System.getProperty ("line.separator"));
           userWriter.flush();
        }
        return true; 
    }

    //beware that domaindata HAS to only have three items each line
    //domain_name:user,user,user:dev,dev,dev 
    //where the first user is the owner of the domain
    synchronized public static void readDomainsFile() throws IOException{
        String line;
        while (( line = domainReader.readLine()) != null) {
            String[]dominfo = line.split(":");
            String[]users = dominfo[1].split(",");
            String[]devices = dominfo[2].split(",");
            String domainName = dominfo[0];
            Domain domain = new Domain(domainName,users[0]);
            ServerManager.DOMAINS.put(domainName, domain);

            for(String devName : devices){
                domain.registerDevice(devName);
                Device device;
                if(!DEVICES.containsKey(devName)){
                    device = new Device(devName);
                    DEVICES.put(devName,device);
                }
                device = DEVICES.get(devName);
                device.registerInDomain(domainName);
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

    private static String fullID(String userId, String devId){
        return null;
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