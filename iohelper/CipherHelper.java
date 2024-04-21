package iohelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

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

    private static SymmetricCipherParams getParams(String paramFilePath) throws IOException{
        File paramFile = new File(paramFilePath);
        BufferedReader output = FileHelper.createFileReader(paramFile);
        String[] lines = (String[]) output.lines().toArray(String[]::new);
        output.close();
        if (lines.length>0){
            byte[]salt = Base64.getDecoder().decode(lines[0]);
            int iterations = Integer.parseInt(lines[1]);
            return new SymmetricCipherParams(salt, iterations);
        }else{
            return new SymmetricCipherParams(generateSalt());
        }
    }

    /*
     * uses PBE with AES 128 bits
     */
    public static SecretKey getKeyFromPwd(String pwd, String paramFilePath) 
                    throws NoSuchAlgorithmException, InvalidKeySpecException, IOException{
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
        SymmetricCipherParams params = getParams(paramFilePath);
        byte[] salt = getParams(paramFilePath).getSalt();
        int iterations = params.getIteration();
        KeySpec spec = new PBEKeySpec(pwd.toCharArray(), salt, iterations, 128);

        File paramFile = new File(paramFilePath);
        BufferedWriter output = FileHelper.createFileWriter(paramFile);
        output.write(Base64.getEncoder().encodeToString(salt));
        output.write(System.getProperty("line.separator"));
        output.write(iterations);
        // File f = FileHelper.receiveFile( paramFilePath, null);
        
        SecretKey sKey = factory.generateSecret(spec);
        return sKey;
    }

    public static String encryptString(String algorithm, PublicKey key, String input)
            throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
        byte[] encryptedData = encrypt(algorithm, key, Base64.getDecoder().decode(input));
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    public static String decryptString(String algorithm, PublicKey key, String cipherText)
            throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
        byte[] PlainData = decrypt(algorithm, key, Base64.getDecoder().decode(cipherText));
        return Base64.getEncoder().encodeToString(PlainData);
    }

    //PBEWithHmacSHA256AndAES_128
    // TODO: figure out what type of data will be en/decrypted
    // Be mindful of buffering when using 
    public static byte[] encrypt(String algorithm, PublicKey key, byte[] input) 
                throws NoSuchAlgorithmException, NoSuchPaddingException,
                       InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        Cipher c = Cipher.getInstance(algorithm);
        c.init(Cipher.ENCRYPT_MODE, key); // this line is fucked
        byte[] cipherData = c.doFinal(input);
        return cipherData;
    }

    public static byte[] decrypt(String algorithm, PublicKey key, byte[] cipherData)
                throws NoSuchAlgorithmException, NoSuchPaddingException,
                       InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        Cipher c = Cipher.getInstance(algorithm);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = c.doFinal(cipherData);
        return plainText;
    }

    public static void encryptAsym(PublicKey pK, PrivateKeyEntry pirK){}

    
    public static void decryptAsym(){}

    // public static 

    /*
     * Load keystore from drive
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

}
