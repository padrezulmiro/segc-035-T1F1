package iotserver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
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
