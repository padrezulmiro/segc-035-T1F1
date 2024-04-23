package iohelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Utils {

    static public String[] split(String str, char sep) {
        int occurrences = 1;
        ArrayList<String> blocks = new ArrayList<>();

        int i = 0;
        int j = str.indexOf(sep) != -1 ? str.indexOf(sep) : str.length();
        blocks.add(str.substring(i, j).trim());

        while (j != str.length()) {
            i = j + 1;
            j = str.indexOf(sep, i) != -1 ? str.indexOf(sep, i) : str.length();
            blocks.add(str.substring(i, j).trim());
            occurrences++;
        }

        return blocks.toArray(new String[occurrences]);
    }

    public static String fullID(String userId, String devId){
        return (userId + ":" + devId);
    }

    public static String userIDFromFullID(String fullDevID) {
        return fullDevID.split(":")[0];
    }

    public static String devIDFromFullID(String fullDevID) {
        return fullDevID.split(":")[1];
    }

    public static File initializeFile(String filename) throws IOException {
        File fileCreated = new File(filename);
        if (!fileCreated.exists()) {
            fileCreated.createNewFile();
            System.out.println("File created: " + fileCreated.getName());
        }
        return fileCreated;
    }

    public static String certPathFromUser(String user) {
        return "output/server/scerts/" + user + ".cert";
    }
}
