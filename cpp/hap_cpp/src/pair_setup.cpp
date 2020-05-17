#include "pair_setup.h"
#include <stdlib.h>
#include <memory.h>
#include "pairing_helper.h"
#include "tlv.h"
#include "srp.h"

#define NLEN    384
#define PIN_CODE "111-11-111"

static byte N[]={
  0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xc9, 0x0f, 0xda, 0xa2,
  0x21, 0x68, 0xc2, 0x34, 0xc4, 0xc6, 0x62, 0x8b, 0x80, 0xdc, 0x1c, 0xd1,
  0x29, 0x02, 0x4e, 0x08, 0x8a, 0x67, 0xcc, 0x74, 0x02, 0x0b, 0xbe, 0xa6,
  0x3b, 0x13, 0x9b, 0x22, 0x51, 0x4a, 0x08, 0x79, 0x8e, 0x34, 0x04, 0xdd,
  0xef, 0x95, 0x19, 0xb3, 0xcd, 0x3a, 0x43, 0x1b, 0x30, 0x2b, 0x0a, 0x6d,
  0xf2, 0x5f, 0x14, 0x37, 0x4f, 0xe1, 0x35, 0x6d, 0x6d, 0x51, 0xc2, 0x45,
  0xe4, 0x85, 0xb5, 0x76, 0x62, 0x5e, 0x7e, 0xc6, 0xf4, 0x4c, 0x42, 0xe9,
  0xa6, 0x37, 0xed, 0x6b, 0x0b, 0xff, 0x5c, 0xb6, 0xf4, 0x06, 0xb7, 0xed,
  0xee, 0x38, 0x6b, 0xfb, 0x5a, 0x89, 0x9f, 0xa5, 0xae, 0x9f, 0x24, 0x11,
  0x7c, 0x4b, 0x1f, 0xe6, 0x49, 0x28, 0x66, 0x51, 0xec, 0xe4, 0x5b, 0x3d,
  0xc2, 0x00, 0x7c, 0xb8, 0xa1, 0x63, 0xbf, 0x05, 0x98, 0xda, 0x48, 0x36,
  0x1c, 0x55, 0xd3, 0x9a, 0x69, 0x16, 0x3f, 0xa8, 0xfd, 0x24, 0xcf, 0x5f,
  0x83, 0x65, 0x5d, 0x23, 0xdc, 0xa3, 0xad, 0x96, 0x1c, 0x62, 0xf3, 0x56,
  0x20, 0x85, 0x52, 0xbb, 0x9e, 0xd5, 0x29, 0x07, 0x70, 0x96, 0x96, 0x6d,
  0x67, 0x0c, 0x35, 0x4e, 0x4a, 0xbc, 0x98, 0x04, 0xf1, 0x74, 0x6c, 0x08,
  0xca, 0x18, 0x21, 0x7c, 0x32, 0x90, 0x5e, 0x46, 0x2e, 0x36, 0xce, 0x3b,
  0xe3, 0x9e, 0x77, 0x2c, 0x18, 0x0e, 0x86, 0x03, 0x9b, 0x27, 0x83, 0xa2,
  0xec, 0x07, 0xa2, 0x8f, 0xb5, 0xc5, 0x5d, 0xf0, 0x6f, 0x4c, 0x52, 0xc9,
  0xde, 0x2b, 0xcb, 0xf6, 0x95, 0x58, 0x17, 0x18, 0x39, 0x95, 0x49, 0x7c,
  0xea, 0x95, 0x6a, 0xe5, 0x15, 0xd2, 0x26, 0x18, 0x98, 0xfa, 0x05, 0x10,
  0x15, 0x72, 0x8e, 0x5a, 0x8a, 0xaa, 0xc4, 0x2d, 0xad, 0x33, 0x17, 0x0d,
  0x04, 0x50, 0x7a, 0x33, 0xa8, 0x55, 0x21, 0xab, 0xdf, 0x1c, 0xba, 0x64,
  0xec, 0xfb, 0x85, 0x04, 0x58, 0xdb, 0xef, 0x0a, 0x8a, 0xea, 0x71, 0x57,
  0x5d, 0x06, 0x0c, 0x7d, 0xb3, 0x97, 0x0f, 0x85, 0xa6, 0xe1, 0xe4, 0xc7,
  0xab, 0xf5, 0xae, 0x8c, 0xdb, 0x09, 0x33, 0xd7, 0x1e, 0x8c, 0x94, 0xe0,
  0x4a, 0x25, 0x61, 0x9d, 0xce, 0xe3, 0xd2, 0x26, 0x1a, 0xd2, 0xee, 0x6b,
  0xf1, 0x2f, 0xfa, 0x06, 0xd9, 0x8a, 0x08, 0x64, 0xd8, 0x76, 0x02, 0x73,
  0x3e, 0xc8, 0x6a, 0x64, 0x52, 0x1f, 0x2b, 0x18, 0x17, 0x7b, 0x20, 0x0c,
  0xbb, 0xe1, 0x17, 0x57, 0x7a, 0x61, 0x5d, 0x6c, 0x77, 0x09, 0x88, 0xc0,
  0xba, 0xd9, 0x46, 0xe2, 0x08, 0xe2, 0x4f, 0xa0, 0x74, 0xe5, 0xab, 0x31,
  0x43, 0xdb, 0x5b, 0xfc, 0xe0, 0xfd, 0x10, 0x8e, 0x4b, 0x82, 0xd1, 0x20,
  0xa9, 0x3a, 0xd2, 0xca, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff
};

