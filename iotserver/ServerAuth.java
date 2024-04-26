package iotserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Cipher;

import iohelper.CipherHelper;
import iohelper.Utils;

public class ServerAuth {
    private static volatile ServerAuth INSTANCE;

    private static final String HASH_KEY_ALIAS = "files-hash-key";
    private static final String MAC_ALGORITHM = "HmacSHA256";
    private static final String HMAC_FILE_PATH = "./output/server/attestation-sha";
    private static final String USER_FILEPATH = "./output/server/user.txt";
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
        return ThreadLocalRandom.current().nextInt(10000, 100000);
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
            throws IOException, NoSuchAlgorithmException{
        final int CHUNK_SIZE = 1024;
        String clientExecPath = Utils.getAttestationPath();
        long clientExecSize = new File(clientExecPath).length();
        FileInputStream clientExecInStream = new FileInputStream(clientExecPath);
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

        File hmac = new File(HMAC_FILE_PATH);
        boolean validHmac = false;
        boolean validDigest = MessageDigest.isEqual(hash, computedHash);
        try {
            if (hmac.exists()){ // if there is a hmac check
                validHmac = CipherHelper.verifyHmac(Base64.getEncoder().encodeToString(hash),
                                         HASH_KEY_ALIAS, MAC_ALGORITHM, HMAC_FILE_PATH);
            }else if (validDigest){ // if it's the first time + digest valid
                validHmac = true;
                CipherHelper.writeHmacToFile(
                    CipherHelper.computeFileHash(Base64.getEncoder().encodeToString(hash),
                                        HASH_KEY_ALIAS, MAC_ALGORITHM), HMAC_FILE_PATH);
            }

        } catch (UnrecoverableKeyException | InvalidKeyException | KeyStoreException | NoSuchAlgorithmException
                | CertificateException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // check if the digest is more or less
        return validDigest && validHmac;
    }
}
