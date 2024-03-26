package iotclient;

import iohelper.FileHelper;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Um cliente IoT é um programa – chamemos-lhe IoTDevice – que representa um
 * dispositivo de sensorização.
 */
public class IoTDevice {
    private static final int DEFAULT_PORT = 12345;
    static String userid;
    static String devid;
    static String password;
    static Socket clientSocket = null;
    static ObjectInputStream in;
    static ObjectOutputStream out;

    private static Scanner sc;
    public static void main(String[] args) {
        addCliShutdownHook();
        sc = new Scanner(System.in);
        // Check arguments
        if (args.length < 3) {
            System.out.println(
                    "Error: not enough args!\nUsage: IoTDevice <serverAddress> <dev-id> <user-id>\n");
            System.exit(1);
        }
        String serverAddress = args[0];
        devid = args[1];
        userid = args[2];

        // Ask for pswd
        System.out.println("Password for " + userid + ":");
        password = sc.nextLine();

        // Connection & Authentication
        if (connect(serverAddress)) {
            userAuth(userid, password);
            deviceAuth(devid);
            testDevice();
            printMenu();
            // Program doesn't end until CTRL+C is pressed
            while (true) {// Steps 8 - 10
                System.out.print("> ");
                String command = sc.nextLine();
                executeCommand(command);
            }
        }
    }

