package iotserver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Device {
    private String userId;
    private String devId;
    private String fullId;

    private boolean online;
    private String imgPath;
    private List<Float> temperatures;
    private Set<String> registeredDomains;

    public Device(String fullId) {
        this(fullId.split(":")[0], fullId.split(":")[1]);
    }

    public Device(String userId, String devId) {
        this.userId = userId;
        this.devId = devId;
        this.fullId = userId + ":" + devId;
        this.online = false;
        this.imgPath = null;
        this.temperatures = new ArrayList<>();
        this.registeredDomains = new HashSet<>();
    }

    public boolean isOnline() {
        return online;
    }

    public void goOnline() {
        online = true;
    }

    public void goOffline() {
        online = false;
    }

    public String fullId() {
        return userId + ":" + devId;
    }

    public void registerImage(String imgPath) {
        throw new UnsupportedOperationException();
    }

    public void registerInDomain(String domainName) {
        registeredDomains.add(domainName);
        IoTServer.DOMAINS.get(domainName).registerDevice(fullId);
    }

    public void registerTemperature(float temperature) {
        temperatures.add(temperature);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Device)) return false;

        Device other = (Device) obj;
        return this.userId.equals(other.userId) && 
                this.devId.equals(other.devId);
    }

    @Override
    public int hashCode() {
        int code = this.userId.hashCode();
        code = 31 * code + this.devId.hashCode();
        return code;
    }
}
