import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        System.out.println("Accepted connection!");

        try {
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMessage(String message) {
        out.writeInt(message.length());
        byte buffer[] = message.getBytes();
        out.write(buffer);
    }

    private String readMessage() {
        int size = in.readInt();
        byte buffer[] = in.readNBytes(size);
        return new String(buffer);
    }

    private UserAuthStatus autheticateUser() {
        // TODO
        String user = readMessage();
        String pass = readMessage();

        return UserAuthStatus.WRONG_PWD;
    }
}

public interface AuthStatus {}

public enum UserAuthStatus implements AuthStatus {
   OK_USER,
   OK_NEW_USER,
   WRONG_PWD;

   @Override
   public String toString() {
       switch (this) {
           case OK_USER:
               return "OK_USER";
           case OK_NEW_USER:
               return "OK_NEW_USER";
           case WRONG_PWD:
               return "WRONG_PWD";
       }
   }
}

public enum DeviceAuthStatus implements AuthStatus {
    OK_DEV_ID,
    NOK_DEV_ID;

    @Override
    public String toString() {
        switch (this) {
            case OK_DEV_ID:
                return "OK_DEV_ID";
            case NOK_DEV_ID:
                return "NOK_DEV_ID";
        }
    }
}
