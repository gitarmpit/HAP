package bouncy;

public class Salsa20Engine    implements SkippingStreamCipher
{
    public final static int DEFAULT_ROUNDS = 20;

    private final static int STATE_SIZE = 16; // 16, 32 bit ints = 64 bytes

    protected final static byte[]
        sigma = Strings.toByteArray("expand 32-byte k"),
        tau   = Strings.toByteArray("expand 16-byte k");

    protected int rounds;
    private int         index = 0;
    protected int[]     engineState = new int[STATE_SIZE]; // state
    protected int[]     x = new int[STATE_SIZE] ; // internal buffer
    private byte[]      keyStream   = new byte[STATE_SIZE * 4]; // expanded state, 64 bytes
    private boolean     initialised = false;

    private int cW0, cW1, cW2;

    public Salsa20Engine()
    {
        this(DEFAULT_ROUNDS);
    }

    public Salsa20Engine(int rounds)
    {
        if (rounds <= 0 || (rounds & 1) != 0)
        {
            throw new IllegalArgumentException("'rounds' must be a positive, even number");
        }

        this.rounds = rounds;
    }

    public void init(
        boolean             forEncryption, 
        CipherParameters     params)
    {

        if (!(params instanceof ParametersWithIV))
        {
            throw new IllegalArgumentException(getAlgorithmName() + " Init parameters must include an IV");
        }

        ParametersWithIV ivParams = (ParametersWithIV) params;

        byte[] iv = ivParams.getIV();
        if (iv == null || iv.length != getNonceSize())
        {
            throw new IllegalArgumentException(getAlgorithmName() + " requires exactly " + getNonceSize()
                    + " bytes of IV");
        }

        CipherParameters keyParam = ivParams.getParameters();
        if (keyParam == null)
        {
            if (!initialised)
            {
                throw new IllegalStateException(getAlgorithmName() + " KeyParameter can not be null for first initialisation");
            }

            setKey(null, iv);
        }
        else if (keyParam instanceof KeyParameter)
        {
            setKey(((KeyParameter)keyParam).getKey(), iv);
        }
        else
        {
            throw new IllegalArgumentException(getAlgorithmName() + " Init parameters must contain a KeyParameter (or null for re-init)");
        }

        reset();

        initialised = true;
    }

    protected int getNonceSize()
    {
        return 8;
    }

    public String getAlgorithmName()
    {
        String name = "Salsa20";
        if (rounds != DEFAULT_ROUNDS)
        {
            name += "/" + rounds;
        }
        return name;
    }

    public byte returnByte(byte in)
    {
        if (limitExceeded())
        {
            throw new RuntimeException("2^70 byte limit per IV; Change IV");
        }

        byte out = (byte)(keyStream[index]^in);
        index = (index + 1) & 63;

        if (index == 0)
        {
            advanceCounter();
            generateKeyStream(keyStream);
        }

        return out;
    }

    protected void advanceCounter()
    {
        if (++engineState[8] == 0)
        {
            ++engineState[9];
        }
    }

    protected void retreatCounter()
    {
        if (engineState[8] == 0 && engineState[9] == 0)
        {
            throw new IllegalStateException("attempt to reduce counter past zero.");
        }

        if (--engineState[8] == -1)
        {
            --engineState[9];
        }
    }

    public int processBytes(
        byte[]     in, 
        int     inOff, 
        int     len, 
        byte[]     out, 
        int     outOff)
    {
        if (!initialised)
        {
            throw new IllegalStateException(getAlgorithmName() + " not initialised");
        }

        if ((inOff + len) > in.length)
        {
            throw new RuntimeException("input buffer too short");
        }

        if ((outOff + len) > out.length)
        {
            throw new RuntimeException("output buffer too short");
        }

        if (limitExceeded(len))
        {
            throw new RuntimeException("2^70 byte limit per IV would be exceeded; Change IV");
        }

        for (int i = 0; i < len; i++)
        {
            out[i + outOff] = (byte)(keyStream[index] ^ in[i + inOff]);
            index = (index + 1) & 63;

            if (index == 0)
            {
                advanceCounter();
                generateKeyStream(keyStream);
            }
        }

        return len;
    }

    public long skip(long numberOfBytes)
    {
        if (numberOfBytes >= 0)
        {
            for (long i = 0; i < numberOfBytes; i++)
            {
                index = (index + 1) & 63;

                if (index == 0)
                {
                    advanceCounter();
                }
            }
        }
        else
        {
            for (long i = 0; i > numberOfBytes; i--)
            {
                if (index == 0)
                {
                    retreatCounter();
                }

                index = (index - 1) & 63;
            }
        }

        generateKeyStream(keyStream);

        return numberOfBytes;
    }

    public long seekTo(long position)
    {
        reset();

        return skip(position);
    }

    public long getPosition()
    {
        return getCounter() * 64 + index;
    }

    public void reset()
    {
        index = 0;
        resetLimitCounter();
        resetCounter();

        generateKeyStream(keyStream);
    }

    protected long getCounter()
    {
        return ((long)engineState[9] << 32) | (engineState[8] & 0xffffffffL);
    }

    protected void resetCounter()
    {
        engineState[8] = engineState[9] = 0;
    }

