package iotserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class IoTServer {
    public static final ServerManager SERVERMANAGER = ServerManager.getInstance();

    private static final int ARG_NUM = 1;

    private int port;
    private ServerSocket socket;

    public static void main(String[] args) {

        int portArg = 12345;
        if (args.length == ARG_NUM) {
           portArg = Integer.parseInt(args[0]);
           System.out.println("IoTServer runs with port: " + portArg);
        } else if (args.length == 0){
            System.out.println("IoTServer runs with default port: 12345");
        } else {
            System.err.println("IoTServer runs with at most 1 argument: a port number.");
            System.exit(-1);
        }
        
        IoTServer server = new IoTServer(portArg);
    }

    public IoTServer(int port) {
        this.port = port;
        this.startServer();
    }

    private void startServer() {
        socket = null;

        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        while (true) {
            try{
                Socket connection = socket.accept();
                ServerThread thread = new ServerThread(connection);
                thread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
