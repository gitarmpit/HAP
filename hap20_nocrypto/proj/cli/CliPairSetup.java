package cli;

import java.math.BigInteger;
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;


//import srp6.*;
//import bouncy.*;
//import crypto.*;
import main.*;

public class CliPairSetup //extends SRP6Session
{
    private final static BigInteger N_3072 = new BigInteger(
            "5809605995369958062791915965639201402176612226902900533702900882779736177890990861472094774477339581147373410185646378328043729800750470098210924487866935059164371588168047540943981644516632755067501626434556398193186628990071248660819361205119793693985433297036118232914410171876807536457391277857011849897410207519105333355801121109356897459426271845471397952675959440793493071628394122780510124618488232602464649876850458861245784240929258426287699705312584509625419513463605155428017165714465363094021609290561084025893662561222573202082865797821865270991145082200656978177192827024538990239969175546190770645685893438011714430426409338676314743571154537142031573004276428701433036381801705308659830751190352946025482059931306571004727362479688415574702596946457770284148435989129632853918392117997472632693078113129886487399347796982772784615865232621289656944284216824611318709764535152507354116344703769998514148343807");
    private final static BigInteger G = BigInteger.valueOf(5);
    private final static String IDENTIFIER = "Pair-Setup";

    //private final SRP6CryptoParams config;
    private final String pin = "111-11-111";
    private byte[] ios_pairing_ID;    

//    protected ClientEvidenceRoutineImpl _clientEvidenceRoutine = null;
//    protected ServerEvidenceRoutineImpl _serverEvidenceRoutine = null;
    
    byte[] ios_Ed25519_ltsk;

    private byte[] acc_pairing_ID;  //will be set
    private byte[] acc_Ed25519_ltpk; //will be set 

    private BigInteger s;
    private String userID;
    
    public CliPairSetup(byte[] ios_pairing_ID, byte[] ios_Ed25519_ltsk, byte[] acc_salt)
    {
       // super(0);
        
        this.ios_pairing_ID = ios_pairing_ID;
        this.ios_Ed25519_ltsk = ios_Ed25519_ltsk;

//        _clientEvidenceRoutine = new ClientEvidenceRoutineImpl();
//        _serverEvidenceRoutine = new ServerEvidenceRoutineImpl();

//        config = new SRP6CryptoParams(N_3072, G, "SHA-512");
//        digest = config.getMessageDigestInstance();
//
//        if (digest == null)
//            throw new IllegalArgumentException("Unsupported hash algorithm 'H': " + config.H);
        
        this.s = new BigInteger(1, acc_salt);
        this.userID = IDENTIFIER;
    }

    public byte[] step_M3 (byte[] acc_SRP_publicKey)
    {
        
//    	B = new BigInteger (1, acc_SRP_publicKey);
//    	
//        k = SRP6Routines.computeK(digest, config.N, config.g);
//        digest.reset();
//
//        BigInteger a = generatePrivateValue(config.N, random);
//        digest.reset();
//        
//
//        //ios SRP public key
//        A = SRP6Routines.computePublicClientValue(config.N, config.g, a);
//        digest.reset();
//        
//        u = SRP6Routines.computeU(digest, config.N, A, B);
//        digest.reset();
//        
//        //BigInteger x = SRP6Routines.computeX(digest, bigIntegerToUnsignedByteArray(this.s), pin.getBytes());
//        BigInteger x = new XRoutineWithUserIdentity ().
//        		computeX(digest, bigIntegerToUnsignedByteArray(this.s), IDENTIFIER.getBytes(), pin.getBytes());
//        digest.reset();
//       
//        S = SRP6Routines.computeSessionKey(config.N, config.g, k, x, u, a, B);
//        
//        
//        SRP6ClientEvidenceContext cli_ctx = new SRP6ClientEvidenceContext(IDENTIFIER, s, A, B, S);
//        M1 = _clientEvidenceRoutine.computeClientEvidence(config, cli_ctx);
        
//        byte[] ios_SRP_proof = bigIntegerToUnsignedByteArray(M1);
//        byte[] ios_SRP_pk = bigIntegerToUnsignedByteArray(A);

        byte[] ios_SRP_pk = new byte[384];
        for (int i = 0; i < ios_SRP_pk.length; ++i)
        {
            ios_SRP_pk[i] = 0x39;
        }

        byte[] ios_SRP_proof = new byte[64];
        for (int i = 0; i < ios_SRP_proof.length; ++i)
        {
            ios_SRP_proof[i] = 0x64;
        }

        TLV_Encoder e = new TLV_Encoder();
        e.add(6, 3);
        e.add(3, ios_SRP_pk);
        e.add(4, ios_SRP_proof);
        
        return e.toByteArray();
    }
    
