package iohelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class FileHelper {
    // private ObjectOutputStream out;
    // private ObjectInputStream in;

    // public Iohelper(ObjectOutputStream out, ObjectInputStream in){
    //     this.out = out;
    //     this.in = in;
    // }

    /**
     * Sends a file to the server.
     * 
     * @param path File path
     */
    public static void sendFile(File f,ObjectOutputStream out) {
        long fileSize = f.length();
        try {
            // Send file name
            // out.writeObject(f.getName());
            // Send file size
            out.writeObject(fileSize);

            FileInputStream fin = new FileInputStream(f);
            InputStream input = new BufferedInputStream(fin);
            // Send file
            int bytesSent = 0;
            byte[] buffer = new byte[1024];
            while (fileSize > bytesSent) {
                int bytesRead = input.read(buffer, 0, 1024);
                bytesSent += bytesRead;
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
            input.close();
            fin.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

  /**
     * Receive's a file from the server.
     * 
     * @param fileSize File size.
     * @param path File path
     */
    public static void receiveFile(File f, ObjectInputStream in) {
        try {
            long fileSize = (long) in.readObject(); // Read file size
            f.createNewFile();

            FileOutputStream fout = new FileOutputStream(f);
            OutputStream output = new BufferedOutputStream(fout);

            int bytesWritten = 0;
            byte[] buffer = new byte[1024];

            while (fileSize > bytesWritten) {
                int bytesRead = in.read(buffer, 0, 1024);
                output.write(buffer, 0, bytesRead);
                output.flush();
                fout.flush();
                bytesWritten += bytesRead;
            }
            output.close();
            fout.close();
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     * Given a file, it will just write on in there baby
     */
    public static BufferedWriter createFileWriter(File f) throws IOException{
        f.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        return bw;
    }    

    public static BufferedReader createFileReader(File f) throws IOException{
        f.createNewFile();
        BufferedReader br = new BufferedReader(new FileReader(f));
        return br;
    }
}
