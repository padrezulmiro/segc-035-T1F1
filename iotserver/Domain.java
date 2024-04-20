package iotserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Domain {
    private String name;
    private String ownerId;
    private Set<String> registeredUsers;
    private Set<String> devices;
    private HashMap<String, String> enDomkeyMap;

    public Domain(String name, String ownerId) {
        this.name = name;
        this.ownerId = ownerId;
        this.registeredUsers = new HashSet<>();
        this.devices = new HashSet<>();
        this.enDomkeyMap = new HashMap<>();
    }

    public boolean registerUser(String userId, String enDomkey) {
        if (this.isOwner(userId)){
            return false;
        }
        if(registeredUsers.add(userId)){
            enDomkeyMap.put(userId, enDomkey);
            return true;
        }
        return false;
    }

    public boolean isOwner(String userId) {
        return userId.equals(ownerId);
    }

    public boolean isRegistered(String userId) {
        return registeredUsers.contains(userId) || isOwner(userId);
    }

    public boolean registerDevice(String deviceFullID) {
        return devices.add(deviceFullID);
    }

    public boolean isDeviceRegistered(String device) {
        return devices.contains(device);
    }
    
    public String getName(){
        return this.name;
    }

    public Set<String> getDevices(){
        return this.devices;
    }

    public Set<String> getUsers(){
        return this.registeredUsers;
    }

    @Override
    public String toString() {
        final char NL = '\n';
        final char TAB = '\t';
        final char SP = ':';

        StringBuilder sb = new StringBuilder();
        sb.append(getName() + SP + ownerId);

        for (String registeredUser : registeredUsers) {
            sb.append(SP + registeredUser);
            sb.append(SP + enDomkeyMap.get(registeredUser));
            // append user's associated endomkey as well
        }

        sb.append(NL);

        for (String devFullId : devices) {
            sb.append(TAB + devFullId + NL);
        }

        return sb.toString();
    }
}
