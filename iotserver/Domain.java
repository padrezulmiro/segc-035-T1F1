package iotserver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Domain {
    private String name;
    private String ownerId;
    private Set<String> registeredUsers;
    private Set<String> devices;
    private List<Float> temperatures;

    public Domain(String name, String ownerId) {
        this.name = name;
        this.ownerId = ownerId;
        this.registeredUsers = new HashSet<>();
        this.devices = new HashSet<>();
        this.temperatures = new ArrayList<>();
    }

    public boolean registerUser(String userId) {
        return registeredUsers.add(userId);
    }

    public boolean isOwner(String userId) {
        return userId.equals(ownerId);
    }

    public boolean isRegistered(String userId) {
        return registeredUsers.contains(userId);
    }

    public boolean registerDevice(String deviceFullID) {
        return devices.add(deviceFullID);
    }

    public void registerTemperature(float temperature) {
        temperatures.add(temperature);
    }

    public boolean isDeviceRegistered(String device) {
        return devices.contains(device);
    }
    
    public String getName(){
        return name;
    }

    public List<Float> getTempList(){
        return this.temperatures;
    }

}
