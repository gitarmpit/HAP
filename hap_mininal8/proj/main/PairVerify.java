package main;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import bouncy.*;
import crypto.*;

public class PairVerify {
    
    private static volatile SecureRandom secureRandom;

    private final AuthInfo authInfo;
    
    private byte[] hkdfKey;
    private byte[] clientPublicKey;
    private byte[] publicKey;
    private byte[] sharedSecret;
    
    private String label;
    
    public PairVerify(AuthInfo authInfo) {
        this.authInfo = authInfo;
        label = "Home Bridge";
    }


    

    public byte[] stage1(byte[] pk) throws Exception
    {
        System.out.println("Starting pair verification for " + label);
        clientPublicKey = pk; 
        publicKey = new byte[32];
        byte[] privateKey = new byte[32];
        getSecureRandom().nextBytes(privateKey);
        Curve25519.keygen(publicKey, null, privateKey);

        sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, privateKey, clientPublicKey);

        byte[] material = PairSetup.joinBytes(publicKey, authInfo.getMac().getBytes(StandardCharsets.UTF_8),
                clientPublicKey);

        byte[] proof = new EdsaSigner(authInfo.getPrivateKey()).sign(material);

        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        hkdfKey = new byte[32];
        hkdf.generateBytes(hkdfKey, 0, 32);

        Encoder encoder = new Encoder();
        encoder.add(MessageType.USERNAME.getKey(), authInfo.getMac().getBytes(StandardCharsets.UTF_8));
        encoder.add(MessageType.SIGNATURE.getKey(), proof);
        byte[] plaintext = encoder.toByteArray();

        ChachaEncoder chacha = new ChachaEncoder(hkdfKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

        encoder = new Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 2);
        encoder.add(MessageType.ENCRYPTED_DATA.getKey(), ciphertext);
        encoder.add(MessageType.PUBLIC_KEY.getKey(), publicKey);
        return encoder.toByteArray();
    }
   
    public byte[] stage2(byte[] t5) throws Exception {
        
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
        
        byte[] clientLtpk = authInfo.getUserPublicKey(authInfo.getMac()+new String(clientUsername, StandardCharsets.UTF_8));
        if (clientLtpk == null) {
            throw new Exception("Unknown user: "+new String(clientUsername, StandardCharsets.UTF_8));
        }
        
        Encoder encoder = new Encoder();
        if (new EdsaVerifier(clientLtpk).verify(material, clientSignature)) {
            encoder.add(MessageType.STATE.getKey(), (short) 4);
            System.out.println("Completed pair verification for " + label);
            
            authInfo.storeReadKey(createKey("Control-Write-Encryption-Key"));
            authInfo.storeWriteKey(createKey("Control-Read-Encryption-Key"));
            
            return encoder.toByteArray();
            //            return new UpgradeResponse(encoder.toByteArray(), createKey("Control-Write-Encryption-Key"),
//                    createKey("Control-Read-Encryption-Key"));
        } else {
            encoder.add(MessageType.ERROR.getKey(), (short) 4);
            throw new Exception("Invalid signature. Could not pair " + label);
        }
    }
    
    private byte[] createKey(String info) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(StandardCharsets.UTF_8),
                info.getBytes(StandardCharsets.UTF_8)));
        byte[] key = new byte[32];
        hkdf.generateBytes(key, 0, 32);
        return key;
    }
    
    private static SecureRandom getSecureRandom() {
        if (secureRandom == null) {
            synchronized(PairVerify.class) {
                if (secureRandom == null) {
                    secureRandom = new SecureRandom();
                }
            }
        }
        return secureRandom;
    }

}
