package iohelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import iotserver.ServerConfig;

public class CipherHelper {

    // need a function to save salt and key
    public static byte[] generateSalt(){
        SecureRandom rd = new SecureRandom();
        byte[] salt = new byte[128];
        rd.nextBytes(salt);
        return salt;
    }

    // public static int generateIterations(){
    //     SecureRandom rd = new SecureRandom();
    //     return rd.nextInt(20);
    // }

    /**
     * Attempt to read symmetric cipher key's parameters from file, given a domain name.
     * If the domain didn't exist, it will generate params and populate the file.
     * @param paramFilePath
     * @param domainName
     * @return
     * @throws IOException
     */
    private static SymmetricCipherParams getParams(String paramFilePath, String domainName) throws IOException{
        final char SP = ':';

        File paramFile = new File(paramFilePath);
        if (paramFile.createNewFile()){
            // no salt
            SymmetricCipherParams params = new SymmetricCipherParams(generateSalt());
            BufferedWriter writer = FileHelper.createFileWriter(paramFile);
            writer.write(domainName + SP + params.toString());
            writer.close();
            return params;
        }else{
            // read salt
            BufferedReader reader = new BufferedReader(new FileReader(paramFile));
            String line = (String) reader.readLine();
            reader.close();
            String[] tokens = Utils.split(line, SP);
            byte[]salt = Base64.getDecoder().decode(tokens[1]);
            int iterations = Integer.parseInt(tokens[2]);
            return new SymmetricCipherParams(salt, iterations);
        }
    }


    /*
     * uses PBE with AES 128 bits
     */
    public static SecretKey getSecretKeyFromPwd(String domainName, String pwd, String paramFilePath) 
                    throws NoSuchAlgorithmException, InvalidKeySpecException, IOException{
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        // SymmetricCipherParams params = getParams(paramFilePath, domainName);
        SymmetricCipherParams params = getParams(paramFilePath, domainName);
        byte[] salt = params.getSalt();
        int iterations = params.getIterations();
        KeySpec spec = new PBEKeySpec(pwd.toCharArray(), salt, iterations, 128);
        // SecretKey sKey = factory.generateSecret(spec);
        SecretKey sKey = factory.generateSecret(spec);
        System.out.println(sKey.getEncoded().length); // Prints 16
        return new SecretKeySpec(sKey.getEncoded(),"AES");
    }


    public static void handleFileAES_ECB(int opmode, SecretKey key, File inputFile, File outputFile)
        throws IOException, NoSuchPaddingException, NoSuchAlgorithmException,
                InvalidAlgorithmParameterException, InvalidKeyException, 
                BadPaddingException, IllegalBlockSizeException {
    
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(opmode, key);

        FileInputStream in = new FileInputStream(inputFile);
        FileOutputStream out = new FileOutputStream(outputFile);

        byte[] buffer = new byte[64];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            byte[] outputBuffer = c.update(buffer, 0, bytesRead);
            if (outputBuffer != null) {
                out.write(outputBuffer);
            }
        }
        byte[] outputBytes = c.doFinal();
        if (outputBytes != null) {
            out.write(outputBytes);
        }

