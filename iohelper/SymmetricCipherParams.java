package iohelper;

import java.util.Arrays;
import java.util.Base64;

public class SymmetricCipherParams{
    private byte[] salt;
    private int iteration;

    public SymmetricCipherParams(byte[] salt, int iteration) {
        this.salt = salt;
        this.iteration = iteration;
    }

    // DEFAULT is 20 iteration rounds
    public SymmetricCipherParams(byte[] salt) {
        this.salt = salt;
        this.iteration = 20;
    }

    public byte[] getSalt() {
        return salt;
    }

    public int getIterations() {
        return iteration;
    }

    @Override
    public String toString() {
        return Base64.getEncoder().encodeToString(this.getSalt()) + ":" + iteration;
    }
    
}