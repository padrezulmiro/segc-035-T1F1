package iohelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
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


    // private static byte[] getParameters(String paramFilePath, String domainName) throws IOException{

    //     File paramFile = new File(paramFilePath);
    //     if(!paramFile.createNewFile()){
    //         // param file already exist
    //         BufferedReader reader = new BufferedReader(new FileReader(paramFile));
    //         String line = (String) reader.readLine();
    //         return Base64.getDecoder().decode(line);
    //     }

    //     // // If the params don't exist
    //     // SymmetricCipherParams params = new SymmetricCipherParams(generateSalt());
    //     // BufferedWriter writer = FileHelper.createFileWriter(paramFile);
    //     // writer.write(domainName + SP + params.toString());
    //     // writer.close();
    //     return null;
    // }

    /*
     * uses PBE with AES 128 bits
     */
    public static SecretKey getSecretKeyFromPwd(String domainName, String pwd, String paramFilePath) 
                    throws NoSuchAlgorithmException, InvalidKeySpecException, IOException{
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
        // SymmetricCipherParams params = getParams(paramFilePath, domainName);
        SymmetricCipherParams params = getParams(paramFilePath, domainName);
        byte[] salt = params.getSalt();
        int iterations = params.getIterations();
        KeySpec spec = new PBEKeySpec(pwd.toCharArray(), salt, iterations, 128);
        SecretKey sKey = factory.generateSecret(spec);
        return sKey;
    }

    //PBEWithHmacSHA256AndAES_128
    // TODO: figure out what type of data will be en/decrypted
    // Be mindful of buffering when using 
    public static byte[] encrypt(String algorithm, PublicKey key, byte[] input) 
                throws NoSuchAlgorithmException, NoSuchPaddingException,
                       InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        Cipher c = Cipher.getInstance(algorithm);
        c.init(Cipher.ENCRYPT_MODE, key); // this line is fucked
        c.getParameters();
        byte[] cipherData = c.doFinal(input);
        return cipherData;
    }

    public static byte[] decrypt(String algorithm, PrivateKey key, byte[] cipherData)
                throws NoSuchAlgorithmException, NoSuchPaddingException,
                       InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        Cipher c = Cipher.getInstance(algorithm);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = c.doFinal(cipherData);
        return plainText;
    }

    /**
     * Load keystore from drive
     * @param keystorePath
     * @param keystorePwd
     * @return
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
     * @param priKey
     * @param wrappedKey
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     */
    public static Key unwrap(PrivateKey priKey, byte[] wrappedKey)
        throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException{
        Cipher c = Cipher.getInstance("RSA");
        c.init(Cipher.UNWRAP_MODE, priKey);
        return c.unwrap(wrappedKey, "PBEWithHmacSHA256AndAES_128", Cipher.SECRET_KEY);
    }

    /**
     * Wrapping a key with a public key, using RSA
     * @param pubKey
     * @param sharedKey
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     */
    public static byte[] wrapSkey(PublicKey pubKey, Key sharedKey)
        throws NoSuchAlgorithmException, NoSuchPaddingException,
                    InvalidKeyException, IllegalBlockSizeException{
        Cipher c = Cipher.getInstance("RSA");
        c.init(Cipher.WRAP_MODE, pubKey);
        // cifrar a chave secreta que queremos enviar
        byte[] wrappedKey = c.wrap(sharedKey);
        return wrappedKey;
    }

}
