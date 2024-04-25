package iotserver;

public class ServerConfig {
    private static volatile ServerConfig instance;

    private String keyStorePath;
    private String keyStorePwd;

    public static ServerConfig getInstance() {
        ServerConfig singleton = instance;
        if (singleton != null) return singleton;

        synchronized(ServerConfig.class) {
            if (instance == null) return new ServerConfig();
            return instance;
        }
    }

    private ServerConfig() {}

    public void setKeyStorePath(String path) {
        keyStorePath = path;
    }

    public void setKeyStorePwd(String pwd) {
        keyStorePwd = pwd;
    }

    public String keyStorePath() {
        return keyStorePath;
    }

    public String keyStorePwd() {
        return keyStorePwd;
    }
}
