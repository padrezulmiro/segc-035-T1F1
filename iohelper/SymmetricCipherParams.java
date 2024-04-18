package iohelper;
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

    public int getIteration() {
        return iteration;
    }
    
}