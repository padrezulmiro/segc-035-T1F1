package iotserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import iotclient.MessageCode;

public class ServerResponse  {
    private MessageCode code;
    private long fileSize;
    private InputStream fileStream;
    private String filePath;
    private Map<String,Float> temperatures;
    public ServerResponse(MessageCode code) {
        this.code = code;
        this.fileSize = -1;
        this.fileStream = null;
        this.temperatures = null;
    }

    public ServerResponse(MessageCode code, String filePath) {
        this.code = code;
        File file = new File(filePath);
        this.fileSize = file.length();
        try {
            this.fileStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public ServerResponse(MessageCode code, Map<String,Float> temps){
        this.code = code;
        this.temperatures = temps;
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

    public Map<String,Float> temperatures(){
        return temperatures;
    }

}
