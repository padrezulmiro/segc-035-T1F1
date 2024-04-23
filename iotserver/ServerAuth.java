package iotserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;
import iohelper.Utils;

public class ServerAuth {
    private static volatile ServerAuth INSTANCE;

    private static final String USER_FILEPATH = "user.txt";
    private static final String CLIENT_EXEC_PATH = "IoTDevice.jar";
    private static String apiKey;

    private UserStorage userStorage;

    public static ServerAuth getInstance() {
        ServerAuth instance = INSTANCE;
        if (instance != null)
            return instance;

        synchronized (ServerAuth.class) {
            if (instance == null)
                instance = new ServerAuth();
            return instance;
        }
    }

    private ServerAuth() {
        userStorage = new UserStorage(USER_FILEPATH);
    }

    public boolean isUserRegistered(String user) {
        userStorage.readLock();
        try {
            return userStorage.isUserRegistered(user);
        } finally {
            userStorage.readUnlock();
        }
    }

    public boolean registerUser(String user, String certPath) {
        userStorage.writeLock();
        try {
            return userStorage.registerUser(user, certPath);
        } finally {
            userStorage.writeUnlock();
        }
    }

    public String userCertPath(String user) {
        userStorage.readLock();
        try {
            return userStorage.userCertPath(user);
        } finally {
            userStorage.readUnlock();
        }
    }

    public static long generateNonce() {
        return ThreadLocalRandom.current().nextLong();
    }

    public static void setApiKey(String key) {
        apiKey = key;
    }

    public static int generate2FACode() {
        return ThreadLocalRandom.current().nextInt(0, 100000);
    }

    public static int send2FAEmail(String emailAddress, int code) {
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

    public void saveCertificateInFile(String user, Certificate cert) {
        try {
            Utils.initializeFile(Utils.certPathFromUser(user));
            FileOutputStream os = new FileOutputStream(Utils.certPathFromUser(user));
            os.write("-----BEGIN CERTIFICATE-----\n".getBytes("US-ASCII"));
            os.write(Base64.getEncoder().encode(cert.getEncoded()));
            os.write("-----END CERTIFICATE-----\n".getBytes("US-ASCII"));
            os.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CertificateEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean verifySignedNonce(byte[] signedNonce, Certificate cert, long nonce)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
        Signature signature = Signature.getInstance("MD5withRSA");
        signature.initVerify(cert);
        signature.update(ByteBuffer.allocate(Long.BYTES).putLong(nonce).array());
        return signature.verify(signedNonce);
    }

    public static boolean verifyAttestationHash(byte[] hash, long nonce)
            throws IOException, NoSuchAlgorithmException {
        final int CHUNK_SIZE = 1024;

        long clientExecSize = new File(CLIENT_EXEC_PATH).length();
        FileInputStream clientExecInStream = new FileInputStream(CLIENT_EXEC_PATH);
        MessageDigest md = MessageDigest.getInstance("SHA");

        long leftToRead = clientExecSize;
        while (leftToRead >= CHUNK_SIZE) {
            md.update(clientExecInStream.readNBytes(CHUNK_SIZE));
            leftToRead -= CHUNK_SIZE;
        }
        md.update(clientExecInStream.readNBytes(Long.valueOf(leftToRead)
                .intValue()));
        md.update(Utils.longToByteArray(nonce));

        clientExecInStream.close();

        byte[] computedHash = md.digest();
        return MessageDigest.isEqual(hash, computedHash);
    }
}
