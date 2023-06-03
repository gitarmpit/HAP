package bouncy;

public interface Mac
{
    public void init(CipherParameters params);
    public String getAlgorithmName();
    public int getMacSize();
    public void update(byte in);
    public void update(byte[] in, int inOff, int len);
    public int doFinal(byte[] out, int outOff);
    public void reset();
}
