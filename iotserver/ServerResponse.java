package iotserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import iotclient.MessageCode;

public class ServerResponse implements Serializable{
    private MessageCode code;
    private long fileSize;
    private transient InputStream fileStream;
    private String filePath;
    private Map<String,String> temperatures; // devFullID, encryptedTempID
    private String encryptedDomainKey;
    private HashMap<String,String> allEncryptedDomainKeys;
    private Set<String> domains;

    public ServerResponse(MessageCode code) {
        this.code = code;
        this.fileSize = -1;
        this.fileStream = null;
        this.temperatures = null;
    }

    public ServerResponse(MessageCode code, HashMap<String,String> enDomkeys) {
        this.code = code;
        this.allEncryptedDomainKeys = enDomkeys;
    }

    public ServerResponse(MessageCode code, String filePath, String enDomkey) {
        this.code = code;
        this.filePath= filePath;
        this.encryptedDomainKey = enDomkey;
        File file = new File(filePath);
        this.fileSize = file.length();
        try {
            this.fileStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public ServerResponse(MessageCode code, Map<String,String> temps, String enDomkey){
        this.code = code;
        this.temperatures = temps;
        this.encryptedDomainKey = enDomkey;
    }
    public ServerResponse(MessageCode code, Set<String> domains) {
        this.code = code;
        this.domains = domains;
    }

    public MessageCode responseCode() {
        return code;
    }

    public long dataStreamLength() {
        return fileSize;
    }

    public InputStream dataStream() {
        return fileStream;
    }

    public String filePath(){
        return filePath;
    }

    public Map<String,String> temperatures(){
        return temperatures;
    }

    public String encryptedDomainKey(){
        return encryptedDomainKey;
    }

    public HashMap<String,String> allEncryptedDomainKeys() {
        return allEncryptedDomainKeys;
    }

    public Set<String> domains(){
        return domains;
    }
}
