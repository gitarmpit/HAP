package main;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import srp6.*;
import bouncy.*;
import crypto.*;

public class PairSetup extends SRP6Session
{
    private final static BigInteger N_3072 = new BigInteger(
            "5809605995369958062791915965639201402176612226902900533702900882779736177890990861472094774477339581147373410185646378328043729800750470098210924487866935059164371588168047540943981644516632755067501626434556398193186628990071248660819361205119793693985433297036118232914410171876807536457391277857011849897410207519105333355801121109356897459426271845471397952675959440793493071628394122780510124618488232602464649876850458861245784240929258426287699705312584509625419513463605155428017165714465363094021609290561084025893662561222573202082865797821865270991145082200656978177192827024538990239969175546190770645685893438011714430426409338676314743571154537142031573004276428701433036381801705308659830751190352946025482059931306571004727362479688415574702596946457770284148435989129632853918392117997472632693078113129886487399347796982772784615865232621289656944284216824611318709764535152507354116344703769998514148343807");
    private final static BigInteger G = BigInteger.valueOf(5);
    private final static String IDENTIFIER = "Pair-Setup";

    private final SRP6CryptoParams config;
    private final String pin;

    private BigInteger v = null;
    private BigInteger b = null;

    protected ClientEvidenceRoutineImpl _clientEvidenceRoutine = null;
    protected ServerEvidenceRoutineImpl _serverEvidenceRoutine = null;

    private AccessoryCfg cfg;
    private Adv adv;


    public PairSetup(AccessoryCfg cfg, Adv adv)
    {
        super(0);

        _clientEvidenceRoutine = new ClientEvidenceRoutineImpl();
        _serverEvidenceRoutine = new ServerEvidenceRoutineImpl();

        this.cfg = cfg;
        this.adv = adv;

        config = new SRP6CryptoParams(N_3072, G, "SHA-512");
        digest = config.getMessageDigestInstance();

        if (digest == null)
            throw new IllegalArgumentException("Unsupported hash algorithm 'H': " + config.H);

        
        this.pin = cfg.getAccPin();
        this.s = cfg.getAccSalt();
        this.userID = IDENTIFIER;
    }

    //input: accessory's generated salt and PIN
    //output: accessory's salt and a generated SRP public key
    public HttpReply step1() throws Exception
    {
    	//<M2>  SRP start response 
    	
        SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator(config);
        verifierGenerator.setXRoutine(new XRoutineWithUserIdentity());
        v = verifierGenerator.generateVerifier(this.s, "Pair-Setup", pin);
        k = SRP6Routines.computeK(digest, config.N, config.g);
        digest.reset();

        b = generatePrivateValue(config.N, random);
        digest.reset();

    	//Generate an SRP public key
        B = SRP6Routines.computePublicServerValue(config.N, config.g, k, v, b);

        byte[] B_bytes = Util.bigIntegerToUnsignedByteArray(B);
        byte[] s_bytes = Util.bigIntegerToUnsignedByteArray(s);
        System.out.printf ("pair1: acc SRP salt len=%d, acc SRP public key B len: %d\n",  s_bytes.length, B_bytes.length);
        return PairHelper.pair1_reply(s_bytes, B_bytes);
        
    }

    //input: controller's SRP public key  and SRP proof
    //process: verify controller's SRP public key 
    //		   generate session key S  (SRP shared secret?)
    //         verify controller's SRP proof
    //output:  accessory's SRP proof 
    public HttpReply step2(byte[] ios_SRP_public_key, byte[] ios_SRP_proof) throws Exception
    {
    	
    	//<M4>  SRP Verify Response

        BigInteger A = new BigInteger(1, ios_SRP_public_key);
        BigInteger M1 = new BigInteger(1, ios_SRP_proof);
        

        this.A = A;
        this.M1 = M1;

        if (!SRP6Routines.isValidPublicValue(config.N, A))
        {
            System.out.println("Bad client public value 'A'");
            return HttpReply.generateAuthError(4);
        }
        
        u = SRP6Routines.computeU(digest, config.N, A, B);
        digest.reset();
    	
        //1. Use the iOS device's SRP public key to compute the SRP shared secret key  
        S = SRP6Routines.computeSessionKey(config.N, v, u, A, b);
        
        //2. Verify the iOS device's SRP proof
        SRP6ClientEvidenceContext cli_ctx = new SRP6ClientEvidenceContext(userID, s, A, B, S);
        BigInteger computedM1 = _clientEvidenceRoutine.computeClientEvidence(config, cli_ctx);

        if (!computedM1.equals(M1))
        {
            System.out.println("Pair: Bad client credentials");
            return HttpReply.generateAuthError(4);
        }

    	//3. Generate the accessory-side SRP proof
        SRP6ServerEvidenceContext srv_ctx = new SRP6ServerEvidenceContext(A, M1, S);
        M2 = _serverEvidenceRoutine.computeServerEvidence(config, srv_ctx);
        
        byte[] SRP_proof = Util.bigIntegerToUnsignedByteArray(M2);

        System.out.printf ("pair2 output: acc SRP proof len=%d\n", SRP_proof.length);
        System.out.println("step2 complete");
        
        return PairHelper.pair2_reply(SRP_proof);

    }

