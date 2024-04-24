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
        if(registeredUsers.add(userId)){
            registerEncryptedDomainKey(userId, enDomkey);
            return true;
        }
        return false;
    }

    public boolean isOwner(String userId) {
        return userId.equals(ownerId);
    }

    public boolean isRegistered(String userId) {
        return registeredUsers.contains(userId); // || isOwner(userId);
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

    public void registerEncryptedDomainKey(String user, String enDomkey){
        enDomkeyMap.put(user, enDomkey);
    }

    public String getDeviceEncryptedDomainKey(String user){
        return enDomkeyMap.get(user);
    }

    @Override
    public String toString() {
        final char NL = '\n';
        final char TAB = '\t';
        final char SP = ':';

        StringBuilder sb = new StringBuilder();
        sb.append(getName() + SP + ownerId + NL);

        for (String registeredUser : registeredUsers) {
            sb.append("" + TAB + registeredUser);
            sb.append(SP + enDomkeyMap.get(registeredUser) + NL);
        }
        
        for (String devFullId : devices) {
            sb.append(TAB + "" + TAB + devFullId + NL);
        }

        return sb.toString();
    }
}
