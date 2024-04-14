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
        devicesFile = new File(deviceFilePath);
        devices = new HashMap<>();
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
        wLock = rwLock.writeLock();
        rLock = rwLock.readLock();
    }

    public void addDevice(String userID, String devID) {
        Device device = new Device(userID, devID);
        device.goOnline();
        devices.put(Utils.fullID(userID, devID), device);
    }

    public void addDomainToDevice(String userID, String devID,
            String domainName) {
        devices.get(Utils.fullID(userID, devID)).registerInDomain(domainName);
    }

    public void saveDeviceImage() {
        throw new UnsupportedOperationException();
    }

    public void getDeviceImage() {
        throw new UnsupportedOperationException();
    }

    public void saveDeviceTemperature(String userID, String devID, float temp) {
        devices.get(Utils.fullID(userID, devID)).registerTemperature(temp);
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