    protected void setKey(byte[] keyBytes, byte[] ivBytes)
    {
        if (keyBytes != null)
        {
            if ((keyBytes.length != 16) && (keyBytes.length != 32))
            {
                throw new IllegalArgumentException(getAlgorithmName() + " requires 128 bit or 256 bit key");
            }

            // Key
            engineState[1] = Pack.littleEndianToInt(keyBytes, 0);
            engineState[2] = Pack.littleEndianToInt(keyBytes, 4);
            engineState[3] = Pack.littleEndianToInt(keyBytes, 8);
            engineState[4] = Pack.littleEndianToInt(keyBytes, 12);

            byte[] constants;
            int offset;
            if (keyBytes.length == 32)
            {
                constants = sigma;
                offset = 16;
            }
            else
            {
                constants = tau;
                offset = 0;
            }

            engineState[11] = Pack.littleEndianToInt(keyBytes, offset);
            engineState[12] = Pack.littleEndianToInt(keyBytes, offset + 4);
            engineState[13] = Pack.littleEndianToInt(keyBytes, offset + 8);
            engineState[14] = Pack.littleEndianToInt(keyBytes, offset + 12);
    
            engineState[0 ] = Pack.littleEndianToInt(constants, 0);
            engineState[5 ] = Pack.littleEndianToInt(constants, 4);
            engineState[10] = Pack.littleEndianToInt(constants, 8);
            engineState[15] = Pack.littleEndianToInt(constants, 12);
        }

        // IV
        engineState[6] = Pack.littleEndianToInt(ivBytes, 0);
        engineState[7] = Pack.littleEndianToInt(ivBytes, 4);
    }

    protected void generateKeyStream(byte[] output)
    {
        salsaCore(rounds, engineState, x);
        Pack.intToLittleEndian(x, output, 0);
    }

    public static void salsaCore(int rounds, int[] input, int[] x)
    {
        if (input.length != 16)
        {
            throw new IllegalArgumentException();
        }
        if (x.length != 16)
        {
            throw new IllegalArgumentException();
        }
        if (rounds % 2 != 0)
        {
            throw new IllegalArgumentException("Number of rounds must be even");
        }

        int x00 = input[ 0];
        int x01 = input[ 1];
        int x02 = input[ 2];
        int x03 = input[ 3];
        int x04 = input[ 4];
        int x05 = input[ 5];
        int x06 = input[ 6];
        int x07 = input[ 7];
        int x08 = input[ 8];
        int x09 = input[ 9];
        int x10 = input[10];
        int x11 = input[11];
        int x12 = input[12];
        int x13 = input[13];
        int x14 = input[14];
        int x15 = input[15];

        for (int i = rounds; i > 0; i -= 2)
        {
            x04 ^= rotl(x00 + x12, 7);
            x08 ^= rotl(x04 + x00, 9);
            x12 ^= rotl(x08 + x04, 13);
            x00 ^= rotl(x12 + x08, 18);
            x09 ^= rotl(x05 + x01, 7);
            x13 ^= rotl(x09 + x05, 9);
            x01 ^= rotl(x13 + x09, 13);
            x05 ^= rotl(x01 + x13, 18);
            x14 ^= rotl(x10 + x06, 7);
            x02 ^= rotl(x14 + x10, 9);
            x06 ^= rotl(x02 + x14, 13);
            x10 ^= rotl(x06 + x02, 18);
            x03 ^= rotl(x15 + x11, 7);
            x07 ^= rotl(x03 + x15, 9);
            x11 ^= rotl(x07 + x03, 13);
            x15 ^= rotl(x11 + x07, 18);

            x01 ^= rotl(x00 + x03, 7);
            x02 ^= rotl(x01 + x00, 9);
            x03 ^= rotl(x02 + x01, 13);
            x00 ^= rotl(x03 + x02, 18);
            x06 ^= rotl(x05 + x04, 7);
            x07 ^= rotl(x06 + x05, 9);
            x04 ^= rotl(x07 + x06, 13);
            x05 ^= rotl(x04 + x07, 18);
            x11 ^= rotl(x10 + x09, 7);
            x08 ^= rotl(x11 + x10, 9);
            x09 ^= rotl(x08 + x11, 13);
            x10 ^= rotl(x09 + x08, 18);
            x12 ^= rotl(x15 + x14, 7);
            x13 ^= rotl(x12 + x15, 9);
            x14 ^= rotl(x13 + x12, 13);
            x15 ^= rotl(x14 + x13, 18);
        }

        x[ 0] = x00 + input[ 0];
        x[ 1] = x01 + input[ 1];
        x[ 2] = x02 + input[ 2];
        x[ 3] = x03 + input[ 3];
        x[ 4] = x04 + input[ 4];
        x[ 5] = x05 + input[ 5];
        x[ 6] = x06 + input[ 6];
        x[ 7] = x07 + input[ 7];
        x[ 8] = x08 + input[ 8];
        x[ 9] = x09 + input[ 9];
        x[10] = x10 + input[10];
        x[11] = x11 + input[11];
        x[12] = x12 + input[12];
        x[13] = x13 + input[13];
        x[14] = x14 + input[14];
        x[15] = x15 + input[15];
    }

    protected static int rotl(int x, int y)
    {
        return (x << y) | (x >>> -y);
    }

    private void resetLimitCounter()
    {
        cW0 = 0;
        cW1 = 0;
        cW2 = 0;
    }

    private boolean limitExceeded()
    {
        if (++cW0 == 0)
        {
            if (++cW1 == 0)
            {
                return (++cW2 & 0x20) != 0;          // 2^(32 + 32 + 6)
            }
        }

        return false;
    }

    private boolean limitExceeded(int len)
    {
        cW0 += len;
        if (cW0 < len && cW0 >= 0)
        {
            if (++cW1 == 0)
            {
                return (++cW2 & 0x20) != 0;          // 2^(32 + 32 + 6)
            }
        }

        return false;
    }
}
