package main;

import java.math.BigInteger;

import bouncy.Arrays;

public class Util
{
    public static byte[] joinBytes(byte[]... piece)
    {
        int pos = 0;
        int length = 0;
        for (int i = 0; i < piece.length; i++)
        {
            length += piece[i].length;
        }
        byte[] ret = new byte[length];
        for (int i = 0; i < piece.length; i++)
        {
            System.arraycopy(piece[i], 0, ret, pos, piece[i].length);
            pos += piece[i].length;
        }
        return ret;
    }

    public static byte[] bigIntegerToUnsignedByteArray(BigInteger i)
    {
        byte[] array = i.toByteArray();
        if (array[0] == 0)
        {
            array = Arrays.copyOfRange(array, 1, array.length);
        }
        return array;
    }
    
}