    public byte[] step_M5 (byte[] acc_SRP_proof)  throws Exception
    {
    	
//        SRP6ServerEvidenceContext srv_ctx = new SRP6ServerEvidenceContext(A, M1, S);
//        BigInteger computedM2 = _serverEvidenceRoutine.computeServerEvidence(config, srv_ctx);
//        BigInteger M2 = new BigInteger(1, acc_SRP_proof);
//        
//        if (!computedM2.equals(M2))
//        {
//            throw new Exception("M5: failed to very acc proof");
//        }
//        
//        ////////////////////////////
//        //Generate iOS long term public key
//        EdsaSigner signer = new EdsaSigner(ios_Ed25519_ltsk);
//        byte[] ios_Ed25519_ltpk = signer.getPublicKey();
//        
//        byte[] K = getK();  //SRP shared secret (S)
//        
//        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
//        hkdf.init(new HKDFParameters(K, "Pair-Setup-Controller-Sign-Salt".getBytes(),  "Pair-Setup-Controller-Sign-Info".getBytes()));
//
//        byte[] iOSDeviceX  = new byte[32];
//        hkdf.generateBytes(iOSDeviceX , 0, 32);
//
//        //Construct iOSDeviceInfo by concatenating:        
//        byte[] iOSDeviceInfo = joinBytes(iOSDeviceX, ios_pairing_ID, ios_Ed25519_ltpk);
//        
//        //Generate iOSDeviceSignature by signing iOSDeviceInfo with  iOSDeviceLTSK, using Ed25519
//        byte[] ios_proof = signer.sign(iOSDeviceInfo);  //signature
//
        
        byte[] ios_Ed25519_ltpk = new byte[32];
        for (int i = 0; i < ios_Ed25519_ltpk.length; ++i)
        {
            ios_Ed25519_ltpk[i] = 0x32;
        }

        byte[] ios_Ed25519_proof = new byte[64];
        for (int i = 0; i < ios_Ed25519_proof.length; ++i)
        {
            ios_Ed25519_proof[i] = 0x32;
        }
       
        TLV_Encoder e = new TLV_Encoder();
        e.add(MessageType.ID.getKey(), ios_pairing_ID);
        e.add(MessageType.PUBLIC_KEY.getKey(), ios_Ed25519_ltpk);
        e.add(MessageType.SIGNATURE.getKey(), ios_Ed25519_proof);
        
//        hkdf = new HKDFBytesGenerator(new SHA512Digest());
//    	  //the two init parameters salt and info are not documented:
//        hkdf.init(new HKDFParameters(K, "Pair-Setup-Encrypt-Salt".getBytes(),  "Pair-Setup-Encrypt-Info".getBytes()));
//        byte[] hkdf_enc_key = new byte[32];  //used do decrypt and encrypt
//        hkdf.generateBytes(hkdf_enc_key, 0, 32);  //generate an enc/dec  key

        
//        ChachaEncoder chachaEnc = new ChachaEncoder(hkdf_enc_key, "PS-Msg05".getBytes());
//        byte[] ciphertext = chachaEnc.encodeCiphertext(e.toByteArray());
        
        byte[] auth = new byte[16];
        for (int i = 0; i < 16; ++i)
        {
            auth[i] = (byte)i;
        }
        byte[] ciphertext = joinBytes(e.toByteArray(), auth); 
        
        e = new TLV_Encoder();
        e.add(MessageType.ENCRYPTED_DATA.getKey(), ciphertext);
        e.add(MessageType.STATE.getKey(), (short) 5);
        
        return e.toByteArray();
    	
    }
    
    public boolean step_M6_verify(byte[] message, byte[] authData) throws Exception
    {
    	
    	//<M5> Verification
    	
//        byte[] K = getK();  //SRP shared secret (S)
//    	
//    	
//    	//2. decrypt message  (parameters not documented!)
//        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
//    	  //the two init parameters salt and info are not documented:
//        hkdf.init(new HKDFParameters(K, "Pair-Setup-Encrypt-Salt".getBytes(),  "Pair-Setup-Encrypt-Info".getBytes()));
//        byte[] hkdf_session_key = new byte[32];  //used do decrypt and encrypt
//        hkdf.generateBytes(hkdf_session_key, 0, 32);  //generate an enc/dec  key
//
//    	 //decrypt
//        ChachaDecoder chachaDec = new ChachaDecoder(hkdf_session_key, "PS-Msg06".getBytes());
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
        
        //these two will be needed later 
        acc_pairing_ID = d.getData(MessageType.ID.getKey());
        acc_Ed25519_ltpk = d.getData(MessageType.PUBLIC_KEY.getKey());
        
//        byte[] acc_Ed25519_proof = d.getData(MessageType.SIGNATURE.getKey());
//        
//        hkdf = new HKDFBytesGenerator(new SHA512Digest());
//        hkdf.init(new HKDFParameters(K, "Pair-Setup-Accessory-Sign-Salt".getBytes(),  "Pair-Setup-Accessory-Sign-Info".getBytes()));
//
//        byte[] accesoryX  = new byte[32];
//        hkdf.generateBytes(accesoryX , 0, 32);
//
//        byte[] accDeviceInfo = joinBytes(accesoryX, acc_pairing_ID, acc_Ed25519_ltpk);
//
//        if (!new EdsaVerifier(acc_Ed25519_ltpk).verify(accDeviceInfo, acc_Ed25519_proof))
//        {
//            System.out.println("Pair M6 verify: Invalid signature");
//            return false;
//        }
        
        return true;
    }    
    