    //M5 encrypted input: controller's username,  Ed25519 longterm public key (ltpk),  Ed25519 sig (proof)
    //process: decrypt,
    //         verify controller's proof based on username and ltpk 
    //         create user (username -> ltpk)
    //         generate acessory's proof and ltpk
    //M6 return:  encrypted proof and ltpk
    public HttpReply step3(byte[] message, byte[] authData) throws Exception
    {
    	
    	//<M5> Verification
    	
        byte[] K = getK();  //SRP shared secret (S)
    	
    	
    	//2. decrypt message  (parameters not documented!)
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
    	  //the two init parameters salt and info are not documented:
        hkdf.init(new HKDFParameters(K, "Pair-Setup-Encrypt-Salt".getBytes(),  "Pair-Setup-Encrypt-Info".getBytes()));
        byte[] hkdf_session_key = new byte[32];  //used do decrypt and encrypt
        hkdf.generateBytes(hkdf_session_key, 0, 32);  //generate an enc/dec  key

    	 //decrypt
        ChachaDecoder chachaDec = new ChachaDecoder(hkdf_session_key, "PS-Msg05".getBytes());
        byte[] plaintext;
        try
        {
            plaintext = chachaDec.decodeCiphertext(authData, message);
        }
        catch (Exception ex)
        {
            System.out.println("pair M6: chacha decode error");
            return HttpReply.generateAuthError(6);
        }
        
        TLV_Decoder d = new TLV_Decoder();
        d.decode(plaintext);
        byte[] ios_pairing_ID = d.getData(MessageType.ID.getKey());
        byte[] ios_Ed25519_ltpk = d.getData(MessageType.PUBLIC_KEY.getKey());
        byte[] ios_Ed25519_proof = d.getData(MessageType.SIGNATURE.getKey());

        //3. Derive iOSDeviceX from the SRP shared secret by using HKDF-SHA-512
        
        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(K, "Pair-Setup-Controller-Sign-Salt".getBytes(),  "Pair-Setup-Controller-Sign-Info".getBytes()));

        byte[] iOSDeviceX  = new byte[32];
        hkdf.generateBytes(iOSDeviceX , 0, 32);

        //4. Construct iOSDeviceInfo by concatenating:        
        byte[] iOSDeviceInfo = Util.joinBytes(iOSDeviceX, ios_pairing_ID, ios_Ed25519_ltpk);

        //5. Use Ed25519 to verify the signature of the constructed iOSDeviceInfo with the iOSDeviceLTPK
        if (!new EdsaVerifier(ios_Ed25519_ltpk).verify(iOSDeviceInfo, ios_Ed25519_proof))
        {
            System.out.println("Pair M6: Invalid signature");
            return HttpReply.generateAuthError(6);
        }
        
        //6. Persistently save the iOSDevicePairingID and iOSDeviceLTPK as a pairing
        cfg.createUser(new String(ios_pairing_ID), ios_Ed25519_ltpk);

        System.out.println("paired, set discoverable to false");

        adv.setDiscoverable(false);
        
        ////////////////////////////////////////////////
        // <M6> generate response

        //1. Generate  Ed25519 long-term public key, AccessoryLTPK, and long-term secret key, AccessoryLTSK  (pre-generated)
        EdsaSigner signer = new EdsaSigner(cfg.getAccPrivateKey());
        byte[] acc_Ed25519_ltpk = signer.getPublicKey();
    	
        //2. Derive AccessoryX from the SRP shared secret by using HKDF-SHA-512
        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(K, "Pair-Setup-Accessory-Sign-Salt".getBytes(), "Pair-Setup-Accessory-Sign-Info".getBytes()));

        byte[] accessoryX = new byte[32];
        hkdf.generateBytes(accessoryX, 0, 32);
    	
    	
