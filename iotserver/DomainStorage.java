package iotserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.Mac;

import iohelper.CipherHelper;
import iohelper.Utils;

import iohelper.Utils;

public class DomainStorage {
    private final String HASH_KEY_ALIAS = "files-hash-key";
    private final String MAC_ALGORITHM = "HmacSHA256";
    private final String HMAC_FILE_PATH = "./output/server/domain-sha";

    private Map<String, Domain> domains;
    private File domainsFile;
    private Lock wLock;
    private Lock rLock;

    public DomainStorage(String domainFilePath, DeviceStorage devStorage) throws GeneralSecurityException {
        domainsFile = new File(domainFilePath);
        domains = new HashMap<>();

        try {
            domainsFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            populateDomainsFromFile(devStorage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
        wLock = rwLock.writeLock();
        rLock = rwLock.readLock();
    }

    public void addDomain(String domainName, String ownerUID, DeviceStorage deviceStorage) {
        Domain domain = new Domain(domainName, ownerUID);
        domains.put(domainName, domain);
        updateDomainsFile(deviceStorage);
    }

    public boolean addUserToDomain(String newUserID,
            String domainName, String enDomkey, DeviceStorage deviceStorage) {
        Domain domain = domains.get(domainName);
        boolean ret = domain.registerUser(newUserID, enDomkey);
        if (ret) updateDomainsFile(deviceStorage);
        return ret;
    }

    public boolean addDeviceToDomain(String userID, String devID,
            String domainName, DeviceStorage deviceStorage) {
        Domain domain = domains.get(domainName);
        boolean ret = domain.registerDevice(Utils.fullID(userID, devID));
        if (ret) updateDomainsFile(deviceStorage);
        return ret;
    }

    public String getDeviceEncryptedDomainKey(String domainName, String userID){
        Domain domain = domains.get(domainName);
        return domain.getDeviceEncryptedDomainKey(userID);
    }

    /**
     * Retrieving all domain's temperature
     * @param domainName the domain to check
     * @param devStorage
     * @return Map<devFullID, encryptedTempStr> where temp is encrypted by
     *         the domain's secret key
     */
    public Map<String, String> temperatures(String domainName,
            DeviceStorage devStorage) {
        //FIXME A better implementation doesn't need access to devStorage
        // This can be achieved by refactoring the domain's registered devices
        // as a Set<Device> instead of Set<String>

        Domain domain = domains.get(domainName);
        Map<String, String> temperatures = new HashMap<>();

        for (String fullDevID : domain.getDevices()) {
            String userID = Utils.userIDFromFullID(fullDevID);
            String devID = Utils.devIDFromFullID(fullDevID);
            String devTemperature =
                devStorage.getDeviceTemperature(userID, devID, domainName);
                temperatures.put(fullDevID, devTemperature);
        }
        return temperatures;
    }

    public boolean domainExists(String domainName) {
        return domains.containsKey(domainName);
    }

    public boolean isOwnerOfDomain(String userID, String domainName) {
        Domain domain = domains.get(domainName);
        return domain.isOwner(userID);
    }

    public boolean isUserRegisteredInDomain(String userID, String domainName) {
        Domain domain = domains.get(domainName);
        return domain.isRegistered(userID);
    }

    public boolean isDeviceRegisteredInDomain(String userID, String devID,
            String domainName) {
        Domain domain = domains.get(domainName);
        return domain.isDeviceRegistered(Utils.fullID(userID, devID));
    }

    public Set<String> getUserDomains(String userID){
        Set<String> userDomains = new HashSet<String>();
        for (String dom : domains.keySet()){
            if (isUserRegisteredInDomain(userID, dom)){
                userDomains.add(dom);
            }
        }
        return userDomains;
    }

    public Set<String> getDeviceDomains(String userID, String devID){
        Set<String> devDomains = new HashSet<String>();
        for (String dom : domains.keySet()){
            if (isDeviceRegisteredInDomain(userID, devID, dom)){
                devDomains.add(dom);
            }
        }
        return devDomains;
    }

    public void readLock() {
        rLock.lock();
    }

    public void readUnlock() {
        rLock.unlock();
    }

    public void writeLock() {
        wLock.lock();
    }

    public void writeUnlock() {
        wLock.unlock();
    }

    public String hasAccessToDeviceIn(String user, String devUID, String devDID) {
        String fullID = Utils.fullID(devUID, devDID);
        for (Domain domain : domains.values()) {
            if (!domain.isDeviceRegistered(fullID)) continue;
            if (domain.isRegistered(user)) {
                return domain.getName();
            }
        }
        return null;
    }

    // forgive me god of good OOP  practice for i have sinned
    public void updateDomainsFile(DeviceStorage devStorage){
        StringBuilder sb = new StringBuilder();
        final char TAB = '\t';

        for (Domain domain : domains.values()) {
            sb.append(domain.toString());

            Set<String> devices = domain.getDevices();
            for (String dev : devices){
                sb.append(TAB + "" + TAB);
                sb.append(devStorage.getDeviceString(dev,domain.getName()));
            }
        }

        try (PrintWriter pw = new PrintWriter(domainsFile)) {
            pw.write(sb.toString());
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            CipherHelper.writeHmacToFile(
                CipherHelper.computeFileHash(sb.toString(),
                            HASH_KEY_ALIAS,MAC_ALGORITHM),
                HMAC_FILE_PATH
                );
        } catch (UnrecoverableKeyException |
                 InvalidKeyException |
                 KeyStoreException |
                 NoSuchAlgorithmException |
                 CertificateException |
                 IOException e) {
            e.printStackTrace();
        }
    }

    private void populateDomainsFromFile(DeviceStorage devStorage)
            throws IOException, GeneralSecurityException {
        final char SP = ':';
        final char TAB = '\t';
        final char NL = '\n';


        BufferedReader reader = new BufferedReader(new FileReader(domainsFile));
        String[] lines = (String[]) reader.lines().toArray(String[]::new);
        reader.close();

        StringBuilder sb = new StringBuilder();
        for (String s : lines) {
            sb.append(s+NL);
        }

        boolean validHmac = false;
        try {
            if (domainsFile.exists() && domainsFile.length()!=0){
                validHmac = CipherHelper.verifyHmac(sb.toString(),
                            HASH_KEY_ALIAS, MAC_ALGORITHM, HMAC_FILE_PATH);
            }else{
                validHmac = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!validHmac) {
            throw new GeneralSecurityException("Computed SHA-Hmac" +
                    "differs from expected! Corrupted domains' file.");
        }
        
        String currentDomainName = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean isDomainLine = line.charAt(0) != TAB;
            boolean isEnDomKeyLine = line.charAt(1) != TAB;
            String[] tokens = Utils.split(line, SP);

            if (isDomainLine) {
                currentDomainName = tokens[0];
                String owner = tokens[1];
                Domain domain = new Domain(currentDomainName, owner);
                domains.put(currentDomainName, domain);
            } else if(isEnDomKeyLine){
                String devUID = tokens[0];
                String enDomkey = tokens[1];
                domains
                .get(currentDomainName)
                .registerUser(devUID,enDomkey);
            }else {
                String devUID = tokens[0];
                String devDID = tokens[1];
                String imgPath = tokens[2];
                String enTempStr = tokens[3];
                domains
                .get(currentDomainName)
                .registerDevice(Utils.fullID(devUID, devDID));
                devStorage.addDomainToDevice(devUID, devDID, currentDomainName);
                devStorage.saveDeviceImage(devUID, devDID, imgPath, currentDomainName);
                devStorage.saveDeviceTemperature(devUID, devDID, enTempStr, currentDomainName);
            }
        }
    }
}
