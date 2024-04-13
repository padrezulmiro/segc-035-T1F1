package iotserver;

import java.util.Map;
import java.util.concurrent.locks.Lock;

public class DeviceStorage {
    private Map<String, Device> devices;
    private Lock wLock;
    private Lock rLock;

    public DeviceStorage() {
        throw new UnsupportedOperationException();
    }

    public void addDevice() {
        throw new UnsupportedOperationException();
    }

    public void saveTemperature() {
        throw new UnsupportedOperationException();
    }

    public void saveDeviceImage() {
        throw new UnsupportedOperationException();
    }

    public void getDeviceImage() {
        throw new UnsupportedOperationException();
    }

    public float getDeviceTemperature() {
        throw new UnsupportedOperationException();
    }

    public boolean deviceExists() {
        throw new UnsupportedOperationException();
    }

    public void activateDevice() {
        throw new UnsupportedOperationException();
    }

    public void deactivateDevice() {
        throw new UnsupportedOperationException();
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
}
