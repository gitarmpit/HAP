package bouncy;

public interface DerivationFunction
{
    public void init(DerivationParameters param);
    public int generateBytes(byte[] out, int outOff, int len);
}
