package main;

import java.io.IOException;

import bouncy.Pack;
import crypto.ChachaDecoder;
import crypto.ChachaEncoder;

public class SessionCrypto
{
    private static final int MAX_ENCRYPTED_LENGTH = 1024;
    int inboundBinaryMessageCount = 0;
    int outboundBinaryMessageCount = 0;
    private AccessoryCfg cfg;
    
    public SessionCrypto(AccessoryCfg cfg)
    {
        this.cfg = cfg;
    }
    
    private byte[] addChunk(byte[] from, int len, byte[] to, int offset) throws Exception
    {
        byte[] nonce = Pack.longToLittleEndian(outboundBinaryMessageCount++);

        byte[] additionalData = new byte[2];

        byte[] chunk;
        if (offset == 0 && len < MAX_ENCRYPTED_LENGTH)
        {
            chunk = from;
        }
        else
        {
            chunk = new byte[len];
            for (int i = 0; i < len; ++i)
            {
                chunk[i] = from[i + offset];
            }
        }

        additionalData[0] = (byte) (chunk.length & 0xff);
        additionalData[1] = (byte) (chunk.length >> 8);

        byte[] ciphertext = new ChachaEncoder(cfg.getWriteKey(), nonce).encodeCiphertext(chunk, additionalData);

        // ///////////////
        byte[] merge = new byte[to.length + ciphertext.length + 2];

        int i;
        for (i = 0; i < to.length; ++i)
        {
            merge[i] = to[i];
        }

        merge[i] = additionalData[0];
        merge[i + 1] = additionalData[1];

        for (i = 0; i < ciphertext.length; ++i)
        {
            merge[i + 2 + to.length] = ciphertext[i];
        }
        
        return merge;
    }
    
    public byte[] decrypt(byte[] msg)
    {
        byte[] mac = new byte[16];
        byte[] ciphertext = new byte[msg.length - 16];
        System.arraycopy(msg, 0, ciphertext, 0, msg.length - 16);
        System.arraycopy(msg, msg.length - 16, mac, 0, 16);
        int msglen = msg.length - 16;
        byte[] additionalData = new byte[2];
        additionalData[0] = (byte) (msglen & 0xff);
        additionalData[1] = (byte) (msglen >> 8);

        try
        {
            byte[] nonce = Pack.longToLittleEndian(inboundBinaryMessageCount++);
            return new ChachaDecoder(cfg.getReadKey(), nonce).decodeCiphertext(mac, additionalData, ciphertext);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public byte[] encrypt(byte[] plaintext) throws Exception
    {
        int offset = 0;
        byte[] result = new byte[0];
        while (offset < plaintext.length)
        {
            int len = plaintext.length - offset;
            if (len > MAX_ENCRYPTED_LENGTH)
            {
                len = MAX_ENCRYPTED_LENGTH;
            }
            result = addChunk(plaintext, len, result, offset);
            offset += len;
        }

        return result;
    }
   
 
}
