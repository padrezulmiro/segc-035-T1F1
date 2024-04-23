package iotserver;

import java.util.HashMap;
import java.util.Set;


public class Device {
    private String userId;
    private String devId;

    private boolean online;
    // private Set<String> registeredDomains;
    // domain,   possible img, possible temp
    private HashMap<String, StoredData> domainDataMap;

    public class StoredData {
        public StoredData(String img, String tempStr) {
           this.image = img;
           this.tempStr = tempStr;
        }
    
        public void setImagePath(String image) {this.image = image;}
        public void setTempStr(String temp) {this.tempStr = temp;}

        public String getImagePath() { return this.image; }
        public String getTempStr() { return this.tempStr; }
    
        private String image;
        private String tempStr;
    }

    public Device(String fullId) {
        this(fullId.split(":")[0], fullId.split(":")[1]);
    }

    public Device(String userId, String devId) {
        this.userId = userId;
        this.devId = devId;
        this.online = false;
        // this.registeredDomains = new HashSet<>();
        this.domainDataMap = new HashMap<>();
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

    public void registerImage(String imgPath, String domainName) {
        // this.imgPath=imgPath;
        StoredData sd = domainDataMap.get(domainName);
        sd.setImagePath(imgPath);
    }

    public void registerInDomain(String domainName) {
        // registeredDomains.add(domainName);
        domainDataMap.put(domainName, new StoredData(null,null));
    }

    public void registerTemperature(String temperature, String domainName) {
        StoredData sd = domainDataMap.get(domainName);
        sd.setTempStr(temperature);
    }

    public String getTemperature(String domainName){
        return domainDataMap.get(domainName).getTempStr();
    }

    public String getImagePath(String domainName) {
        return domainDataMap.get(domainName).getImagePath();
    }

    public Set<String> getDomains(){
        // return this.registeredDomains;
        return domainDataMap.keySet();
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


    @Override
    public String toString() {
        return fullId();
    }
    
    // @Override
    public String toString(String domain) {
        final char NL = '\n';
        final char SP = ':';

        String temperature = getTemperature(domain) != null ?
            getTemperature(domain) : "";
        String imagePath = getImagePath(domain) != null ?
            getImagePath(domain) : "";

        StringBuilder sb = new StringBuilder();
        sb.append(fullId() + SP + temperature + SP + imagePath + NL);
        return sb.toString();
    }
}
