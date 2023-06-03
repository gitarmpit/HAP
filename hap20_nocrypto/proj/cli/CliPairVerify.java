package cli;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

//import bouncy.*;
//import crypto.*;
import main.*;

public class CliPairVerify
{
    private static SecureRandom secureRandom = new SecureRandom();

    
    private byte[] ios_pairing_ID; 
    private byte[] ios_Ed25519_ltsk;
    private byte[] acc_pairing_ID; 
    private byte[] acc_Ed25519_ltpk;  

    private byte[] ios_Curve25519_pk;
    private byte[] ios_Curve25519_sk;

    //read/write keys will be set
    private byte[] write_key;
    private byte[] read_key;
    
    public byte[] getWriteKey() 
    {
    	return write_key;
    }
    
    public byte[] getReadKey() 
    {
    	return read_key;
    }

    public CliPairVerify(byte[] ios_pairing_ID, byte[] ios_Ed25519_ltsk, byte[] acc_pairing_ID, byte[] acc_Ed25519_ltpk)
    {
    	this.ios_pairing_ID = ios_pairing_ID;
    	this.ios_Ed25519_ltsk = ios_Ed25519_ltsk;
    	this.acc_pairing_ID = acc_pairing_ID;
    	this.acc_Ed25519_ltpk = acc_Ed25519_ltpk;
    }

    //M1
    public byte[] stage_M1() throws Exception
    {
        //1. Generate new, random Curve25519 key pair.
        ios_Curve25519_sk = new byte[32];
        secureRandom.nextBytes(ios_Curve25519_sk);
        ios_Curve25519_pk = new byte[32];
        for (int i = 0; i < 32; ++i)
        {
            ios_Curve25519_pk[i] = 0x33;
        }
//        Curve25519.keygen(ios_Curve25519_pk, null, ios_Curve25519_sk);
        TLV_Encoder encoder = new TLV_Encoder();
        encoder.add(MessageType.STATE.getKey(), 1);
        encoder.add(MessageType.PUBLIC_KEY.getKey(), ios_Curve25519_pk);
        return encoder.toByteArray();
    }
    
    //M3
    public byte[] stage_M3(byte[] message, byte[] authData, byte[] acc_Curve25519_pk) throws Exception
    {
//    	byte[] sharedSecret = new byte[32];
//        Curve25519.curve(sharedSecret, ios_Curve25519_sk, acc_Curve25519_pk);
//        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
//        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(), "Pair-Verify-Encrypt-Info".getBytes()));
//        byte[] sessionKey = new byte[32];
//        hkdf.generateBytes(sessionKey, 0, 32); 
//
//        //decrypt
//        ChachaDecoder chachaDec = new ChachaDecoder(sessionKey, "PV-Msg02".getBytes());
//        byte[] plaintext;
//        try
//        {
//            plaintext = chachaDec.decodeCiphertext(authData, message);
//        }
//        catch (Exception ex)
//        {
//            throw new Exception("pair M6: chacha decode error");
//        }
        
        byte[] plaintext = message;
        TLV_Decoder d = new TLV_Decoder();
        d.decode(plaintext);
        byte[] received_acc_pairing_ID = d.getData(MessageType.ID.getKey());
        byte[] acc_Ed25519_proof = d.getData(MessageType.SIGNATURE.getKey());
        
        if (!(new String(acc_pairing_ID)).equals(new String(received_acc_pairing_ID)) )
        {
        	throw new Exception ("unexpected accessory pairing ID: " + new String(received_acc_pairing_ID));
        }
        
        byte[] accessoryInfo = PairSetup.joinBytes(acc_Curve25519_pk, acc_pairing_ID, ios_Curve25519_pk);
        
//        if (new EdsaVerifier(acc_Ed25519_ltpk).verify(accessoryInfo,  acc_Ed25519_proof))
        {
//        	write_key = createKey("Control-Write-Encryption-Key", sharedSecret);
//        	read_key = createKey("Control-Read-Encryption-Key", sharedSecret);
            System.out.println("Completed pair verification");
        }
//        else
//        {
//            throw new Exception("Verify: invalid signature");
//        }

//        byte[] iosDeviceInfo = PairSetup.joinBytes(ios_Curve25519_pk, ios_pairing_ID, acc_Curve25519_pk);
//
//        byte[] ios_sign = new EdsaSigner(ios_Ed25519_ltsk).sign(iosDeviceInfo);
//        
        byte[] ios_sign = new byte[64];
        for (int i = 0; i < ios_sign.length; ++i)
        {
            ios_sign[i] = 0x64;
        }

        TLV_Encoder e = new TLV_Encoder();
        e.add(MessageType.ID.getKey(), ios_pairing_ID);
        e.add(MessageType.SIGNATURE.getKey(), ios_sign);
//        
//        ChachaEncoder chacha = new ChachaEncoder(sessionKey, "PV-Msg03".getBytes());
//        byte[] ciphertext = chacha.encodeCiphertext(e.toByteArray());

        byte[] auth = new byte[16];
        for (int i = 0; i < 16; ++i)
        {
            auth[i] = (byte)i;
        }
        byte[] ciphertext = PairSetup.joinBytes(e.toByteArray(), auth); 
       
        
        System.out.printf ("verify M3: encrypted data length: %d\n", ciphertext.length);

        e = new TLV_Encoder();
        e.add(MessageType.STATE.getKey(), (short) 3);
        e.add(MessageType.ENCRYPTED_DATA.getKey(), ciphertext);
        
        return e.toByteArray();
    }
    
//    private byte[] createKey(String keyType, byte[] sharedSecret)
//    {
//        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
//        hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(), keyType.getBytes()));
//        byte[] key = new byte[32];
//        hkdf.generateBytes(key, 0, 32);
//        return key;
//    }

}
