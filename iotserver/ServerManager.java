package iotserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import iohelper.Utils;
import iotclient.MessageCode;

public class ServerManager {
    private static volatile ServerManager instance;

    private DomainStorage domStorage;
    private DeviceStorage devStorage;
    private UserStorage userStorage;
    private Long clientFileSize;

    private static final String attestationFilePath = "attestation.txt";
    private static final String domainFilePath = "domain.txt";
    private static final String deviceFilePath = "device.txt";
    private static final String userFilePath = "user.txt";
    private static final String imageDirectoryPath = "./img/";
    private static final String temperatureDirectoryPath = "./temp/";
    private static final String clientFileName = "IoTDevice.jar";

    private ServerManager(){
        domStorage = new DomainStorage(domainFilePath);
        devStorage = new DeviceStorage(deviceFilePath);
        userStorage = new UserStorage(userFilePath);

        new File(imageDirectoryPath).mkdirs();
        new File(temperatureDirectoryPath).mkdirs();

        // register attestation value
        try {
            File attestationFile = new File(attestationFilePath);
            BufferedReader attestationReader =
                new BufferedReader(new FileReader(attestationFile));
            clientFileSize = Long.parseLong(attestationReader.readLine());
            attestationReader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static ServerManager getInstance(){
        // thread calls this to get the db
        ServerManager res = instance;
        if(res != null){
            return res;
        }

        synchronized(ServerManager.class) {
            if (instance == null) {
                instance = new ServerManager();
            }
            return instance;
        }
    }

    /*
     * CLIENT COMMANDS====================================================================================================================
     */
    public ServerResponse createDomain(String ownerUID, String domainName){
        domStorage.writeLock();
        try {
            if (domStorage.domainExists(domainName)) {
                return new ServerResponse(MessageCode.NOK);
            }

            domStorage.addDomain(domainName, ownerUID);
            return new ServerResponse(MessageCode.OK);
        } finally {
            domStorage.writeUnlock();
        }
    }

    public ServerResponse addUserToDomain(String requesterUID, String newUserID,
            String domainName, String enDomkey) {
        domStorage.writeLock();
        userStorage.readLock();
        try {
            if (!domStorage.domainExists(domainName)) {
                return new ServerResponse(MessageCode.NODM);
            }

            if (!userStorage.isUserRegistered(newUserID)) {
                return new ServerResponse(MessageCode.NOUSER);
            }

            if (!domStorage.isOwnerOfDomain(requesterUID, domainName)) {
                return new ServerResponse(MessageCode.NOPERM);
            }

            boolean ret = domStorage
                .addUserToDomain(requesterUID, newUserID, domainName, enDomkey);
            if (ret) {
                return new ServerResponse(MessageCode.OK);
            } else {
                return new ServerResponse(MessageCode.USEREXISTS);
            }
        } finally {
            userStorage.readUnlock();
            domStorage.writeUnlock();
        }
    }

    // devID being ID
    public ServerResponse registerDeviceInDomain(String domainName,
            String userId, String devId) {
        domStorage.writeLock();
        devStorage.writeLock();
        try {
            if (!domStorage.domainExists(domainName)) {
                return new ServerResponse(MessageCode.NODM);
            }

            if (!domStorage.isUserRegisteredInDomain(userId, domainName)) {
                return new ServerResponse(MessageCode.NOPERM);
            }

            if (domStorage.isDeviceRegisteredInDomain(userId, devId,
                    domainName)) {
                return new ServerResponse(MessageCode.DEVICEEXISTS);
            }

            domStorage.addDeviceToDomain(userId, devId, domainName);
            devStorage.addDomainToDevice(userId, devId, domainName);
            return new ServerResponse(MessageCode.OK);
        } finally {
            devStorage.writeUnlock();
            domStorage.writeUnlock();
        }
    }


    public ServerResponse registerTemperature(float temperature, String userId,
            String devId) {
        devStorage.writeLock();
        try {
            devStorage.saveDeviceTemperature(userId, devId, temperature);
            return new ServerResponse(MessageCode.OK);
        } finally {
            devStorage.writeUnlock();
        }
    }

    public ServerResponse registerImage(String filename, String userId,
            String devId) {
        devStorage.writeLock();
        try {
            devStorage.saveDeviceImage(userId, devId, filename);
            return new ServerResponse(MessageCode.OK);
        } finally {
            devStorage.writeUnlock();
        }
    }

    public ServerResponse getTemperatures(String user, String domainName)
            throws IOException {
        domStorage.readLock();
        devStorage.readLock();
        try {
            if (!domStorage.domainExists(domainName)) {
                return new ServerResponse(MessageCode.NODM);
            }

            if (!domStorage.isUserRegisteredInDomain(user, domainName)) {
                return new ServerResponse(MessageCode.NOPERM);
            }

            Map<String, Float> temps = domStorage.temperatures(domainName,
                    devStorage);

            //XXX ServerResponse is being init with a Map?
            return new ServerResponse(MessageCode.OK, temps);
        } finally {
            devStorage.readUnlock();
            domStorage.readUnlock();
        }
    }

    public ServerResponse getImage(String requesterUID, String targetUID,
            String targetDID) {
        domStorage.readLock();
        devStorage.readLock();
        try {
            if (!devStorage.deviceExists(targetUID, targetDID)) {
                return new ServerResponse(MessageCode.NOID);
            }

            String filepath = devStorage.getDeviceImage(targetUID, targetDID);
            if (filepath == null) {
                return new ServerResponse(MessageCode.NODATA);
            }

            if (domStorage.hasAccessToDevice(requesterUID, targetUID,
                    targetDID)) {
                return new ServerResponse(MessageCode.OK, filepath);
            }

            return new ServerResponse(MessageCode.NOPERM);
        } finally {
            devStorage.readUnlock();
            domStorage.readUnlock();
        }
    }

    /*
     *AUTHENTICATION====================================================================================================================
     */

    public ServerResponse authenticateUser(String user)
            throws IOException {
        userStorage.readLock();
        try {
            if (userStorage.isUserRegistered(user)) {
                return new ServerResponse(MessageCode.OK_USER);
            }
        } finally {
            userStorage.readUnlock();
        }

        userStorage.writeLock();
        try {
            userStorage.registerUser(user);
            return new ServerResponse(MessageCode.OK_NEW_USER);
        } finally {
            userStorage.writeUnlock();
        }
    }

    public void disconnectDevice(String userID, String devID){
        devStorage.writeLock();
        try {
            devStorage.deactivateDevice(userID, devID);
        } finally {
            devStorage.writeUnlock();
        }
    }

    //assumes userId exists
    public ServerResponse authenticateDevice(String userId, String devId)
            throws IOException {
        devStorage.writeLock();
        try {
            if (devStorage.deviceExists(userId, devId)) {
                System.out.println("devid:" + Utils.fullID(userId, devId));

                if (devStorage.isDeviceOnline(userId, devId)) {
                    System.out.println("dev is online");
                    return new ServerResponse(MessageCode.NOK_DEVID);
                } else {
                    devStorage.activateDevice(userId, devId);
                    return new ServerResponse(MessageCode.OK_DEVID);
                }
            }

            devStorage.addDevice(userId, devId);
            return new ServerResponse(MessageCode.OK_DEVID);
        } finally {
            devStorage.writeUnlock();
        }
    }

    public ServerResponse attestClient(String devFileName, long devFileSize)
            throws IOException {
        if (devFileName.equals(clientFileName) && devFileSize==clientFileSize) {
            return new ServerResponse(MessageCode.OK_TESTED);
        }

        return new ServerResponse(MessageCode.NOK_TESTED);
    }
}