    public byte[] getAccPairingID() 
    {
    	return acc_pairing_ID;
    }
    
    public byte[] getAccLTPK() 
    {
    	return acc_Ed25519_ltpk;
    }
    
    
    public static byte[] joinBytes(byte[]... piece)
    {
        int pos = 0;
        int length = 0;
        for (int i = 0; i < piece.length; i++)
        {
            length += piece[i].length;
        }
        byte[] ret = new byte[length];
        for (int i = 0; i < piece.length; i++)
        {
            System.arraycopy(piece[i], 0, ret, pos, piece[i].length);
            pos += piece[i].length;
        }
        return ret;
    }

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
//
//    private byte[] getK()
//    {
//        BigInteger S = getSessionKey(false);
//        byte[] sBytes = bigIntegerToUnsignedByteArray(S);
//        return digest.digest(sBytes);
//    }

    private static byte[] bigIntegerToUnsignedByteArray(BigInteger i)
    {
        byte[] array = i.toByteArray();
        if (array[0] == 0)
        {
            array = Arrays.copyOfRange(array, 1, array.length);
        }
        return array;
    }

//    static class ServerEvidenceRoutineImpl implements ServerEvidenceRoutine
//    {
//
//        @Override
//        public BigInteger computeServerEvidence(SRP6CryptoParams cryptoParams, SRP6ServerEvidenceContext ctx)
//        {
//
//            MessageDigest digest;
//            try
//            {
//                digest = MessageDigest.getInstance(cryptoParams.H);
//            }
//            catch (NoSuchAlgorithmException e)
//            {
//                throw new RuntimeException("Could not locate requested algorithm", e);
//            }
//
//            byte[] hS = digest.digest(CliPairSetup.bigIntegerToUnsignedByteArray(ctx.S));
//
//            digest.update(CliPairSetup.bigIntegerToUnsignedByteArray(ctx.A));
//            digest.update(CliPairSetup.bigIntegerToUnsignedByteArray(ctx.M1));
//            digest.update(hS);
//
//            return new BigInteger(1, digest.digest());
//        }
//    }
//
//    static class ClientEvidenceRoutineImpl implements ClientEvidenceRoutine
//    {
//
//        public ClientEvidenceRoutineImpl()
//        {
//        }
//
//        /**
//         * Calculates M1 according to the following formula:
//         * 
//         * M1 = H(H(N) xor H(g) || H(username) || s || A || B || H(S))
//         */
//        @Override
//        public BigInteger computeClientEvidence(SRP6CryptoParams cryptoParams, SRP6ClientEvidenceContext ctx)
//        {
//
//            MessageDigest digest;
//            try
//            {
//                digest = MessageDigest.getInstance(cryptoParams.H);
//            }
//            catch (NoSuchAlgorithmException e)
//            {
//                throw new RuntimeException("Could not locate requested algorithm", e);
//            }
//            digest.update(CliPairSetup.bigIntegerToUnsignedByteArray(cryptoParams.N));
//            byte[] hN = digest.digest();
//
//            digest.update(CliPairSetup.bigIntegerToUnsignedByteArray(cryptoParams.g));
//            byte[] hg = digest.digest();
//
//            byte[] hNhg = xor(hN, hg);
//
//            digest.update(ctx.userID.getBytes(StandardCharsets.UTF_8));
//            byte[] hu = digest.digest();
//
//            digest.update(CliPairSetup.bigIntegerToUnsignedByteArray(ctx.S));
//            byte[] hS = digest.digest();
//
//            digest.update(hNhg);
//            digest.update(hu);
//            digest.update(CliPairSetup.bigIntegerToUnsignedByteArray(ctx.s));
//            digest.update(CliPairSetup.bigIntegerToUnsignedByteArray(ctx.A));
//            digest.update(CliPairSetup.bigIntegerToUnsignedByteArray(ctx.B));
//            digest.update(hS);
//            BigInteger ret = new BigInteger(1, digest.digest());
//            return ret;
//        }
//
//        private static byte[] xor(byte[] b1, byte[] b2)
//        {
//            byte[] result = new byte[b1.length];
//            for (int i = 0; i < b1.length; i++)
//            {
//                result[i] = (byte) (b1[i] ^ b2[i]);
//            }
//            return result;
//        }
//
//    }

}
