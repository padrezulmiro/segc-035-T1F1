package iotclient;

import iohelper.CipherHelper;
import iohelper.FileHelper;
import iohelper.Utils;
import iotserver.ServerResponse;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.InvalidAlgorithmParameterException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Um cliente IoT é um programa – chamemos-lhe IoTDevice – que representa um
 * dispositivo de sensorização.
 */
public class IoTDevice {
    private static final int DEFAULT_PORT = 12345;
    private static final int ARG_NUM = 6;

    static String userid;
    static String devid;
    static SSLSocket clientSocket = null;
    static String serverAddress;
    static String truststore;
    static String keystore;
    static String psw_keystore;
    static ObjectInputStream in;
    static ObjectOutputStream out;
    static KeyStore trustStore;
    static KeyStore keyStore;
    static PrivateKey privateKey;
    static int iterationRounds;

    private static String baseDir; // defined after userid is given
    private static String domkeyParamPath;
    private static final String deviceJar = "IoTDevice.jar";

    private static Scanner sc;

    public static void main(String[] args) throws IOException, InterruptedException {
        addCliShutdownHook();
        sc = new Scanner(System.in);
        // Check arguments
        if (args.length < ARG_NUM) {
            System.out.println(
                    "Error: not enough args!\nUsage: IoTDevice <serverAddress> <truststore> <keystore> <passwordkeystore> <dev-id> <user-id>\n");
            System.exit(1);
        }
        Pattern p = Pattern.compile(".+@{1}.+");
        Matcher m = p.matcher(args[5]);
        if (!m.matches()) {
            System.out.println(
                    "Error: user-id should be an email.\n");
            System.exit(1);
        }

        serverAddress = args[0];
        truststore = args[1];
        keystore = args[2];
        psw_keystore = args[3];
        devid = args[4];
        userid = args[5];

        System.setProperty("javax.net.ssl.trustStore", truststore);
        System.setProperty("javax.net.ssl.trustStorePassword", "");
        System.setProperty("javax.net.ssl.trustStoreType", "JCEKS");
        System.setProperty("javax.net.ssl.keyStore", keystore);
        System.setProperty("javax.net.ssl.keyStorePassword", psw_keystore);
        System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        try {
            trustStore = CipherHelper.getKeyStore(truststore, "iotclient");
            keyStore = CipherHelper.getKeyStore(keystore, psw_keystore);
            privateKey = (PrivateKey) keyStore.getKey(userid, psw_keystore.toCharArray());
        } catch (NoSuchAlgorithmException | CertificateException | 
                    KeyStoreException | IOException | UnrecoverableKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } //TODO: get rid of trustStore password

        baseDir = "./output/" + userid + "/"; 
        domkeyParamPath = baseDir + "domkey/";
        new File(domkeyParamPath).mkdirs();

        // Connection & Authentication
        if (connect(serverAddress)) {
            twoFactorAuth(userid);
            remoteAttestation(devid);
            printMenu();
            // Program doesn't end until CTRL+C is pressed
            while (true) {// Steps 8 - 10
                System.out.print("> ");
                while (System.in.available() <= 0) Thread.sleep(500);
                String command = sc.nextLine();
                try {
                    executeCommand(command);
                } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException |
                        IllegalBlockSizeException | BadPaddingException | KeyStoreException |
                        InvalidKeySpecException | ClassNotFoundException | IOException |
                        InvalidAlgorithmParameterException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private static void sendAttestationHash(long nonce) {
        try {
            final int CHUNK_SIZE = 1024;
            String clientExecPath = Utils.getAttestationPath();
            long clientExecSize = new File(clientExecPath).length();
            FileInputStream clientFIS;
            clientFIS = new FileInputStream(clientExecPath);
            MessageDigest md = MessageDigest.getInstance("SHA");

            long leftToRead = clientExecSize;
            while (leftToRead >= CHUNK_SIZE) {
                md.update(clientFIS.readNBytes(CHUNK_SIZE)); // MessageDigest.update *appends* the byte array provided
                leftToRead -= CHUNK_SIZE;
            }

            md.update(clientFIS.readNBytes(Long.valueOf(leftToRead).intValue()));
            md.update(ByteBuffer.allocate(8).putLong(nonce).array());
            clientFIS.close();
            byte[] attestationHash = md.digest();
            out.writeObject(attestationHash);

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void twoFactorAuth(String user) throws InterruptedException {

        try {
            boolean auth = false;
            do {
                // FACTOR 1 - Auth based on asymmetric encryption
                System.out.println("Starting authentication.");
                out.writeObject(MessageCode.AU);
                // Send user id
                out.writeObject(user);
                // Receive nonce from server
                long nonce = in.readLong();

                FileInputStream kfile = new FileInputStream(keystore);
                KeyStore kstore = KeyStore.getInstance("JCEKS");
                kstore.load(kfile, psw_keystore.toCharArray());
                PrivateKey privKey = (PrivateKey) kstore.getKey(user, psw_keystore.toCharArray());

                // MessageCode used as flag
                MessageCode code = (MessageCode) in.readObject();
                switch (code) {
                    case OK_NEW_USER:
                        System.out.println(MessageCode.OK_NEW_USER.getDesc());
                        // Send nonce
                        out.writeLong(nonce);
                        // Send nonce signed with private key
                        sendSignedNonce(user, nonce, privKey);

                        // Send certificate with public key
                        Certificate cert = kstore.getCertificate(user);
                        out.writeObject(cert);

                        // Receive confirmation
                        // handle WRONG_NONCE
                        if (!in.readObject().equals(MessageCode.OK)) {
                            System.exit(-1);
                        }
                        System.out.println(MessageCode.OK.getDesc());
                        break;
                    case OK_USER:
                        System.out.println(MessageCode.OK_USER.getDesc());
                        // Send nonce signed with private key
                        sendSignedNonce(user, nonce, privKey);

                        // Receive confirmation
                        if (!in.readObject().equals(MessageCode.OK)) {
                            System.exit(-1);
                        }
                        System.out.println(MessageCode.OK.getDesc());
                        break;
                    default:
                        System.out.println("Read incorrect code from server.");
                        System.exit(-1);
                        break;
                }

                // FACTOR 2 - Email auth
                MessageCode emailCode;
                do {
                    System.out.println("Check your email for an authentication code.");
                    ;
                    System.out.print("> Code: ");

                    while (System.in.available() <= 0) Thread.sleep(500);

                    String c2fa = sc.nextLine();
                    out.writeInt(Integer.valueOf(c2fa));
                    out.flush();
                    // receive code
                    emailCode = (MessageCode) in.readObject();
                } while (emailCode.equals(MessageCode.EMAIL_FAIL));

                switch (emailCode) {
                    case OK:
                        auth = true;
                        System.out.println("Auhentication ok.");
                        break;
                    case NOK:
                        System.out.println("Auhentication has failed, it'll now restart.");
                        break;
                    default:
                        break;
                }

            } while (!auth);

        } catch (IOException | ClassNotFoundException | NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void remoteAttestation(String deviceID) {
        try {
            System.out.println("Starting remote attestation.");
            out.writeObject(MessageCode.AD);
            out.writeObject(deviceID);

            MessageCode code = (MessageCode) in.readObject();
            switch (code) {
                case NOK_DEVID:
                    System.out.println(MessageCode.NOK_DEVID.getDesc());
                    System.exit(-1); // Connection is shut down by the shutdown hook.
                    break;
                case OK_DEVID:
                    System.out.println(MessageCode.OK_DEVID.getDesc());
                    out.writeObject(MessageCode.TD);
                    long nonce = in.readLong();
                    sendAttestationHash(nonce);
                    MessageCode attestCode = (MessageCode) in.readObject();
                    switch (attestCode) {
                        case NOK_TESTED:
                            System.out.println(MessageCode.NOK_TESTED.getDesc());
                            System.exit(-1);
                            break;
                        case OK_TESTED:
                            System.out.println(MessageCode.OK_TESTED.getDesc());
                            break;
                        default:
                            break;
                    }
                default:
                    break;
            }
        } catch (IOException e) {
            System.err.println("ERROR" + e.getMessage());
            System.exit(-1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void sendSignedNonce(String user, long nonce, PrivateKey privKey) {
        try { // Send nonce signed with private key

            Signature signature = Signature.getInstance("MD5withRSA");
            signature.initSign(privKey);
            byte[] nonceBytes = ByteBuffer.allocate(8).putLong(nonce).array(); // TODO: check this
            signature.update(nonceBytes);
            byte[] signedNonce = signature.sign();
            out.writeObject(signedNonce);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SignatureException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void addCliShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down.\n");
            try {
                if (out != null) {
                    out.writeObject(MessageCode.STOP);
                }

                // Close socket
                if (clientSocket != null) {
                    clientSocket.close();
                }
                sc.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }));
    }

    /**
     * Takes a string and executes the corresponding command.
     * 
     * @param command
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException 
     * @throws NoSuchPaddingException 
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     * @throws IOException 
     * @throws ClassNotFoundException 
     * @throws InvalidKeySpecException 
     * @throws KeyStoreException 
     * @throws InvalidAlgorithmParameterException 
     */
    private static void executeCommand(String command)
        throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
                IllegalBlockSizeException, BadPaddingException, KeyStoreException,
                InvalidKeySpecException, ClassNotFoundException, IOException,
                InvalidAlgorithmParameterException {
        // First word is the command
        String[] cmds = command.split(" ");
        String op = cmds[0].toUpperCase();

        switch (op) {
            case "C":
            case "CREATE":
                if (cmds.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: CREATE <dm>");
                } else {
                    createDomain(cmds[1]);
                }
                break;
            case "A":
            case "ADD":
                if (cmds.length != 4) {
                    System.out.println("Error: incorrect args\nUsage: ADD <user1> <dm> <dm-pwd>");
                } else {
                    addUser(cmds[1], cmds[2], cmds[3]);
                }
                break;
            case "RD":
                if (cmds.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: RD <dm>");
                } else {
                    registerDevice(cmds[1]);
                }
                break;
            case "ET":
                if (cmds.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: ET <float>");
                } else {
                    sendTemperature(cmds[1]);
                }
                break;
            case "EI":
                if (cmds.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: EI <filename.jpg>");
                } else {
                    sendImage(cmds[1]);
                }
                break;
            case "RT":
                if (cmds.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: RT <dm>");
                } else {
                    receiveTemps(cmds[1]);
                }
                break;
            case "RI":
                if (cmds.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: RI <user-id>:<dev_id>");
                } else {
                    receiveImage(cmds[1]);
                }
                break;
            case "MYDOMAINS":
                showDomains();
                break;
            case "H":
            case "HELP":
                printMenu();
                break;
            default:
                System.out.println("That command does not exist.");
                break;
        }
    }

    private static void showDomains() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, ClassNotFoundException, IOException {
        out.writeObject(MessageCode.MYDOMAINS);
        ServerResponse sResponse = (ServerResponse) in.readObject();
        Set<String> doms = sResponse.domains();
            if (doms.isEmpty()){
                System.out.println("This device doesn't belong to any domains.");
            } else {
                System.out.println("This device belongs to the following domains:");
                Iterator itr = doms.iterator();
 
                while (itr.hasNext()) {
                    System.out.println("\t - " + itr.next());
            }
        }
    }

    /**
     * 
     * @param device
     * @throws NoSuchPaddingException 
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     * @throws IllegalBlockSizeException 
     * @throws BadPaddingException 
     * @throws InvalidAlgorithmParameterException 
     */
    private static void receiveImage(String device)
        throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
                InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
        try {
            out.writeObject(MessageCode.RI); // Send opcode
            String[] dev = device.split(":");
            if (dev.length!=2){
                System.out.println("Incorrect user:dev format.");
                return;
            }
            out.writeObject(dev[0]);
            out.writeObject(dev[1]);
            // Receive message
            MessageCode code = (MessageCode) in.readObject();
            switch (code) {
                case OK:
                    String enDomkey = (String) in.readObject();
                    // String s = (String) in.readObject(); // This is discarded
                    // long fileSize = (long) in.readObject(); // Read file size
                    // String[] dev = device.split(":");
                    String enFilePath = baseDir + "en_" + dev[0] + "_" + dev[1] + ".jpg";
                    File encryptedFile = new File(enFilePath);
                    FileHelper.receiveFile(encryptedFile, in);
                    String plainFilePath = baseDir + dev[0] + "_" + dev[1] + ".jpg";
                    File plainFile = new File(plainFilePath);

                    // getting secret key, using it to decrypt
                    SecretKey skey = (SecretKey) CipherHelper.unwrapSkey(privateKey, Base64.getDecoder().decode(enDomkey));
                    CipherHelper.handleFileAES_ECB(Cipher.DECRYPT_MODE, skey, encryptedFile, plainFile);
                    encryptedFile.delete();

                    System.out.println(MessageCode.OK.getDesc() + ", received file " + plainFile.length() + " bytes"); 
                    break;
                case NODATA:
                    System.out.println(MessageCode.NODATA.getDesc());
                    break;
                case NOPERM:
                    System.out.println(MessageCode.NOPERM.getDesc());
                    break;
                case NOID:
                    System.out.println(MessageCode.NOID.getDesc());
                    break;
                default:
                    break;
            }
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void receiveTemps(String domain)
        throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
               IllegalBlockSizeException{
        try {
            out.writeObject(MessageCode.RT); // Send opcode
            out.writeObject(domain);
            // Receive message
            MessageCode code = (MessageCode) in.readObject();
            switch (code) {
                case OK:
                    // Long fileSize = (long) in.readObject(); // Read file size
                    ServerResponse sResponse = (ServerResponse) in.readObject();

                    String enDomkey = sResponse.encryptedDomainKey();
                    SecretKey sKey = (SecretKey) CipherHelper.unwrapSkey(privateKey, Base64.getDecoder().decode(enDomkey));


                    HashMap<String, String> enTemps = (HashMap<String, String>) sResponse.temperatures();
                    for (@SuppressWarnings("unused")
                    String s : enTemps.keySet())
                        ;
                    for (@SuppressWarnings("unused")
                    String n : enTemps.values())
                        ;
                    // reason for the empty loops: https://stackoverflow.com/a/509288
                    // essentially ClassCastException will be thrown if any of the maps is bad

                    HashMap<String, String> temps =  new HashMap<String, String>();
                    for (String dev : enTemps.keySet()){
                        String enTemp = enTemps.get(dev);
                        String temp = new String(CipherHelper.decryptAES_ECB(sKey, Base64.getDecoder().decode(enTemp)));
                        temps.put(dev, temp);
                    }
                    // TODO: write it to file
                    File f = new File((baseDir + "temps_" + domain + ".txt"));
                    BufferedWriter output = FileHelper.createFileWriter(f);
                    for (Map.Entry<String, String> entry : temps.entrySet()) {
                        output.write(entry.getKey() + ":" + entry.getValue() + System.getProperty("line.separator"));
                        output.flush();
                    }
                    // FileHelper.receiveFile(fileSize, fileName,in);
                    System.out.println(MessageCode.OK.getDesc() + ", " + f.length() + " (long)");
                    break;
                case NODATA:
                    System.out.println(MessageCode.NODATA.getDesc());
                    break;
                case NODM:
                    System.out.println(MessageCode.NODM.getDesc());
                    break;
                case NOPERM:
                    System.out.println(MessageCode.NOPERM.getDesc());
                    break;
                default:
                    break;
            }
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            System.out.println(MessageCode.BADPASS.getDesc());
            e.printStackTrace();
        }
    }

    private static void sendImage(String imagePath)
        throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
                IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        try {
            out.writeObject(MessageCode.EI); // Send opcode

            //get (domain,encrypted keys) map
            HashMap<String,String> enDomkeysMap = getDeviceEncryptedDomainKeys();

            if (enDomkeysMap.entrySet().size()==0){
                System.out.println(MessageCode.NOTREGISTER.getDesc());
                return;
            }

            for (String dom : enDomkeysMap.keySet()){

                // getting secret key
                String enDomkey = enDomkeysMap.get(dom);
                byte[] wrappedKey = Base64.getDecoder().decode(enDomkey);
                SecretKey sKey = (SecretKey) CipherHelper.unwrapSkey(privateKey, wrappedKey);

                // write domain name
                out.writeObject(dom);                

                // encrypt file with secret key
                File plainFile = new File(imagePath);
                if(!plainFile.exists()){
                    System.out.println(MessageCode.FILENOTFOUND);
                }

                String enFilePath = plainFile.getParent() + "en_" + plainFile.getName();
                File encryptedFile = new File(enFilePath);
                // encrypt the image, this file is the one sent
                CipherHelper.handleFileAES_ECB(Cipher.ENCRYPT_MODE,sKey, plainFile, encryptedFile);
                FileHelper.sendFile(encryptedFile, out); 
                encryptedFile.delete();

                // receive message
                MessageCode code = (MessageCode) in.readObject();
                switch (code) {
                    case OK:
                        System.out.println(MessageCode.OK.getDesc());
                        break;
                    case NOK:
                        System.out.println(MessageCode.NOK.getDesc());
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Sends a temperature value to the server.
     * 
     * @param temp
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException 
     * @throws NoSuchPaddingException 
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     */
    private static void sendTemperature(String temp){ // Should it test if the value can be converted to a float?
        try {
            //This is to trigger NumberFormatException if it wasn't a float
            Float.parseFloat(temp);

            out.writeObject(MessageCode.ET); // Send opcode

            //get (domain,encrypted keys) map
            HashMap<String,String> enDomkeysMap = getDeviceEncryptedDomainKeys();

            if (enDomkeysMap.entrySet().size()==0){
                System.out.println(MessageCode.NOTREGISTER.getDesc());
                return;
            }
            for (String dom : enDomkeysMap.keySet()){
                System.out.println("dom:"+dom);
                // getting encrypted dom keys
                String enDomkey = enDomkeysMap.get(dom);
                byte[] wrappedKey = Base64.getDecoder().decode(enDomkey);
                SecretKey sKey = (SecretKey) CipherHelper.unwrapSkey(privateKey, wrappedKey);
                // use sKey to encrypt temp
                byte[] encryptedTemp = CipherHelper.encryptAES_ECB(sKey, temp.getBytes());
                out.writeObject(dom);                
                out.writeObject(encryptedTemp);

                // receive message
                MessageCode code = (MessageCode) in.readObject();
                switch (code) {
                    case OK:
                        System.out.println(MessageCode.OK.getDesc());
                        break;
                    case NOK:
                        System.out.println(MessageCode.NOK.getDesc());
                        break;
                    default:
                        break;
                }
            }
            
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(NumberFormatException e) {
            System.out.println(MessageCode.BADFLOAT.getDesc());
            return;
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static HashMap<String,String> getDeviceEncryptedDomainKeys()
            throws ClassNotFoundException, IOException{
        out.writeObject(userid);
        out.writeObject(devid);
        // read a ServerResponse for confirmation + domkeys
        ServerResponse sr = (ServerResponse)in.readObject();
        return sr.allEncryptedDomainKeys();
    }

    private static void registerDevice(String domain) {
        try {
            out.writeObject(MessageCode.RD); // Send opcode
            out.writeObject(domain); // Send domain
            // Receive message
            MessageCode code = (MessageCode) in.readObject();
            switch (code) {
                case OK:
                    System.out.println(MessageCode.OK.getDesc());
                    break;
                case NOPERM:
                    System.out.println(MessageCode.NOPERM.getDesc());
                    break;
                case NODM:
                    System.out.println(MessageCode.NODM.getDesc());
                    break;
                case DEVICEEXISTS:
                    System.out.println(MessageCode.DEVICEEXISTS.getDesc());
                default:
                    break;
            }
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Asks server to add a specified user to a given domain.
     * 
     * @param user
     * @param domain
     * @throws KeyStoreException 
     * @throws IOException 
     * @throws InvalidKeySpecException 
     * @throws NoSuchAlgorithmException 
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException 
     * @throws NoSuchPaddingException 
     * @throws InvalidKeyException 
     * @throws ClassNotFoundException 
     */
    private static void addUser(String user, String domain, String domPwd)
        throws KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException,
                IOException, InvalidKeyException, NoSuchPaddingException, 
                IllegalBlockSizeException, BadPaddingException, ClassNotFoundException {
        // String domkeyLocation = domkeyParamPath + domain + ".txt";
        // get user's cert
        Certificate newUserCert = trustStore.getCertificate(user); 
        // get user's pk
        PublicKey pk = newUserCert.getPublicKey();
        // generate domkey with dompwd
        String domkeyLocation = domkeyParamPath + domain + ".txt";
        SecretKey skey = CipherHelper.getSecretKeyFromPwd(domain,domPwd,domkeyLocation);
        // encrypt domkey with pk of the new user
        byte[] enDomkey = CipherHelper.wrapSkey(pk,skey);

        out.writeObject(MessageCode.ADD); // Send opcode
        out.writeObject(user); // Send user
        out.writeObject(domain); // Send domain
        out.writeObject(Base64.getEncoder().encodeToString(enDomkey)); // SendDomKey to server
        // Receive message
        MessageCode code = (MessageCode) in.readObject();
        switch (code) {
            case OK:
                System.out.println(MessageCode.OK.getDesc());
                break;
            case NOPERM:
                System.out.println(MessageCode.NOPERM.getDesc());
                break;
            case NODM:
                System.out.println(MessageCode.NODM.getDesc());
                break;
            case NOUSER:
                System.out.println(MessageCode.NOUSER.getDesc());
                break;
            case USEREXISTS:
                System.out.println(MessageCode.USEREXISTS.getDesc());
            default:
                break;
        }
    }

    /**
     * Asks server to create a domain with the given {@code dmName} name.
     * 
     * @param dmName Domain name.
     */
    private static void createDomain(String dmName) {
        try {
            out.writeObject(MessageCode.CREATE); // Send opcode
            out.writeObject(dmName); // Send domain
            // Receive message
            MessageCode code = (MessageCode) in.readObject();
            switch (code) {
                case NOK:
                    System.out.println(MessageCode.NOK.getDesc());
                    break;
                case OK:
                    System.out.println(MessageCode.OK.getDesc());
                    break;
                default:
                    break;
            }
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Sends the name and size of the IoTDevice file to the server for remote
     * attestation.
     */
    private static void testDevice() {
        try {
            File iotFile = new File(deviceJar);
            // Send Test Device message
            out.writeObject(MessageCode.TD);
            // Send IoTDevice file name
            out.writeObject(iotFile.getName());
            // Send IoTDevice file size
            out.writeLong(iotFile.length());
            // Long mock_size = (long) 3;
            // out.writeLong(mock_size);
            // Receive message
            System.out.println(iotFile.getName() + ": " + iotFile.length());
            out.flush();
            MessageCode code = (MessageCode) in.readObject();
            switch (code) {
                case NOK_TESTED:
                    System.out.println(MessageCode.NOK_TESTED.getDesc());
                    System.exit(-1);
                    break;
                case OK_TESTED:
                    System.out.println(MessageCode.OK_TESTED.getDesc());
                    break;
                default:
                    break;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints the menu.
     */
    private static void printMenu() {
        System.out.println("\n");
        System.out.println("******** IoTDevice ********");
        System.out.println("CREATE <dm> # Criar domínio - utilizador é Owner");
        System.out.println("ADD <user1> <dm> <dm-pwd> # Adicionar utilizador <user1> ao domínio <dm>");
        System.out.println("RD <dm> # Registar o Dispositivo atual no domínio <dm>");
        System.out.println("ET <float> # Enviar valor <float> de Temperatura para o servidor.");
        System.out.println("EI <filename.jpg> # Enviar Imagem <filename.jpg> para o servidor.");
        System.out.println("RT <dm> # Receber as últimas medições de Temperatura de cada dispositivo"
                + " do domínio <dm>, desde que o utilizador tenha permissões.");
        System.out.println("RI <user-id>:<dev_id> # Receber o ficheiro Imagem do dispositivo "
                + "<user-id>:<dev_id> do servidor, desde que o utilizador tenha permissões.");
        System.out.println("MYDOMAINS # Imprime a lista de domínios a que o dispositivo pertence.");
        System.out.println("HELP # Mostrar este menu.");

    }

    /**
     * Takes a server adress String and connects the client to the specified server.
     * 
     * @param serverAddress - String {@code <serverAddress>} that identifies the
     *                      server. Format: {@code<IP/hostname>[:Port]}
     */
    private static Boolean connect(String serverAddress) {
        // Check if port was given
        String[] address_port = serverAddress.split(":");
        String addr = new String(address_port[0]);
        int port = address_port.length > 1 ? Integer.parseInt(address_port[1]) : DEFAULT_PORT;

        // Try server connection
        System.out.println("Connecting to server.");
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket clientSocket = (SSLSocket) factory.createSocket(addr, port);
            // Socket clientSocket = new Socket(addr, port);
            System.out.println("Connection successful - " + addr + ":" + port);
            in = new ObjectInputStream(clientSocket.getInputStream()); // the line that prompts the closed socket
                                                                       // exceptionsocket
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            return true;
        } catch (UnknownHostException e) {
            System.out.println("Server not found: " + e.getMessage());

        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            // System.exit(-1);
        }
        return false;
    }

}
