import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class IoTServer {
    private static final int ARG_NUM = 2;

    public static void main(String[] args) {
        if (args.length >= ARG_NUM) {
            System.err.println("IoTServer runs with at most 1 argument: a port" +
                               "number.");
            System.exit(-1);
        }

        int portArg = 12345;
        if (args.length == ARG_NUM) {
           portArg = Integer.parseInt(args[1]);
        }

        IoTServer server = new IoTServer(portArg);
    }

    private int port;
    private ServerSocket socket;

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
            try (Socket connection = socket.accept()) {
                ServerThread thread = new ServerThread(connection);
                thread.start();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
