package bouncy;

public interface Memoable
{
    public Memoable copy();
    public void reset(Memoable other);
}
