package main;

import java.math.BigInteger;
import java.util.Arrays;

public class TLV_Encoder
{

    private byte[] out;
    final int max = 255;

    public TLV_Encoder()
    {
        out = new byte[0];
    }

    public void add(int type, int val)
    {
        byte[] array = new byte[1];
        array[0] = (byte)val;
        add(type, array);
    }

    public void add(int type, BigInteger bi)
    {
        byte[] array = bi.toByteArray();
        if (array[0] == 0)
        {
            array = Arrays.copyOfRange(array, 1, array.length);
        }
        add(type, array);
    }
    
    public void add(int type, byte[] from)
    {
        int offset = 0;
        while (offset < from.length)
        {
            int len = from.length - offset;
            if (len > max )
            {
                len = max;
            }
            if (len == 1)
            {
                System.out.printf ("encoder: added type=%d, len=%d val=%d\n", type, len, from[0]);
            }
            else 
            {
                System.out.printf ("encoder: added type=%d, len=%d\n", type, len);
            }
            addChunk (type, len, offset, from);
            offset += len;
        }
    }
    
    private void addChunk (int type, int len, int fromPos, byte[] from)
    {
        byte[] merge = new byte[out.length + len + 2];
        
        int i;
        for (i = 0; i < out.length; ++i)
        {
            merge[i] = out[i];
        }
        
        merge[i] = (byte)type;
        merge[i+1] = (byte)len;
        
        for (i = 0; i < len; ++i)
        {
            merge[i + 2 + out.length] = from[fromPos + i];
        }

        out = merge;
        
    }
    
    public byte[] toByteArray() 
    {
        return out;
    }
    
//    public static void main (String args[])
//    {
//        Encoder e = new Encoder();
//        e.add((short)6, (short)1);
//        e.add((short)5, new byte[] { 0xd, 0xe, 0xa, 0xd });
//        e.add((short)4, new byte[] { 0xb, 0xe, 0xe, 0xf, 0xb, 0xe, 0xe, 0xf });
//        e.add((short)10, new byte[] { 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 });
//        byte[] b = e.toByteArray();
//        for (int i = 0; i < b.length; ++i)
//        {
//            System.out.printf("%x ", b[i]);
//        }
//        
//    }

}
