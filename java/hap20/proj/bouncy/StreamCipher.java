package bouncy;

public interface StreamCipher
{
    public void init(boolean forEncryption, CipherParameters params);
    public String getAlgorithmName();
    public byte returnByte(byte in);
    public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff);
    public void reset();
}
