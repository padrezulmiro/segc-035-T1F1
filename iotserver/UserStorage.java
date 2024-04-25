package iotserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import iohelper.Utils;

public class UserStorage {
    private final static String PBE_PARAMS_PATH = "pbe-params";
    private final static String PBE_ALGORITHM = "PBEWithHmacSHA256AndAES_128";
    private final static int SALT_LENGTH = 8;

    // FIXME(azul) Init these values
    private SecretKey pbeKey;
    private AlgorithmParameters pbeParams;

    private Map<String, String> users;
    private File usersFile;
    private Lock wLock;
    private Lock rLock;
    private String cipherPwd;

    public UserStorage(String usersFilePath, String cipherPwd) {
        usersFile = new File(usersFilePath);
        this.cipherPwd = cipherPwd;

        users = new HashMap<>();
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
        wLock = rwLock.writeLock();
        rLock = rwLock.readLock();

        try {
            initPBEParams();
            usersFile.createNewFile();
            populateUsersFromFile();
        } catch (IOException |
                 NoSuchAlgorithmException |
                 InvalidKeySpecException |
                 NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public boolean registerUser(String user, String certPath) {
        boolean ret = users.put(user, certPath) != null;
        updateUsersFile();
        return ret;
    }

    public boolean isUserRegistered(String user) {
        return users.containsKey(user);
    }

    public String userCertPath(String user) {
        return users.get(user);
    }

    public void readLock() {
        rLock.lock();
    }

    public void readUnlock() {
        rLock.unlock();
    }

    public void writeLock() {
        wLock.lock();
    }

    public void writeUnlock() {
        wLock.unlock();
    }

    private void updateUsersFile() {
        final String NL = "\n";
        final String SEP = ":";

        StringBuilder sb = new StringBuilder();
        for (String user: users.keySet()) {
            sb.append(user + SEP + users.get(user) + NL);
        }

        try {
            encryptToFile(sb.toString());
        } catch (InvalidKeyException |
                 NoSuchAlgorithmException |
                 NoSuchPaddingException |
                 InvalidAlgorithmParameterException |
                 IOException e) {
            e.printStackTrace();
        }
    }

    private void populateUsersFromFile() throws IOException {
        String[] lines = null;
        try {
            lines = decryptLinesFromFile();
        } catch (InvalidKeyException |
                 NoSuchAlgorithmException |
                 NoSuchPaddingException |
                 InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        for (String line: lines) {
            String[] tokens  = Utils.split(line, ':');
            String user = tokens[0];
            String certPath = tokens[1];
            registerUser(user, certPath);
        }
    }

    private void initPBEParams() throws IOException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException {
        ByteBuffer paramsBB = ByteBuffer
            .wrap(Files.readAllBytes(Paths.get(PBE_PARAMS_PATH)));

        int pbeIterations = paramsBB.getInt();

        byte[] pbeSalt = new byte[SALT_LENGTH];
        paramsBB.get(pbeSalt);

        byte[] pbeAlgorithmParameters = new byte[paramsBB.remaining()];
        paramsBB.get(pbeAlgorithmParameters);

        PBEKeySpec keySpec = new PBEKeySpec(cipherPwd.toCharArray(), pbeSalt,
                pbeIterations);
        SecretKeyFactory kf = SecretKeyFactory.getInstance(PBE_ALGORITHM);
        pbeKey = kf.generateSecret(keySpec);

        pbeParams = AlgorithmParameters.getInstance(PBE_ALGORITHM);
        pbeParams.init(pbeAlgorithmParameters);
    }

    private String[] decryptLinesFromFile() throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, FileNotFoundException {
        Cipher cipher = Cipher.getInstance(PBE_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParams);

        CipherInputStream cis =
            new CipherInputStream(new FileInputStream(usersFile), cipher);

        BufferedReader reader = new BufferedReader(new InputStreamReader(cis));
        return reader.lines().toArray(String[]::new);
    }

    private void encryptToFile(String body) throws NoSuchAlgorithmException,
            IOException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance(PBE_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParams);

        new PrintWriter(usersFile).close();

        CipherOutputStream cos =
            new CipherOutputStream(new FileOutputStream(usersFile), cipher);
        cos.write(body.getBytes());
    }
}
