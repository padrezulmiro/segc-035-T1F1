package iotserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class DomainStorage {
    private Map<String, Domain> domains;
    private File domainsFile;
    private Lock wLock;
    private Lock rLock;

    public DomainStorage() {
        throw new UnsupportedOperationException();
    }

    public void addDomain(String domainName, String ownerUID) {
        Domain domain = new Domain(domainName, ownerUID);
        domains.put(domainName, domain);
        updateDomainsFile();
    }

    public boolean addUserToDomain(String requesterUID, String newUserID,
            String domainName) {
        Domain domain = domains.get(domainName);
        boolean wasRegistered = domain.registerUser(newUserID);
        if (wasRegistered) updateDomainsFile();
        return wasRegistered;
    }

    public void addDeviceToDomain(String userID, String devID,
            String domainName) {
        Domain domain = domains.get(domainName);
        domain.registerDevice(Utils.fullID(userID, devID));
    }

    public Map<String, Float> temperatures(String domainName,
            DeviceStorage devStorage) {
        Domain domain = domains.get(domainName);
        Map<String, Float> temperatures = new HashMap<>();

        for (String devID : domain.getDevices()) {
            float devTemperature = devStorage.getDeviceTemperature(devID);
            temperatures.put(devID, devTemperature);
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

    public void readLock() {
        rLock.lock();
    }

    public void readUnlock() {
        rLock.unlock();
    }

    public void writerLock() {
        wLock.lock();
    }

    public void writerUnlock() {
        wLock.unlock();
    }

    private Domain getDomain() {
        throw new UnsupportedOperationException();
    }

    private void updateDomainsFile(){
        StringBuilder sb = new StringBuilder();
        for (Domain domain : domains.values()) {
            sb.append(domain.toString());
        }

        try (PrintWriter pw = new PrintWriter(domainsFile)) {
            pw.write(sb.toString());
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
