#include "pair_setup.h"
#include <stdlib.h>
#include <memory.h>
#include "pairing_helper.h"
#include "tlv.h"

//input: accessory's generated salt and PIN
//output: accessory's salt and a generated SRP public key
HttpReply PairSetup::step1()
{
    //<M2>  SRP start response

//        SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator(config);
//        verifierGenerator.setXRoutine(new XRoutineWithUserIdentity());
//        v = verifierGenerator.generateVerifier(this.s, "Pair-Setup", pin);
//        k = SRP6Routines.computeK(digest, config.N, config.g);
//        digest.reset();
//
//        b = generatePrivateValue(config.N, random);
//        digest.reset();
//
//      //Generate an SRP public key
//        B = SRP6Routines.computePublicServerValue(config.N, config.g, k, v, b);

    static uint8_t _B [384];
    for (size_t i = 0; i < sizeof(_B); ++i)
    {
        _B[i] = 0x38;
    }

    buf B;
    B.length = sizeof(_B);
    B.data = _B;

    static uint8_t _s [384];
    for (size_t i = 0; i < sizeof(_s); ++i)
    {
        _s[i] = 0x38;
    }

    buf s;
    s.length = sizeof(_s);
    s.data = _s;


    //byte[] B_bytes = bigIntegerToUnsignedByteArray(B);
    //byte[] s_bytes = bigIntegerToUnsignedByteArray(s);

    printf ("pair1 (M2): acc SRP salt len=%d, acc SRP public key B len: %d\n", s.length, B.length);
    return PairingHelper::pair1_reply(s, B);

}



//input: controller's SRP public key  and SRP proof
//process: verify controller's SRP public key
//         generate session key S  (SRP shared secret?)
//         verify controller's SRP proof
//output:  accessory's SRP proof
HttpReply PairSetup::step2(const buf& ios_SRP_public_key, const buf& ios_SRP_proof)
{

    printf ("starting pair2 (M4), ios pk len=%d, proof len=%d\n", ios_SRP_public_key.length, ios_SRP_proof.length);

    //<M4>  SRP Verify Response

//        BigInteger A = new BigInteger(1, ios_SRP_public_key);
//        BigInteger M1 = new BigInteger(1, ios_SRP_proof);
//
//
//        this.A = A;
//        this.M1 = M1;
//
//        if (!SRP6Routines.isValidPublicValue(config.N, A))
//        {
//            System.out.println("Bad client public value 'A'");
//            return HttpReply.generateAuthError(4);
//        }
//
//        u = SRP6Routines.computeU(digest, config.N, A, B);
//        digest.reset();
//
//        //1. Use the iOS device's SRP public key to compute the SRP shared secret key
//        S = SRP6Routines.computeSessionKey(config.N, v, u, A, b);
//
//        //2. Verify the iOS device's SRP proof
//        SRP6ClientEvidenceContext cli_ctx = new SRP6ClientEvidenceContext(userID, s, A, B, S);
//        BigInteger computedM1 = _clientEvidenceRoutine.computeClientEvidence(config, cli_ctx);
//
//        if (!computedM1.equals(M1))
//        {
//            System.out.println("Pair: Bad client credentials");
//            return HttpReply.generateAuthError(4);
//        }
//
//      //3. Generate the accessory-side SRP proof
//        SRP6ServerEvidenceContext srv_ctx = new SRP6ServerEvidenceContext(A, M1, S);
//        M2 = _serverEvidenceRoutine.computeServerEvidence(config, srv_ctx);

//        byte[] acc_SRP_proof = bigIntegerToUnsignedByteArray(M2);

    static uint8_t _acc_SRP_proof[64];
    for (size_t i = 0; i < sizeof(_acc_SRP_proof); ++i)
    {
        _acc_SRP_proof[i] = 0x64;
    }

    buf acc_SRP_proof;
    acc_SRP_proof.data = _acc_SRP_proof;
    acc_SRP_proof.length = sizeof (_acc_SRP_proof);

    printf ("pair2 (M4) output: acc SRP proof len=%d\n", acc_SRP_proof.length);
    printf("step2 complete\n");

    return PairingHelper::pair2_reply(acc_SRP_proof);

}

