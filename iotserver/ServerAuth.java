package iotserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.concurrent.ThreadLocalRandom;

public class ServerAuth {
    private static volatile ServerAuth INSTANCE;

    private final String USER_FILEPATH = "user.txt";

    private UserStorage userStorage;
    private String apiKey;

    public static ServerAuth getInstance() {
        ServerAuth instance = INSTANCE;
        if (instance != null) return instance;

        synchronized(ServerAuth.class) {
            if (instance == null) instance = new ServerAuth();
            return instance;
        }
    }

    private ServerAuth() {
        userStorage = new UserStorage(USER_FILEPATH);
    }

    public void setApiKey(String key) {
        apiKey = key;
    }

    public long generateNonce() {
        return ThreadLocalRandom.current().nextLong();
    }

    public boolean isUserRegistered(String user) {
        return userStorage.isUserRegistered(user);
    }

    public boolean registerUser(String user, String certPath) {
        return userStorage.registerUser(user, certPath);
    }

    public String userCertPath(String user) {
        return userStorage.userCertPath(user);
    }

    public int generate2FACode() {
        return ThreadLocalRandom.current().nextInt(0, 100000);
    }

    public int send2FAEmail(String emailAddress, int code) {
        String codeStr = String.valueOf(code);
        String urlStr = String.format("https://lmpinto.eu.pythonanywhere.com" +
                "/2FA?e=%s&c=%s&a=%s", emailAddress, codeStr, apiKey);

        int responseCode = 500;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            responseCode = conn.getResponseCode();
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseCode;
    }

    public boolean verifySignedNonce(byte[] signedNonce, String user, long nonce)
            throws FileNotFoundException, IOException, CertificateException,
            NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance("MD5withRSA");
        Certificate cert = null;
        try (InputStream in = new FileInputStream(Utils.certPathFromUser(user))) {
            cert = CertificateFactory.getInstance("X509")
                    .generateCertificate(in);
        }

        signature.initVerify(cert);
        signature.update(ByteBuffer.allocate(Long.BYTES).putLong(nonce).array());
        return signature.verify(signedNonce);
    }

    public void saveCertificateInFile() {}
}
