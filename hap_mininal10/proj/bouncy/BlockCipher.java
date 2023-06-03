package bouncy;

public interface BlockCipher
{
    public void init(boolean forEncryption, CipherParameters params);
    public String getAlgorithmName();
    public int getBlockSize();
    public int processBlock(byte[] in, int inOff, byte[] out, int outOff);
    public void reset();
}