//M5 encrypted input: controller's username,  Ed25519 longterm public key (ltpk),  Ed25519 sig (proof)
//process: decrypt,
//         verify controller's proof based on username and ltpk
//         create user (username -> ltpk)
//         generate acessory's proof and ltpk
//M6 return:  encrypted proof and ltpk
HttpReply PairSetup::step3(const buf& message, const buf& authData)
{

    //<M5> Verification

    //        byte[] K = getK();  //SRP shared secret (S)


    //      //2. decrypt message  (parameters not documented!)
    //        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
    //        //the two init parameters salt and info are not documented:
    //        hkdf.init(new HKDFParameters(K, "Pair-Setup-Encrypt-Salt".getBytes(),  "Pair-Setup-Encrypt-Info".getBytes()));
    //        byte[] hkdf_session_key = new byte[32];  //used do decrypt and encrypt
    //        hkdf.generateBytes(hkdf_session_key, 0, 32);  //generate an enc/dec  key
    //
    //       //decrypt
    //        ChachaDecoder chachaDec = new ChachaDecoder(hkdf_session_key, "PS-Msg05".getBytes());
    //        byte[] plaintext;
    //        try
    //        {
    //            plaintext = chachaDec.decodeCiphertext(authData, message);
    //        }
    //        catch (Exception ex)
    //        {
    //            System.out.println("pair M6: chacha decode error");
    //            return HttpReply.generateAuthError(6);
    //        }

    buf plaintext = message;

    TLV_Decoder d;
    d.decode(plaintext);
    buf ios_pairing_ID = d.getData(ID);
    buf ios_Ed25519_ltpk = d.getData(PUBLIC_KEY);
    buf ios_Ed25519_proof = d.getData(SIGNATURE);

    printf ("pair setup M6: ios pairing ID: ");
    for (int i = 0; i < ios_pairing_ID.length; ++i )
    {
        printf ("%c", ios_pairing_ID.data[i]);
    }
    printf ("\nios lptk len=%d, proof len=%d\n", ios_Ed25519_ltpk.length, ios_Ed25519_proof.length);

    //3. Derive iOSDeviceX from the SRP shared secret by using HKDF-SHA-512

    //        hkdf = new HKDFBytesGenerator(new SHA512Digest());
    //        hkdf.init(new HKDFParameters(K, "Pair-Setup-Controller-Sign-Salt".getBytes(),  "Pair-Setup-Controller-Sign-Info".getBytes()));
    //
    //        byte[] iOSDeviceX  = new byte[32];
    //        hkdf.generateBytes(iOSDeviceX , 0, 32);
    //
    //        //4. Construct iOSDeviceInfo by concatenating:
    //        byte[] iOSDeviceInfo = joinBytes(iOSDeviceX, ios_pairing_ID, ios_Ed25519_ltpk);
    //
    //        //5. Use Ed25519 to verify the signature of the constructed iOSDeviceInfo with the iOSDeviceLTPK
    //        if (!new EdsaVerifier(ios_Ed25519_ltpk).verify(iOSDeviceInfo, ios_Ed25519_proof))
    //        {
    //            System.out.println("Pair M6: Invalid signature");
    //            return HttpReply.generateAuthError(6);
    //        }
    //
    //        //6. Persistently save the iOSDevicePairingID and iOSDeviceLTPK as a pairing

    //cfg.createUser(new String(ios_pairing_ID), ios_Ed25519_ltpk);

    printf("paired, set discoverable to false\n");

    //adv.setDiscoverable(false);

    ////////////////////////////////////////////////
    // <M6> generate response

    //1. Generate  Ed25519 long-term public key, AccessoryLTPK, and long-term secret key, AccessoryLTSK  (pre-generated)
    //        EdsaSigner signer = new EdsaSigner(cfg.getAccPrivateKey());
    //        byte[] acc_Ed25519_ltpk = signer.getPublicKey();
    uint8_t _acc_Ed25519_ltpk[32];
    for (size_t i = 0; i < sizeof(_acc_Ed25519_ltpk); ++i)
    {
        _acc_Ed25519_ltpk[i] = 0x32;
    }

    buf acc_Ed25519_ltpk;
    acc_Ed25519_ltpk.data = _acc_Ed25519_ltpk;
    acc_Ed25519_ltpk.length = sizeof(_acc_Ed25519_ltpk);

    //2. Derive AccessoryX from the SRP shared secret by using HKDF-SHA-512
    //        hkdf = new HKDFBytesGenerator(new SHA512Digest());
    //        hkdf.init(new HKDFParameters(K, "Pair-Setup-Accessory-Sign-Salt".getBytes(), "Pair-Setup-Accessory-Sign-Info".getBytes()));
    //
    //        byte[] accessoryX = new byte[32];
    //        hkdf.generateBytes(accessoryX, 0, 32);
    //
    //
    ////      //3. Concatenate AccessoryX + AccessoryPairingID + Acc LTPK
    //        byte[] accessoryInfo = joinBytes(accessoryX, cfg.getAccPairingID().getBytes(), acc_Ed25519_ltpk);
    //
    //      //4. Use Ed25519 to generate AccessorySignature by signing AccessoryInfo with its long-term secret  key, AccessoryLTSK
    //        byte[] acc_proof = signer.sign(accessoryInfo);  //signature

    uint8_t _acc_proof[64];
    for (size_t i = 0; i < sizeof(_acc_proof); ++i)
    {
        _acc_proof[i] = 0x64;
    }
    buf acc_proof;
    acc_proof.data = _acc_proof;
    acc_proof.length = sizeof(_acc_proof);
    printf ("pair3: acc_ltpk len=%d, acc_proof len: %d\n", acc_Ed25519_ltpk.length, acc_proof.length);

    //5. Construct the sub-TLV
    const char* acc_pairing_ID = "111-11-111";
    //plaintext = PairingHelper::pair3_encode(acc_pairing_ID, strlen(acc_pairing_ID), acc_Ed25519_ltpk, acc_proof);
    TLV_Encoder encoder;
    encoder.add(ID, (uint8_t*)acc_pairing_ID, strlen(acc_pairing_ID));
    encoder.add(PUBLIC_KEY, acc_Ed25519_ltpk);
    encoder.add(SIGNATURE, acc_proof);
    plaintext = encoder.get();

    //6. Encrypt the sub-TLV
    //        ChachaEncoder chachaEnc = new ChachaEncoder(hkdf_session_key, "PS-Msg06".getBytes());
    //        byte[] ciphertext = chachaEnc.encodeCiphertext(plaintext);


    static uint8_t _ciphertext [1024];
    memcpy (&_ciphertext, plaintext.data, plaintext.length);
    for (uint8_t i = 0; i < 16; ++i)
    {
        _ciphertext[plaintext.length + i] = i;
    }

    buf ciphertext;
    ciphertext.data = _ciphertext;
    ciphertext.length = plaintext.length + 16;

    //byte[] ciphertext = joinBytes(plaintext, auth);

    printf ("pair3 (M6) complete: encrypted data length: %d\n", ciphertext.length);
    return PairingHelper::pair3_reply(ciphertext);
}
