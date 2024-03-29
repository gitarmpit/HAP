package bouncy;

public class Poly1305KeyGenerator    extends CipherKeyGenerator
{
    private static final byte R_MASK_LOW_2 = (byte)0xFC;
    private static final byte R_MASK_HIGH_4 = (byte)0x0F;

    public void init(KeyGenerationParameters param)
    {
        // Poly1305 keys are always 256 bits
        super.init(new KeyGenerationParameters(param.getRandom(), 256));
    }

    public byte[] generateKey()
    {
        final byte[] key = super.generateKey();
        clamp(key);
        return key;
    }
    public static void clamp(byte[] key)
    {
        /*
         * Key is k[0] ... k[15], r[0] ... r[15] as per poly1305_aes_clamp in ref impl.
         */
        if (key.length != 32)
        {
            throw new IllegalArgumentException("Poly1305 key must be 256 bits.");
        }

        /*
         * r[3], r[7], r[11], r[15] have top four bits clear (i.e., are {0, 1, . . . , 15})
         */
        key[19] &= R_MASK_HIGH_4;
        key[23] &= R_MASK_HIGH_4;
        key[27] &= R_MASK_HIGH_4;
        key[31] &= R_MASK_HIGH_4;

        /*
         * r[4], r[8], r[12] have bottom two bits clear (i.e., are in {0, 4, 8, . . . , 252}).
         */
        key[20] &= R_MASK_LOW_2;
        key[24] &= R_MASK_LOW_2;
        key[28] &= R_MASK_LOW_2;
    }

    public static void checkKey(byte[] key)
    {
        if (key.length != 32)
        {
            throw new IllegalArgumentException("Poly1305 key must be 256 bits.");
        }

        checkMask(key[19], R_MASK_HIGH_4);
        checkMask(key[23], R_MASK_HIGH_4);
        checkMask(key[27], R_MASK_HIGH_4);
        checkMask(key[31], R_MASK_HIGH_4);

        checkMask(key[20], R_MASK_LOW_2);
        checkMask(key[24], R_MASK_LOW_2);
        checkMask(key[28], R_MASK_LOW_2);
    }

    private static void checkMask(byte b, byte mask)
    {
        if ((b & (~mask)) != 0)
        {
            throw new IllegalArgumentException("Invalid format for r portion of Poly1305 key.");
        }
    }

}