static word32  B_len=NLEN;
static byte B[B_len];

static int wc_SrpSetKeyH(Srp* srp, byte* secret, word32 size) //can this be static???
{
    SrpHash hash;

    srp->key = (byte*)XMALLOC(SHA512_DIGEST_SIZE, NULL, DYNAMIC_TYPE_SRP);
    if (srp->key == NULL)
        return 0;

    srp->keySz = SHA512_DIGEST_SIZE;

    int r = wc_InitSha512(&hash.data.sha512);
    if (!r) r = wc_Sha512Update(&hash.data.sha512, secret, size);
    if (!r) r = wc_Sha512Final(&hash.data.sha512, srp->key);

    memset(&hash,0,sizeof(SrpHash));

    return r;
}

void PairSetup::setup()
{
	word32 salt_len=16;
	byte salt[16];  //TODO initialize
	Srp srp;
	word32 g_len=1;
    byte g[]={0x05};
    byte    b[32];
    word32  b_len=32;

    int r = wc_SrpInit(&srp, SRP_TYPE_SHA512, SRP_CLIENT_SIDE);
    srp.keyGenFunc_cb = wc_SrpSetKeyH;

    if (!r) r = wc_SrpSetUsername(&srp, (const byte*)"Pair-Setup", 10);
    if (!r) r = wc_SrpSetParams(&srp, N, NLEN, g, g_len, salt, salt_len);
    if (!r) r = wc_SrpSetPassword(&srp, (const byte*)PIN_CODE, 10);
    if (!r) r = wc_SrpGetVerifier(&srp, B, &B_len); //use B to store v
    srp.side=SRP_SERVER_SIDE; //switch to server mode
    if (!r) r = wc_SrpSetVerifier(&srp, B, B_len); //used B to store v
    if (!r) r = wc_SrpSetPrivate(&srp, b, b_len);
    if (!r) r = wc_SrpGetPublic(&srp, B, &B_len);


    //step2:
    //wc_SrpComputeKey(&srp, objects[3], objects_len[3], B, B_len);
    //wc_SrpVerifyPeersProof(&srp, ios_SRP_proof, sizeof(ios_SRP_proof)

    byte proof[SHA512_DIGEST_SIZE];
    word32  proof_len=SHA512_DIGEST_SIZE;
    if (!r) r = wc_SrpGetProof(&srp, proof, &proof_len);

}

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

    static uint8_t __B [384];
    for (size_t i = 0; i < sizeof(__B); ++i)
    {
        __B[i] = 0x38;
    }

    buf B;
    B.length = sizeof(__B);
    B.data = __B;

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

    printf ("pair1: acc SRP salt len=%d, acc SRP public key B len: %d\n", s.length, B.length);
    return PairingHelper::pair1_reply(s, B);

}



//input: controller's SRP public key  and SRP proof
//process: verify controller's SRP public key
//         generate session key S  (SRP shared secret?)
//         verify controller's SRP proof
//output:  accessory's SRP proof
HttpReply PairSetup::step2(const buf& ios_SRP_public_key, const buf& ios_SRP_proof)
{

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

    printf ("pair2 output: acc SRP proof len=%d\n", acc_SRP_proof.length);
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
    plaintext = PairingHelper::pair3_encode(acc_pairing_ID, strlen(acc_pairing_ID), acc_Ed25519_ltpk, acc_proof);

    //6. Encrypt the sub-TLV
    //        ChachaEncoder chachaEnc = new ChachaEncoder(hkdf_session_key, "PS-Msg06".getBytes());
    //        byte[] ciphertext = chachaEnc.encodeCiphertext(plaintext);


    uint8_t _ciphertext [plaintext.length + 16];
    memcpy (&_ciphertext, plaintext.data, plaintext.length);
    for (int i = 0; i < 16; ++i)
    {
        _ciphertext[plaintext.length + i] = i;
    }

    buf ciphertext;
    ciphertext.data = _ciphertext;
    ciphertext.length = sizeof (_ciphertext);

    //byte[] ciphertext = joinBytes(plaintext, auth);

    printf ("pair3: encrypted data length: %d\n", ciphertext.length);
    return PairingHelper::pair3_reply(ciphertext);
}
