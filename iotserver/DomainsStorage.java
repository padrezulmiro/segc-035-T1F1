package iotserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class DomainsStorage {
    private Map<String, Domain> domains;
    private File domainsFile;
    private Lock wLock;
    private Lock rLock;

    public DomainsStorage() {
        throw new UnsupportedOperationException();
    }

    public void addDomain() {
        throw new UnsupportedOperationException();
    }

    public void addUserToDomain() {
        throw new UnsupportedOperationException();
    }

    public void addDeviceToDomain() {
        throw new UnsupportedOperationException();
    }

    public Map<String, Float> temperatures() {
        throw new UnsupportedOperationException();
    }

    public boolean domainExists() {
        throw new UnsupportedOperationException();
    }

    public boolean isOwnerOfDomain() {
        throw new UnsupportedOperationException();
    }

    public void readLock() {
        rLock.lock();
    }

    public void readUnlock() {
        rLock.unlock();
    }

    public void writerLock() {
        wLock.lock();
    }

    public void writerUnlock() {
        wLock.unlock();
    }

    private Domain getDomain() {
        throw new UnsupportedOperationException();
    }

    private void updateDomainsFile(){
        StringBuilder sb = new StringBuilder();
        for (Domain domain : domains.values()) {
            sb.append(domain.toString());
        }

        try (PrintWriter pw = new PrintWriter(domainsFile)) {
            pw.write(sb.toString());
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