//      //3. Concatenate AccessoryX + AccessoryPairingID + Acc LTPK
        byte[] accessoryInfo = Util.joinBytes(accessoryX, cfg.getAccPairingID().getBytes(), acc_Ed25519_ltpk);
      
    	//4. Use Ed25519 to generate AccessorySignature by signing AccessoryInfo with its long-term secret 	key, AccessoryLTSK
        byte[] acc_proof = signer.sign(accessoryInfo);  //signature
        
        System.out.printf ("pair3: acc_ltpk len=%d, acc_proof len: %d\n", acc_Ed25519_ltpk.length, acc_proof.length);

        //5. Construct the sub-TLV 
        plaintext = PairHelper.pair3_encode(cfg.getAccPairingID().getBytes(), acc_Ed25519_ltpk, acc_proof);

        //6. Encrypt the sub-TLV
        ChachaEncoder chachaEnc = new ChachaEncoder(hkdf_session_key, "PS-Msg06".getBytes());
        byte[] ciphertext = chachaEnc.encodeCiphertext(plaintext);

        System.out.printf ("pair3: encrypted data length: %d\n", ciphertext.length);
        return PairHelper.pair3_reply(ciphertext);
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////


    // ///////////////////////////////////

    private static BigInteger generatePrivateValue(BigInteger N, SecureRandom random)
    {
        final int minBits = Math.min(3072, N.bitLength() / 2);

        BigInteger min = BigInteger.ONE.shiftLeft(minBits - 1);
        BigInteger max = N.subtract(BigInteger.ONE);

        return createRandomBigIntegerInRange(min, max, random);
    }

    private static BigInteger createRandomBigIntegerInRange(final BigInteger min, final BigInteger max,
            final SecureRandom random)
    {
        final int cmp = min.compareTo(max);

        if (cmp >= 0)
        {

            if (cmp > 0)
                throw new IllegalArgumentException("'min' may not be greater than 'max'");

            return min;
        }

        if (min.bitLength() > max.bitLength() / 2)
            return createRandomBigIntegerInRange(BigInteger.ZERO, max.subtract(min), random).add(min);

        final int MAX_ITERATIONS = 1000;

        for (int i = 0; i < MAX_ITERATIONS; ++i)
        {

            BigInteger x = new BigInteger(max.bitLength(), random);

            if (x.compareTo(min) >= 0 && x.compareTo(max) <= 0)
                return x;
        }

        return new BigInteger(max.subtract(min).bitLength() - 1, random).add(min);
    }

    private byte[] getK()
    {
        BigInteger S = getSessionKey(false);
        byte[] sBytes = Util.bigIntegerToUnsignedByteArray(S);
        return digest.digest(sBytes);
    }


    static class ServerEvidenceRoutineImpl implements ServerEvidenceRoutine
    {

        @Override
        public BigInteger computeServerEvidence(SRP6CryptoParams cryptoParams, SRP6ServerEvidenceContext ctx)
        {

            MessageDigest digest;
            try
            {
                digest = MessageDigest.getInstance(cryptoParams.H);
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new RuntimeException("Could not locate requested algorithm", e);
            }

            byte[] hS = digest.digest(Util.bigIntegerToUnsignedByteArray(ctx.S));

            digest.update(Util.bigIntegerToUnsignedByteArray(ctx.A));
            digest.update(Util.bigIntegerToUnsignedByteArray(ctx.M1));
            digest.update(hS);

            return new BigInteger(1, digest.digest());
        }
    }

    static class ClientEvidenceRoutineImpl implements ClientEvidenceRoutine
    {

        public ClientEvidenceRoutineImpl()
        {
        }

        /**
         * Calculates M1 according to the following formula:
         * 
         * M1 = H(H(N) xor H(g) || H(username) || s || A || B || H(S))
         */
        @Override
        public BigInteger computeClientEvidence(SRP6CryptoParams cryptoParams, SRP6ClientEvidenceContext ctx)
        {

            MessageDigest digest;
            try
            {
                digest = MessageDigest.getInstance(cryptoParams.H);
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new RuntimeException("Could not locate requested algorithm", e);
            }
            digest.update(Util.bigIntegerToUnsignedByteArray(cryptoParams.N));
            byte[] hN = digest.digest();

            digest.update(Util.bigIntegerToUnsignedByteArray(cryptoParams.g));
            byte[] hg = digest.digest();

            byte[] hNhg = xor(hN, hg);

            digest.update(ctx.userID.getBytes(StandardCharsets.UTF_8));
            byte[] hu = digest.digest();

            digest.update(Util.bigIntegerToUnsignedByteArray(ctx.S));
            byte[] hS = digest.digest();

            digest.update(hNhg);
            digest.update(hu);
            digest.update(Util.bigIntegerToUnsignedByteArray(ctx.s));
            digest.update(Util.bigIntegerToUnsignedByteArray(ctx.A));
            digest.update(Util.bigIntegerToUnsignedByteArray(ctx.B));
            digest.update(hS);
            BigInteger ret = new BigInteger(1, digest.digest());
            return ret;
        }

        private static byte[] xor(byte[] b1, byte[] b2)
        {
            byte[] result = new byte[b1.length];
            for (int i = 0; i < b1.length; i++)
            {
                result[i] = (byte) (b1[i] ^ b2[i]);
            }
            return result;
        }

    }

}
