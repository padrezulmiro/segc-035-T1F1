import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import iotclient.MessageCode;

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

    private void writeMessage(MessageCode message) throws IOException {
        // out.writeInt(message.length());
        // byte buffer[] = message.getBytes();
        // out.write(buffer);
        out.writeObject(message);
    }

    private MessageCode readMessage()throws IOException, ClassNotFoundException {
        // byte buffer[] = in.readNBytes(size);
        MessageCode recievedCode = (MessageCode)in.readObject();
        // return new String(buffer);
        return recievedCode;
    }
    
}