        in.close();
        out.close();
    }

    /**
     * Encrypting data to be sent to server
     * @param key secret key of the domain it is sending to
     * @param plainData data to be sent, represented in a byte array
     * @return encrypted data in a byte array
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static byte[] encryptAES_ECB(SecretKey key, byte[] plainData) 
                throws NoSuchAlgorithmException, NoSuchPaddingException,
                       InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        // System.out.println("double check: " + key.length());
        c.init(Cipher.ENCRYPT_MODE, key);
        return c.doFinal(plainData);
    }

    /**
     * Decrypting data sent from the server to be used
     * @param key secret key of the domain data was from
     * @param cipherData encrypted data to be decrypted and use
     * @return decrypted plain data in a byte array
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static byte[] decryptAES_ECB(SecretKey key, byte[] cipherData)
                throws NoSuchAlgorithmException, NoSuchPaddingException,
                       InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, key);
        return c.doFinal(cipherData);
    }

    /**
     * Load keystore from drive
     * @param keystorePath the path for the keystore
     * @param keystorePwd password used for the keystore
     * @return KeyStore object
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     * @throws KeyStoreException
     */
    public static KeyStore getKeyStore(String keystorePath, String keystorePwd)
                throws NoSuchAlgorithmException, CertificateException,
                                        IOException,  KeyStoreException{
        FileInputStream kfile = new FileInputStream(keystorePath); 
        KeyStore kstore = KeyStore.getInstance("JCEKS");
        kstore.load(kfile, keystorePwd.toCharArray()); //this. is wrong?
        kfile.close();
        return kstore;
    }

    /**
     * Unwrapping a key with a given private key, using RSA
     * @param priKey private key of the user
     * @param wrappedKey a wrapped key to be unwrapped
     * @return secret key object
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException 
     */
    public static Key unwrapSkey(PrivateKey priKey, byte[] wrappedKey)
        throws NoSuchAlgorithmException, NoSuchPaddingException,
        InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        // c.init(Cipher.UNWRAP_MODE, priKey);
        // return c.unwrap(wrappedKey, "PBKDF2WithHmacSHA256", Cipher.SECRET_KEY);
        c.init(Cipher.DECRYPT_MODE, priKey);
        return  new SecretKeySpec ( c.doFinal(wrappedKey), "AES" );
    }

    /**
     * Wrapping a key with a public key, using RSA
     * @param pubKey public key of the user
     * @param sharedKey secret key to be wrapped
     * @return wrapped key as a byte array
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException 
     */
    public static byte[] wrapSkey(PublicKey pubKey, Key sharedKey)
        throws NoSuchAlgorithmException, NoSuchPaddingException,
                    InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.ENCRYPT_MODE, pubKey);
        // cifrar a chave secreta que queremos enviar
        // byte[] wrappedKey = c.wrap(sharedKey);
        byte[] wrappedKey = c.doFinal(sharedKey.getEncoded());
        return wrappedKey;
    }

    public static byte[] computeFileHash(String body, String alias, String algorithm) throws KeyStoreException,
                NoSuchAlgorithmException, CertificateException, IOException,
                UnrecoverableKeyException, InvalidKeyException {
        String keyStorePath = ServerConfig.getInstance().keyStorePath();
        String keyStorePwd = ServerConfig.getInstance().keyStorePwd();
        KeyStore ks = CipherHelper.getKeyStore(keyStorePath, keyStorePwd);
        Key key = ks.getKey(alias, keyStorePwd.toCharArray());
        Mac mac = Mac.getInstance(algorithm);
        mac.init(key);
        mac.update(body.getBytes());
        byte[] ret = mac.doFinal();
        return ret;
    }

    public static boolean verifyHmac(String target, String alias,
        String algorithm, String filepath) throws IOException,
        UnrecoverableKeyException, InvalidKeyException, KeyStoreException,
        NoSuchAlgorithmException, CertificateException {
        File f = new File(filepath);
        f.createNewFile();
        byte[] hmac = Files.readAllBytes(Paths.get(filepath));
        System.out.println(" written HMAC: " + Base64.getEncoder().encodeToString(hmac));
        byte[] readHmac = computeFileHash(target,alias,algorithm);
        System.out.println("computed HMAC: " + Base64.getEncoder().encodeToString(readHmac));
        return Arrays.equals(hmac, readHmac);
    }

    public static void writeHmacToFile(byte[] hmac, String filepath) throws IOException {
        new PrintWriter(filepath).close(); // Empties Hmac file
        FileOutputStream fos = new FileOutputStream(filepath);
        fos.write(hmac);
        fos.close();
    }

}
