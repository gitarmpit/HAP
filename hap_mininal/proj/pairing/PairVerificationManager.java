package pairing;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import bouncy.*;


import pairing.PairVerificationRequest.Stage1Request;
import pairing.PairVerificationRequest.Stage2Request;
import pairing.TypeLengthValueUtils.DecodeResult;
import pairing.TypeLengthValueUtils.Encoder;
import impl.*;
import http.*;
import crypto.Curve25519;
import crypto.*;

public class PairVerificationManager {
    
    private static volatile SecureRandom secureRandom;

    private final HomekitAuthInfo authInfo;
    
    private byte[] hkdfKey;
    private byte[] clientPublicKey;
    private byte[] publicKey;
    private byte[] sharedSecret;
    
    private String label;
    
    public PairVerificationManager(HomekitAuthInfo authInfo) {
        this.authInfo = authInfo;
        label = "Home Bridge";
    }

    public HttpResponse handle(HttpRequest rawRequest) throws Exception {
        PairVerificationRequest request = PairVerificationRequest.of(rawRequest.getBody());
        switch(request.getStage()) {
        case ONE:
            return stage1((Stage1Request) request);
            
        case TWO:
            return stage2((Stage2Request) request);
            
        default:
            return new NotFoundResponse();
        }
    }

    private HttpResponse stage1(Stage1Request request) throws Exception {
        System.out.println("Starting pair verification for " + label);
        clientPublicKey = request.getClientPublicKey();
        publicKey = new byte[32];
        //byte[] privateKey = new byte[32];
        byte[] privateKey = { -27, 126, 121, -98, -24, -32, -44, -85, 120, 3, 21, 66, 125, 101, 36, -91, -14, 75, 12, 95, -39, 115, -48, -8, -46, 15, 7, 57, 73, 53, 58, 110 };
        
        //getSecureRandom().nextBytes(privateKey);
        
        /*
        System.out.println ("ver1 key:");
        for (int i = 0; i < privateKey.length; ++i)
        {
            System.out.printf ("%d, ", privateKey[i]);
        }
        System.out.println();
        */
        
        Curve25519.keygen(publicKey, null, privateKey);
        
        sharedSecret = new byte[32];
        Curve25519.curve(sharedSecret, privateKey, clientPublicKey);
        
        byte[] material = ByteUtils.joinBytes(publicKey, authInfo.getMac().getBytes(StandardCharsets.UTF_8),
                clientPublicKey);
        
        byte[] proof = new EdsaSigner(authInfo.getPrivateKey()).sign(material);
        
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Verify-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        hkdfKey = new byte[32];
        hkdf.generateBytes(hkdfKey, 0, 32);
        
        Encoder encoder = TypeLengthValueUtils.getEncoder();
        encoder.add(MessageType.USERNAME, authInfo.getMac().getBytes(StandardCharsets.UTF_8));
        encoder.add(MessageType.SIGNATURE, proof);
        byte[] plaintext = encoder.toByteArray();
        
        ChachaEncoder chacha = new ChachaEncoder(hkdfKey, "PV-Msg02".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chacha.encodeCiphertext(plaintext);
        
        encoder = TypeLengthValueUtils.getEncoder();
        encoder.add(MessageType.STATE, (short) 2);
        encoder.add(MessageType.ENCRYPTED_DATA, ciphertext);
        encoder.add(MessageType.PUBLIC_KEY, publicKey);
        return new PairingResponse(encoder.toByteArray());
    }
    
    private HttpResponse stage2(Stage2Request request) throws Exception {
        ChachaDecoder chacha = new ChachaDecoder(hkdfKey, "PV-Msg03".getBytes(StandardCharsets.UTF_8));

            byte[] message = request.getMessageData();
            byte[] authData = request.getAuthTagData();
        
            System.out.println("message");
            for (int i = 0; i < message.length; ++i) 
            {
                System.out.printf ("%x ", message[i]);
            }
            System.out.println();


            System.out.println("Auth data");
            for (int i = 0; i < authData.length; ++i) 
            {
                System.out.printf ("%x ", authData[i]);
            }
            System.out.println();
        
        
        byte[] plaintext = chacha.decodeCiphertext(request.getAuthTagData(), request.getMessageData());
        
        DecodeResult d = TypeLengthValueUtils.decode(plaintext);
        byte[] clientUsername = d.getBytes(MessageType.USERNAME);
        byte[] clientSignature = d.getBytes(MessageType.SIGNATURE);
        
        byte[] material = ByteUtils.joinBytes(clientPublicKey, clientUsername, publicKey);
        
        byte[] clientLtpk = authInfo.getUserPublicKey(authInfo.getMac()+new String(clientUsername, StandardCharsets.UTF_8));
        if (clientLtpk == null) {
            throw new Exception("Unknown user: "+new String(clientUsername, StandardCharsets.UTF_8));
        }
        
        Encoder encoder = TypeLengthValueUtils.getEncoder();
        if (new EdsaVerifier(clientLtpk).verify(material, clientSignature)) {
            encoder.add(MessageType.STATE, (short) 4);
            System.out.println("Completed pair verification for " + label);
            return new UpgradeResponse(encoder.toByteArray(), createKey("Control-Write-Encryption-Key"),
                    createKey("Control-Read-Encryption-Key"));
        } else {
            encoder.add(MessageType.ERROR, (short) 4);
            System.out.println("Invalid signature. Could not pair " + label);
            return new OkResponse(encoder.toByteArray());
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
            synchronized(PairVerificationManager.class) {
                if (secureRandom == null) {
                    secureRandom = new SecureRandom();
                }
            }
        }
        return secureRandom;
    }

}
