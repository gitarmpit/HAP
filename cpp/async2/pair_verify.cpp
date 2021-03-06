#include "pair_verify.h"
#include "pairing_helper.h"
#include <stdlib.h>
#include <memory.h>
#include "tlv.h"

//M2
HttpReply PairVerify::stage1(const buf& ios_Curve25519_pk)
{
    printf ("M2: starting pair verification\n");
    printf ("ios pk len: %d\n", ios_Curve25519_pk.length);

//        System.out.println("Starting pair verification for " + cfg.getName());
//        this.ios_Curve25519_pk = ios_Curve25519_pk;
//
//        //1. Generate new, random Curve25519 key pair.
//        byte[] acc_Curve25519_sk = new byte[32];
//        secureRandom.nextBytes(acc_Curve25519_sk);

    uint8_t _acc_Curve25519_pk[32];
    for (size_t i = 0; i < sizeof(_acc_Curve25519_pk); ++i)
    {
        _acc_Curve25519_pk[i] = 0x32;
    }
    buf acc_Curve25519_pk;
    acc_Curve25519_pk.data = _acc_Curve25519_pk;
    acc_Curve25519_pk.length = sizeof (_acc_Curve25519_pk);


//        Curve25519.keygen(acc_Curve25519_pk, null, acc_Curve25519_sk);
//
//        //2. Generate the shared secret, SharedSecret, from the acc's Curve25519 secret key and the iOS device's Curve25519  public key
//        sharedSecret = new byte[32];
//        Curve25519.curve(sharedSecret, acc_Curve25519_sk, ios_Curve25519_pk);
//
//        //3. Construct AccessoryInfo = Accessory's Curve25519 PK + AccessoryPairingID + iOS device's Curve25519 public key
//        byte[] accessoryInfo = PairSetup.joinBytes(acc_Curve25519_pk, cfg.getAccPairingID().getBytes(), ios_Curve25519_pk);
//
//        //4. Use Ed25519 to generate AccessorySignature by signing AccessoryInfo with AccessoryLTSK
//        byte[] acc_sign = new EdsaSigner(cfg.getAccPrivateKey()).sign(accessoryInfo);

    uint8_t _acc_sign[64];
    for (size_t i = 0; i < sizeof(_acc_sign); ++i)
    {
        _acc_sign[i] = 0x64;
    }

    buf acc_sign;
    acc_sign.data = _acc_sign;
    acc_sign.length = sizeof (_acc_sign);

    printf("verify1: acc proof length: %d\n", acc_sign.length);

    //5. Construct a sub-TLV
    const char* acc_pairing_ID = "111-11-111";
    //buf plaintext = PairingHelper::verify1_encode(acc_pairing_ID, strlen(acc_pairing_ID), acc_sign);
    TLV_Encoder encoder;
    encoder.add(ID, (uint8_t*)acc_pairing_ID, strlen(acc_pairing_ID));
    encoder.add(SIGNATURE, acc_sign);
    buf plaintext = encoder.get();

    //6. Derive the symmetric session encryption key, sessionKey, from the Curve25519 shared secret
//        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
//        hkdf.init(new HKDFParameters(sharedSecret, "Pair-Verify-Encrypt-Salt".getBytes(), "Pair-Verify-Encrypt-Info".getBytes()));
//        sessionKey = new byte[32];
//        hkdf.generateBytes(sessionKey, 0, 32);
//
//        //7. Encrypt the sub-TLV, encryptedData, and generate the 16-byte auth tag, authTag
//        ChachaEncoder chacha = new ChachaEncoder(sessionKey, "PV-Msg02".getBytes());
//        byte[] ciphertext = chacha.encodeCiphertext(plaintext);

    static uint8_t _ciphertext [1024];
    memcpy (&_ciphertext, plaintext.data, plaintext.length);

    for (uint8_t i = 0; i < 16; ++i)
    {
        _ciphertext[plaintext.length + i] = i;
    }

    buf ciphertext;
    ciphertext.data = _ciphertext;
    ciphertext.length = plaintext.length + 16;

    printf("verify1: encrypted data length: %d\n", ciphertext.length);

    return PairingHelper::verify1_reply(ciphertext, acc_Curve25519_pk);
}

//M4
HttpReply PairVerify::stage2(const buf& message, const buf& authData)
{
    //2. Decrypt the sub-TLV in encryptedData

    //        ChachaDecoder chacha = new ChachaDecoder(sessionKey, "PV-Msg03".getBytes());
    //        byte[] plaintext = chacha.decodeCiphertext(authData, message);

    buf plaintext = message;

    TLV_Decoder d;
    d.decode(plaintext);
    buf ios_pairing_ID;
    buf ios_Ed25519_proof;
    if (!d.getData(ID, ios_pairing_ID) || !d.getData(SIGNATURE, ios_Ed25519_proof)) 
    {
        printf ("M4: no pairing ID or proof in the ios request\n");
        return HttpReply::generate400();
    }


    printf ("pair verify M4: ios pairing ID: ");
    for (int i = 0; i < ios_pairing_ID.length; ++i )
    {
        printf ("%c", ios_pairing_ID.data[i]);
    }
    printf ("\nios proof len=%d\n", ios_Ed25519_proof.length);


    //        //3. Look up ios PK by ios pairing ID
    //        byte[] ios_Ed25519_ltpk = cfg.getIosPublicKey(new String(ios_pairing_ID));
    //        if (ios_Ed25519_ltpk == null)
    //        {
    //            System.out.println("Unknown user: " + new String(ios_pairing_ID));
    //            //throw new Exception ("unknown user");
    //            return HttpReply.generateAuthError(4);
    //        }
    //
    //        byte[] iosDeviceInfo = PairSetup.joinBytes(ios_Curve25519_pk, ios_pairing_ID, acc_Curve25519_pk);
    //
    //        if (new EdsaVerifier(ios_Ed25519_ltpk).verify(iosDeviceInfo, ios_Ed25519_proof))
    {
        //          cfg.storeReadKey(createKey("Control-Write-Encryption-Key"));
        //          cfg.storeWriteKey(createKey("Control-Read-Encryption-Key"));

        printf("Completed pair verification\n");
        return PairingHelper::verify2_reply();
    }
    //        else
    //        {
    //            System.out.println("Invalid signature. Could not pair " + cfg.getName());
    //            return HttpReply.generateAuthError(4);
    //        }
}

//    private byte[] createKey(String keyType)
//    {
//        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
//        hkdf.init(new HKDFParameters(sharedSecret, "Control-Salt".getBytes(), keyType.getBytes()));
//        byte[] key = new byte[32];
//        hkdf.generateBytes(key, 0, 32);
//        return key;
//    }

