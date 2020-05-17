package bouncy;


public class HKDFBytesGenerator     implements DerivationFunction
{

    private HMac hMacHash;
    private int hashLen;

    private byte[] info;
    private byte[] currentT;

    private int generatedBytes;

    public HKDFBytesGenerator(Digest hash)
    {
        this.hMacHash = new HMac(hash);
        this.hashLen = hash.getDigestSize();
    }

    public void init(DerivationParameters param)
    {
        if (!(param instanceof HKDFParameters))
        {
            throw new IllegalArgumentException(
                "HKDF parameters required for HKDFBytesGenerator");
        }

        HKDFParameters params = (HKDFParameters)param;
        if (params.skipExtract())
        {
            // use IKM directly as PRK
            hMacHash.init(new KeyParameter(params.getIKM()));
        }
        else
        {
            hMacHash.init(extract(params.getSalt(), params.getIKM()));
        }

        info = params.getInfo();

        generatedBytes = 0;
        currentT = new byte[hashLen];
    }

    private KeyParameter extract(byte[] salt, byte[] ikm)
    {
        hMacHash.init(new KeyParameter(ikm));
        if (salt == null)
        {
            // TODO check if hashLen is indeed same as HMAC size
            hMacHash.init(new KeyParameter(new byte[hashLen]));
        }
        else
        {
            hMacHash.init(new KeyParameter(salt));
        }

        hMacHash.update(ikm, 0, ikm.length);

        byte[] prk = new byte[hashLen];
        hMacHash.doFinal(prk, 0);
        return new KeyParameter(prk);
    }

    private void expandNext()
    {
        int n = generatedBytes / hashLen + 1;
        if (n >= 256)
        {
            throw new RuntimeException(
                "HKDF cannot generate more than 255 blocks of HashLen size");
        }
        // special case for T(0): T(0) is empty, so no update
        if (generatedBytes != 0)
        {
            hMacHash.update(currentT, 0, hashLen);
        }
        hMacHash.update(info, 0, info.length);
        hMacHash.update((byte)n);
        hMacHash.doFinal(currentT, 0);
    }

    public Digest getDigest()
    {
        return hMacHash.getUnderlyingDigest();
    }

    public int generateBytes(byte[] out, int outOff, int len)
    {

        if (generatedBytes + len > 255 * hashLen)
        {
            throw new RuntimeException(
                "HKDF may only be used for 255 * HashLen bytes of output");
        }

        if (generatedBytes % hashLen == 0)
        {
            expandNext();
        }

        // copy what is left in the currentT (1..hash
        int toGenerate = len;
        int posInT = generatedBytes % hashLen;
        int leftInT = hashLen - generatedBytes % hashLen;
        int toCopy = Math.min(leftInT, toGenerate);
        System.arraycopy(currentT, posInT, out, outOff, toCopy);
        generatedBytes += toCopy;
        toGenerate -= toCopy;
        outOff += toCopy;

        while (toGenerate > 0)
        {
            expandNext();
            toCopy = Math.min(hashLen, toGenerate);
            System.arraycopy(currentT, 0, out, outOff, toCopy);
            generatedBytes += toCopy;
            toGenerate -= toCopy;
            outOff += toCopy;
        }

        return len;
    }
}
