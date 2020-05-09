package main;

import java.io.IOException;

import crypto.ChachaDecoder;
import crypto.ChachaEncoder;
import bouncy.Pack;

public class SessionCrypto
{
	private static final int MAX_ENCRYPTED_LENGTH = 1024;
	int inboundBinaryMessageCount = 0;
	int outboundBinaryMessageCount = 0;
	private byte[] write_key;
	private byte[] read_key;

	public SessionCrypto(byte[] write_key, byte[] read_key)
	{
		this.write_key = write_key;
		this.read_key = read_key;
	}

	private byte[] encryptChunk(byte[] from, int len, byte[] to, int offset) throws Exception
	{
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

		byte[] nonce = Pack.longToLittleEndian(outboundBinaryMessageCount++);
		byte[] ciphertext = new ChachaEncoder(write_key, nonce).encodeCiphertext(chunk, additionalData);

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
	
	private byte[] decryptChunk(byte[] from, int len, byte[] to, int offset) 
	{
		byte[] mac = new byte[16];
		byte[] cipherchunk = new byte[len];
		System.arraycopy(from, offset, cipherchunk, 0, len);
		System.arraycopy(from, offset + len, mac, 0, 16);
		byte[] additionalData = new byte[2];
		additionalData[0] = (byte) (len & 0xff);
		additionalData[1] = (byte) (len >> 8);

		byte[] plaintextChunk;
		try
		{
			byte[] nonce = Pack.longToLittleEndian(inboundBinaryMessageCount++);
			plaintextChunk = new ChachaDecoder(read_key, nonce).decodeCiphertext(mac, additionalData, cipherchunk);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		// ///////////////
		byte[] merge = new byte[to.length + plaintextChunk.length];

		int i;
		for (i = 0; i < to.length; ++i)
		{
			merge[i] = to[i];
		}

		for (i = 0; i < plaintextChunk.length; ++i)
		{
			merge[i + to.length] = plaintextChunk[i];
		}

		return merge;		
		
	}
	
	public byte[] decrypt(byte[] ciphertext)
	{
		
		int offset = 0;
		byte[] result = new byte[0];
		while (offset < ciphertext.length)
		{
	        int len = ((ciphertext[offset + 1] & 0xff) << 8) + (ciphertext[offset + 0] & 0xff);
			System.out.println ("session decrypt: processing chunk len = " + len);
			result = decryptChunk(ciphertext, len, result, offset + 2);
			offset += len + 2 + 16;
		}

		return result;
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
			System.out.println ("session encrypt: processing chunk len = " + len);
			result = encryptChunk(plaintext, len, result, offset);
			offset += len;
		}

		return result;
	}

}
