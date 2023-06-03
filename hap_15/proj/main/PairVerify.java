package main;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import bouncy.*;
import crypto.*;

public class PairVerify
{
    private static SecureRandom secureRandom = new SecureRandom();

    private final AccessoryCfg cfg;

    private byte[] hkdfKey;
    private byte[] clientPublicKey;
    private byte[] publicKey;
    private byte[] sharedSecret;

    public PairVerify(AccessoryCfg cfg)
    {
        this.cfg = cfg;
    }

    public HttpReply stage1(byte[] pk) throws Exception
    {
        System.out.println("Starting pair verification for " + cfg.getName());
        clientPublicKey = pk;
        publicKey = new byte[32];
        byte[] privateKey = new byte[32];
        secureRandom.nextBytes(privateKey);
        Curve25519.keygen(publicKey, null, privateKey);

        sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, privateKey, clientPublicKey);

        byte[] material = PairSetup
                .joinBytes(publicKey, cfg.getMac().getBytes(StandardCharsets.UTF_8), clientPublicKey);

        byte[] proof = new EdsaSigner(cfg.getPrivateKey()).sign(material);

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        hkdfKey = new byte[32];
        hkdf.generateBytes(hkdfKey, 0, 32);

        System.out.printf ("verify1: proof length: %d\n", proof.length);
        Encoder encoder = new Encoder();
        encoder.add(MessageType.USERNAME.getKey(), cfg.getMac().getBytes(StandardCharsets.UTF_8));
        encoder.add(MessageType.SIGNATURE.getKey(), proof);
        byte[] plaintext = encoder.toByteArray();

        ChachaEncoder chacha = new ChachaEncoder(hkdfKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        
        System.out.printf ("verify1: encrypted data length: %d\n", ciphertext.length);
        encoder = new Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 2);
        encoder.add(MessageType.ENCRYPTED_DATA.getKey(), ciphertext);
        encoder.add(MessageType.PUBLIC_KEY.getKey(), publicKey);
        
        return HttpReply.generateOK("application/pairing+tlv8", encoder.toByteArray());
    }

    public HttpReply stage2(byte[] t5) throws Exception
    {

        byte[] authData = new byte[16];
        byte[] message = new byte[t5.length - 16];
        for (int i = 0; i < 16; ++i)
        {
            authData[i] = t5[message.length + i];
        }
        for (int i = 0; i < message.length; ++i)
        {
            message[i] = t5[i];
        }

        ChachaDecoder chacha = new ChachaDecoder(hkdfKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext = chacha.decodeCiphertext(authData, message);

        Decoder d = new Decoder();
        d.decode(plaintext);
        byte[] clientUsername = d.getData(MessageType.USERNAME.getKey());
        byte[] clientSignature = d.getData(MessageType.SIGNATURE.getKey());

        byte[] material = PairSetup.joinBytes(clientPublicKey, clientUsername, publicKey);


        byte[] clientLtpk = cfg.getUserPublicKey(new String(clientUsername, StandardCharsets.UTF_8));
        if (clientLtpk == null)
        {
            System.out.println("Unknown user: " + new String(clientUsername, StandardCharsets.UTF_8));
            //throw new Exception ("unknown user");
            return HttpReply.generateAuthError(4);
        }
        
        System.out.println();

        if (new EdsaVerifier(clientLtpk).verify(material, clientSignature))
        {
            Encoder encoder = new Encoder();
            encoder.add(MessageType.STATE.getKey(), (short) 4);
            System.out.println("Completed pair verification for " + cfg.getName());
            cfg.storeReadKey(createKey("Control-Write-Encryption-Key"));
            cfg.storeWriteKey(createKey("Control-Read-Encryption-Key"));
            return HttpReply.generateOK("application/pairing+tlv8", encoder.toByteArray());
        }
        else
        {
            System.out.println("Invalid signature. Could not pair " + cfg.getName());
            return HttpReply.generateAuthError(4);
        }
    }

    private byte[] createKey(String info)
    {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(StandardCharsets.UTF_8), info
                .getBytes(StandardCharsets.UTF_8)));
        byte[] key = new byte[32];
        hkdf.generateBytes(key, 0, 32);
        return key;
    }

}
