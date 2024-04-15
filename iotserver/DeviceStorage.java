package iotserver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DeviceStorage {
    private Map<String, Device> devices;
    private File devicesFile;
    private Lock wLock;
    private Lock rLock;

    public DeviceStorage(String deviceFilePath) {
        devices = new HashMap<>();
        devicesFile = new File(deviceFilePath);
        populateDevicesFromFile();

        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
        wLock = rwLock.writeLock();
        rLock = rwLock.readLock();
    }

    public void addDevice(String userID, String devID) {
        Device device = new Device(userID, devID);
        device.goOnline();
        devices.put(Utils.fullID(userID, devID), device);
        updateDevicesFile();
    }

    public void addDomainToDevice(String userID, String devID,
            String domainName) {
        devices.get(Utils.fullID(userID, devID)).registerInDomain(domainName);
        updateDevicesFile();
    }

    public void saveDeviceImage(String userID, String devID, String imgPath) {
        devices.get(Utils.fullID(userID, devID)).registerImage(imgPath);
        updateDevicesFile();
    }

    public String getDeviceImage(String userID, String devID) {
        return devices.get(Utils.fullID(userID, devID)).getImagePath();
    }

    public void saveDeviceTemperature(String userID, String devID, float temp) {
        devices.get(Utils.fullID(userID, devID)).registerTemperature(temp);
        updateDevicesFile();
    }

    public float getDeviceTemperature(String userID, String devID) {
        return devices.get(Utils.fullID(userID, devID)).getTemperature();
    }

    public boolean deviceExists(String userID, String devID) {
        return devices.containsKey(Utils.fullID(userID, devID));
    }

    public void activateDevice(String userID, String devID) {
        devices.get(Utils.fullID(userID, devID)).goOnline();
    }

    public void deactivateDevice(String userID, String devID) {
        devices.get(Utils.fullID(userID, devID)).goOffline();
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

    private void updateDevicesFile() {
        throw new UnsupportedOperationException();
    }

    private void populateDevicesFromFile() {
        throw new UnsupportedOperationException();
    }
}
