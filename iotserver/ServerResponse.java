package iotserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import iotclient.MessageCode;

public class ServerResponse  {
    private MessageCode code;
    private long fileSize;
    private InputStream fileStream;
    private String filePath;
    public ServerResponse(MessageCode code) {
        this.code = code;
        this.fileSize = -1;
        this.fileStream = null;
        // this.temperatures = new ArrayList<>();
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

}
