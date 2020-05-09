package main;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import bouncy.*;
import crypto.*;

public class PairVerify
{
    private static SecureRandom secureRandom = new SecureRandom();

    private final AccessoryCfg cfg;

    private byte[] sessionKey; 
    private byte[] ios_Curve25519_pk;
    private byte[] acc_Curve25519_pk;
    private byte[] sharedSecret;

    public PairVerify(AccessoryCfg cfg)
    {
        this.cfg = cfg;
    }

    //M2
    public HttpReply stage1(byte[] ios_Curve25519_pk) throws Exception
    {
        System.out.println("Starting pair verification for " + cfg.getName());
        this.ios_Curve25519_pk = ios_Curve25519_pk;
        
        //1. Generate new, random Curve25519 key pair.
        byte[] acc_Curve25519_sk = new byte[32];
        secureRandom.nextBytes(acc_Curve25519_sk);
        acc_Curve25519_pk = new byte[32];
        Curve25519.keygen(acc_Curve25519_pk, null, acc_Curve25519_sk);

        //2. Generate the shared secret, SharedSecret, from the acc's Curve25519 secret key and the iOS device's Curve25519  public key
        sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, acc_Curve25519_sk, ios_Curve25519_pk);

        //3. Construct AccessoryInfo = Accessory's Curve25519 PK + AccessoryPairingID + iOS device's Curve25519 public key
        byte[] accessoryInfo = Util.joinBytes(acc_Curve25519_pk, cfg.getAccPairingID().getBytes(), ios_Curve25519_pk);

        //4. Use Ed25519 to generate AccessorySignature by signing AccessoryInfo with AccessoryLTSK        
        byte[] acc_sign = new EdsaSigner(cfg.getAccPrivateKey()).sign(accessoryInfo);
        
        System.out.printf ("verify1: acc proof length: %d\n", acc_sign.length);

        //5. Construct a sub-TLV
        byte[] plaintext = PairHelper.verify1_encode(cfg.getAccPairingID().getBytes(StandardCharsets.UTF_8), acc_sign);

        
        //6. Derive the symmetric session encryption key, sessionKey, from the Curve25519 shared secret 
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(), "Pair-Verify-Encrypt-Info".getBytes()));
        sessionKey = new byte[32];
        hkdf.generateBytes(sessionKey, 0, 32); 

        //7. Encrypt the sub-TLV, encryptedData, and generate the 16-byte auth tag, authTag
        ChachaEncoder chacha = new ChachaEncoder(sessionKey, "PV-Msg02".getBytes());
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        System.out.printf ("verify1: encrypted data length: %d\n", ciphertext.length);
        
        return PairHelper.verify1_reply(ciphertext, acc_Curve25519_pk);
    }

    //M4
    public HttpReply stage2(byte[] message, byte[] authData) throws Exception
    {
    	//2. Decrypt the sub-TLV in encryptedData

        ChachaDecoder chacha = new ChachaDecoder(sessionKey, "PV-Msg03".getBytes());
        byte[] plaintext = chacha.decodeCiphertext(authData, message);

        TLV_Decoder d = new TLV_Decoder();
        d.decode(plaintext);
        byte[] ios_pairing_ID = d.getData(MessageType.ID.getKey());
        byte[] ios_Ed25519_proof = d.getData(MessageType.SIGNATURE.getKey());

        //3. Look up ios PK by ios pairing ID
        byte[] ios_Ed25519_ltpk = cfg.getIosPublicKey(new String(ios_pairing_ID));
        if (ios_Ed25519_ltpk == null)
        {
            System.out.println("Unknown user: " + new String(ios_pairing_ID));
            //throw new Exception ("unknown user");
            return HttpReply.generateAuthError(4);
        }
        
        byte[] iosDeviceInfo = Util.joinBytes(ios_Curve25519_pk, ios_pairing_ID, acc_Curve25519_pk);

        if (new EdsaVerifier(ios_Ed25519_ltpk).verify(iosDeviceInfo, ios_Ed25519_proof))
        {
        	cfg.storeReadKey(createKey("Control-Write-Encryption-Key"));
        	cfg.storeWriteKey(createKey("Control-Read-Encryption-Key"));
        	
            System.out.println("Completed pair verification for " + cfg.getName());
            return PairHelper.verify2_reply();
        }
        else
        {
            System.out.println("Invalid signature. Could not pair " + cfg.getName());
            return HttpReply.generateAuthError(4);
        }
    }

    private byte[] createKey(String keyType)
    {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(), keyType.getBytes()));
        byte[] key = new byte[32];
        hkdf.generateBytes(key, 0, 32);
        return key;
    }

}
