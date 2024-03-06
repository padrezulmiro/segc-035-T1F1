package iotsystem;

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
        // TODO
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
}