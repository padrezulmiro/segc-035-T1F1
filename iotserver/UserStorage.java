package iotserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UserStorage {
    private Set<String> users;
    private File usersFile;
    private Lock wLock;
    private Lock rLock;

    public UserStorage(String usersFilePath) {
        users = new HashSet<>();
        usersFile = new File(usersFilePath);
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
        wLock = rwLock.writeLock();
        rLock = rwLock.readLock();

        try {
            populateUsersFromFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean registerUser(String user) {
        boolean ret = users.add(user);
        updateUsersFile();
        return ret;
    }

    public boolean isUserRegistered(String user) {
        return users.contains(user);
    }

    private void updateUsersFile() {
        StringBuilder sb = new StringBuilder();
        for (String s: users) {
            sb.append(s);
        }

        try (PrintWriter pw = new PrintWriter(usersFile)) {
            pw.write(sb.toString());
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void populateUsersFromFile() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(usersFile));
        String[] lines = (String[]) reader.lines().toArray(String[]::new);
        reader.close();

        for (String user: lines) registerUser(user);
    }
}