    private static void addCliShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nCaught Ctrl-C. Shutting down.");
            try {
                out.writeObject(MessageCode.STOP);
                // Close socket 
                if(clientSocket != null){
                    clientSocket.close();
                }
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
     */
    private static void executeCommand(String command) {
        // First word is the command
        String[] cmd = command.split(" ");

        switch (cmd[0]) {
            case "CREATE":
                if (cmd.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: CREATE <dm>");
                } else {
                    createDomain(cmd[1]);
                }
                break;
            case "ADD":
                if (cmd.length != 3) {
                    System.out.println("Error: incorrect args\nUsage: ADD <user1> <dm>");
                } else {
                    addUser(cmd[1], cmd[2]);
                }
                break;
            case "RD":
                if (cmd.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: RD <dm>");
                } else {
                    registerDevice(cmd[1]);
                }
                break;
            case "ET":
                if (cmd.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: ET <float>");
                } else {
                    sendTemperature(cmd[1]);
                }
                break;
            case "EI":
                if (cmd.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: EI <filename.jpg>");
                } else {
                    sendImage(cmd[1]);
                }
                break;
            case "RT":
                if (cmd.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: RT <dm>");
                } else {
                    receiveTemps(cmd[1]);
                }
                break;
            case "RI":
                if (cmd.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: RI <user-id>:<dev_id>");
                } else {
                    receiveImage(cmd[1]);
                }
                break;
            default:
                System.out.println("That command does not exist.");
                break;
        }
    }

    /**
     * 
     * @param device
     */
    private static void receiveImage(String device) {
        try {
            out.writeObject(MessageCode.RI); // Send opcode
            String[] dev = device.split(":");
            out.writeObject(dev[0]);
            out.writeObject(dev[1]);
            // Receive message
            MessageCode code = (MessageCode) in.readObject();
            switch (code) {
                case OK:
                    Long fileSize = in.readLong(); // Read file size
                    // String[] dev = device.split(":");
                    String fileName = "Img_" + dev[0] + "_" + dev[1] + ".jpg";
                    FileHelper.receiveFile(fileSize, fileName,in);
                    System.out.println(MessageCode.OK.getDesc() + ", " + fileSize + " (long)"); // TODO
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
        }
    }

    private static void receiveTemps(String domain) {
        try {
            out.writeObject(MessageCode.RT); // Send opcode
            out.writeObject(domain);
            // Receive message
            MessageCode code = (MessageCode) in.readObject();
            switch (code) {
                case OK:
                    // Long fileSize = (long) in.readObject(); // Read file size
                    @SuppressWarnings("unchecked") HashMap<String,Float> temps = (HashMap<String,Float>) in.readObject();
                    for (@SuppressWarnings("unused") String s : temps.keySet());
                    for (@SuppressWarnings("unused") Number n : temps.values());
                    // reason for the empty loops: https://stackoverflow.com/a/509288
                    // essentially ClassCastException will be thrown if any of the maps is bad

                    // TODO: write it to file
                    String fileName = "temps_" + domain + ".txt";
                    File f = new File(fileName);
                    f.createNewFile();
                    BufferedWriter output = new BufferedWriter(new FileWriter(f));
                    for(Map.Entry<String,Float> entry : temps.entrySet()){
                        output.write(entry.getKey() + ": " + entry.getValue() + System.getProperty ("line.separator"));
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
        }
    }

    // /**
    //  * Receive's a file from the server.
    //  * 
    //  * @param fileSize File size.
    //  * @param path File path
    //  */
    // private static void receiveFile(Long fileSize, String path) {
    //     try {
    //         File f = new File(path);
    //         f.createNewFile();

    //         FileOutputStream fout = new FileOutputStream(f);
    //         OutputStream output = new BufferedOutputStream(fout);

    //         int bytesWritten = 0;
    //         byte[] buffer = new byte[1024];

    //         while (fileSize > bytesWritten) {
    //             int bytesRead = in.read(buffer, 0, 1024);
    //             output.write(buffer, 0, bytesRead);
    //             output.flush();
    //             fout.flush();
    //             bytesWritten += bytesRead;
    //             System.out.println(bytesWritten);
    //         }
    //         output.close();
    //         fout.close();
    //     } catch (IOException e) {
    //         // TODO Auto-generated catch block
    //         e.printStackTrace();
    //     }
    // }

    private static void sendImage(String imagePath) {
        try {
            out.writeObject(MessageCode.EI); // Send opcode
            FileHelper.sendFile((imagePath),out);
            // Receive message
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
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // /**
    //  * Sends a file to the server.
    //  * 
    //  * @param path File path
    //  */
    // private static void sendFile(String path) {
    //     File f = new File("client\\" + path);
    //     long fileSize = f.length();
    //     try {
    //         // Send file name
    //         out.writeObject(f.getName());
    //         // Send file size
    //         out.writeObject(fileSize);

    //         FileInputStream fin = new FileInputStream(f);
    //         InputStream input = new BufferedInputStream(fin);
    //         // Send file
    //         int bytesSent = 0;
    //         byte[] buffer = new byte[1024];
    //         while (fileSize > bytesSent) {
    //             int bytesRead = input.read(buffer, 0, 1024);
    //             bytesSent += bytesRead;
    //             out.write(buffer, 0, bytesRead);
    //             out.flush();
    //         }
    //         input.close();
    //         fin.close();
    //     } catch (IOException e) {
    //         // TODO Auto-generated catch block
    //         e.printStackTrace();
    //     }
    // }

    /**
     * Sends a temperature value to the server.
     * 
     * @param temp
     */
    private static void sendTemperature(String temp) { // Should it test if the value can be converted to a float?
        try {
            out.writeObject(MessageCode.ET); // Send opcode
            out.writeObject(temp); // Send user
            // Receive message
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
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
     */
    private static void addUser(String user, String domain) {
        try {
            out.writeObject(MessageCode.ADD); // Send opcode
            out.writeObject(user); // Send user
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
                case NOUSER:
                    System.out.println(MessageCode.NOUSER.getDesc());
                    break;
                case USEREXISTS:
                    System.out.println(MessageCode.USEREXISTS.getDesc());
                default:
                    break;
            }
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            File iotFile = new File("./bin/iotclient/IoTDevice.class");
            // Send Test Device message
            out.writeObject(MessageCode.TD);
            // Send IoTDevice file name
            out.writeObject(iotFile.getName());
            // Send IoTDevice file size
            out.writeLong(iotFile.length());
            // Long mock_size = (long) 3;
            // out.writeLong(mock_size);
            // Receive message
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
        System.out.println("ADD <user1> <dm> # Adicionar utilizador <user1> ao domínio <dm>");
        System.out.println("RD <dm> # Registar o Dispositivo atual no domínio <dm>");
        System.out.println("ET <float> # Enviar valor <float> de Temperatura para o servidor.");
        System.out.println("EI <filename.jpg> # Enviar Imagem <filename.jpg> para o servidor.");
        System.out.println("RT <dm> # Receber as últimas medições de Temperatura de cada dispositivo"
                + " do domínio <dm>, desde que o utilizador tenha permissões.");
        System.out.println("RI <user-id>:<dev_id> # Receber o ficheiro Imagem do dispositivo "
                + "<user-id>:<dev_id> do servidor, desde que o utilizador tenha permissões.");

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
            Socket clientSocket = new Socket(addr, port);
            System.out.println("Connection successful - " + addr + ":" + port);
            in = new ObjectInputStream(clientSocket.getInputStream()); // the line that prompts the closed socket
                                                                       // exceptionsocket
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            return true;
        } catch (UnknownHostException e) {
            System.out.println("Server not found: " + e.getMessage());

        } catch (IOException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(-1);
        }
        return false;
    }

    /**
     * Authenticates device on the server.
     * 
     * @param user     User ID.
     * @param password User's password.
     */
    private static void userAuth(String user, String password) {

        try {
            System.out.println("Starting authentication.");
            out.writeObject(MessageCode.AU);
            out.writeObject(user);
            out.writeObject(password);
            boolean auth = false;

            // Send given user and password
            // out.writeObject(user);
            // out.writeObject(password);

            do {
                // Receive code from server
                MessageCode code = (MessageCode) in.readObject();
                switch (code) {
                    case WRONG_PWD:
                        System.out.println(MessageCode.WRONG_PWD.getDesc());
                        // Wrong password. Try again.
                        // Server waits for another password and resends a message code
                        // Scanner sc = new Scanner(System.in);
                        System.out.println("Password:");
                        String newPassword = sc.nextLine();
                        // sc.close();
                        out.writeObject(MessageCode.AU);
                        out.writeObject(newPassword);
                        break;
                    case OK_NEW_USER:
                        System.out.println(MessageCode.OK_NEW_USER.getDesc());
                        System.out.println("You've been registered.");
                        auth = true;
                        break;
                    case OK_USER:
                        System.out.println(MessageCode.OK_USER.getDesc());
                        System.out.println("You've been authenticated.");
                        auth = true;
                        break;
                    default:
                        System.out.println("Read incorrect code from server.");
                        break;
                }
            } while (!auth);

        } catch (IOException e) {
            System.err.println("ERROR" + e.getMessage());
            System.exit(-1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Sends device ID for authentication.
     * 
     * @param deviceID
     */
    private static void deviceAuth(String deviceID) {
        try {
            System.out.println("Starting device ID authentication.");
            out.writeObject(MessageCode.AD);
            out.writeObject(deviceID);
            boolean validID = false;

            // out.writeObject(deviceID); //probably only once

            do {
                MessageCode code = (MessageCode) in.readObject();
                switch (code) {
                    case NOK_DEVID:
                        System.out.println(MessageCode.NOK_DEVID.getDesc());

                        // Scanner sc = new Scanner(System.in);
                        System.out.println("New device ID:");
                        String newID = sc.nextLine();
                        // sc.close();
                        out.writeObject(MessageCode.AD);
                        out.writeObject(newID);
                        break;
                    case OK_DEVID:
                        System.out.println(MessageCode.OK_DEVID.getDesc());
                        validID = true;
                    default:
                        break;
                }
            } while (!validID);

        } catch (IOException e) {
            System.err.println("ERROR" + e.getMessage());
            System.exit(-1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
