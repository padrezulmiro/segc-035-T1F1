package iotclient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // Check arguments
        if (args.length < 3) {
            System.out.println(
                    "Error: not enough args!\nUsage: IoTDevice <serverAddress> <dev-id> <user-id>\n");
            System.exit(1);
        }
        String serverAdress = args[0];
        devid = args[1];
        userid = args[2];

        // Ask for pswd
        System.out.println("Password for " + userid + ":");
        password = sc.nextLine();

        // Connection & Authentication
        connectDevice(serverAdress);
        deviceAuth(userid, password);
        sendDeviceID(devid);
        testDevice();
        printMenu();
        // Enquanto o utilizador não pressionar CTRL+C, o programa deverá voltar ao
        // passo 9. Caso contrário, termina.
        while (true) {
            System.out.print("> ");
            String command = sc.nextLine();
            // TODO Execute comand
            executeCommand(command);
        }
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
            case "RD:":
                if (cmd.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: RD <dm>");
                } else {
                    registerDevice(cmd[1]);
                }
                break;
            case "ET:":
                if (cmd.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: ET <float>");
                } else {
                    sendTemperature(cmd[1]);
                }
                break;
            case "EI:":
                if (cmd.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: EI <filename.jpg>");
                } else {
                    sendImage(cmd[1]);
                }
                break;
            case "RT:":
                if (cmd.length != 2) {
                    System.out.println("Error: incorrect args\nUsage: RT <dm>");
                } else {
                    receiveTemp(cmd[1]);
                }
                break;
            case "RI:":
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

    private static void receiveImage(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'receiveImage'");
    }

    private static void receiveTemp(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'receiveTemp'");
    }

    private static void sendImage(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendImage'");
    }

    private static void sendTemperature(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendTemperature'");
    }

    private static void registerDevice(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'registerDevice'");
    }

    private static void addUser(String string, String string2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addUser'");
    }

    private static void createDomain(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createDomain'");
    }

    private static void testDevice() {
        // Send IoTDevice file name
        // Send IoTDevice file size
        // Receive message
        MessageCode code;
        try {
            code = (MessageCode) in.readObject();
            switch (code) {
                case NOK_TESTED:
                    System.out.println(MessageCode.NOK_TESTED.getDesc());
                    // TODO
                    // System exit
                    break;
                case OK_TESTED:
                    System.out.println(MessageCode.OK_TESTED.getDesc());
                    // TODO
                    break;
                default:
                    break;
            }
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
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
    private static void connectDevice(String serverAddress) {
        // Check if port was given
        String[] address_port = serverAddress.split(":");
        String addr = new String(address_port[0]);
        int port = address_port.length > 1 ? Integer.parseInt(address_port[1]) : DEFAULT_PORT;

        // Try server connection
        try {
            System.out.println("Connecting to server.");
            clientSocket = new Socket(addr, port);
            System.out.println("Connection successful - " + addr + ":" + port);
        } catch (IOException e) {
            System.err.println("ERROR" + e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Authenticates device on the server.
     * 
     * @param user
     * @param password
     * @param serverAdress
     */
    private static void deviceAuth(String user, String password) {

        try {
            System.out.println("Starting authentication.");
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            boolean auth = false;

            do {
                MessageCode code = (MessageCode) in.readObject();
                switch (code) {
                    case WRONG_PWD:
                        System.out.println(MessageCode.WRONG_PWD.getDesc());
                        // Wrong password. Try again.
                        // Server waits for another password and resends a message code
                        Scanner sc = new Scanner(System.in);
                        System.out.println("Password:");
                        String newPassword = sc.nextLine();
                        sc.close();
                        out.writeObject(newPassword);
                        break;
                    case OK_NEW_USER:
                        System.out.println(MessageCode.OK_NEW_USER.getDesc());
                        // TODO
                        auth = true;
                        break;
                    case OK_USER:
                        System.out.println(MessageCode.OK_USER.getDesc());
                        // TODO
                        auth = true;
                        break;
                    default:
                        break;
                }
            } while (!auth);

        } catch (IOException e) {
            System.err.println("ERROR" + e.getMessage());
            System.exit(-1);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
        }

    }

    /**
     * 
     * @param deviceID
     */
    private static void sendDeviceID(String deviceID) {
        try {
            System.out.println("Starting authentication.");
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            boolean validID = false;

            do {
                MessageCode code = (MessageCode) in.readObject();
                switch (code) {
                    case NOK_DEVID:
                        System.out.println(MessageCode.NOK_DEVID.getDesc());

                        Scanner sc = new Scanner(System.in);
                        System.out.println("New device ID:");
                        String newID = sc.nextLine();
                        sc.close();
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
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
        }
    }
}