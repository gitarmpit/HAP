package main;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import srp6.ClientEvidenceRoutine;
import srp6.SRP6ClientEvidenceContext;
import srp6.SRP6CryptoParams;
import srp6.SRP6Routines;
import srp6.SRP6ServerEvidenceContext;
import srp6.SRP6Session;
import srp6.SRP6VerifierGenerator;
import srp6.ServerEvidenceRoutine;
import srp6.URoutineContext;
import srp6.XRoutineWithUserIdentity;
import bouncy.HKDFBytesGenerator;
import bouncy.HKDFParameters;
import bouncy.SHA512Digest;
import crypto.ChachaDecoder;
import crypto.ChachaEncoder;
import crypto.EdsaSigner;
import crypto.EdsaVerifier;

public class PairSetup extends SRP6Session
{
//    public static enum State
//    {
//        INIT, STEP_1, STEP_2
//    }

    private final static BigInteger N_3072 = new BigInteger(
            "5809605995369958062791915965639201402176612226902900533702900882779736177890990861472094774477339581147373410185646378328043729800750470098210924487866935059164371588168047540943981644516632755067501626434556398193186628990071248660819361205119793693985433297036118232914410171876807536457391277857011849897410207519105333355801121109356897459426271845471397952675959440793493071628394122780510124618488232602464649876850458861245784240929258426287699705312584509625419513463605155428017165714465363094021609290561084025893662561222573202082865797821865270991145082200656978177192827024538990239969175546190770645685893438011714430426409338676314743571154537142031573004276428701433036381801705308659830751190352946025482059931306571004727362479688415574702596946457770284148435989129632853918392117997472632693078113129886487399347796982772784615865232621289656944284216824611318709764535152507354116344703769998514148343807");
    private final static BigInteger G = BigInteger.valueOf(5);

    private final SRP6CryptoParams config;
    private final String pin;

    private BigInteger v = null;
    private BigInteger b = null;
    //private State state;

    protected ClientEvidenceRoutineImpl _clientEvidenceRoutine = null;
    protected ServerEvidenceRoutineImpl _serverEvidenceRoutine = null;

    private AccessoryCfg cfg;
    private Adv adv;

    // pair step3
    private byte[] hkdf_enc_key;

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

        updateLastActivityTime();
        this.pin = cfg.getPin();
        this.s = cfg.getSalt();
        this.userID = "Pair-Setup";
    }

    //returns our salt and public key
    public PairSetupReply1 step1() throws Exception
    {
        SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator(config);
        verifierGenerator.setXRoutine(new XRoutineWithUserIdentity());
        v = verifierGenerator.generateVerifier(this.s, "Pair-Setup", pin);

        k = SRP6Routines.computeK(digest, config.N, config.g);
        digest.reset();

        b = generatePrivateValue(config.N, random);
        digest.reset();

        B = SRP6Routines.computePublicServerValue(config.N, config.g, k, v, b);

        PairSetupReply1 reply = new PairSetupReply1();
        reply.salt = Encoder.bigIntToBytes(cfg.getSalt());
        reply.publicKey = Encoder.bigIntToBytes(B);
        return reply;
    }

    //returns proof
    public byte[] step2(byte[] pkA, byte[] proofM1) throws Exception
    {
        BigInteger A = new BigInteger(1, pkA);
        BigInteger M1 = new BigInteger(1, proofM1);

        this.A = A;
        this.M1 = M1;

        if (!SRP6Routines.isValidPublicValue(config.N, A))
        {
            throw new Exception("Bad client public value 'A'");
            //return HttpReply.generateAuthError(4);
        }
        
        if (hashedKeysRoutine != null)
        {
            URoutineContext hashedKeysContext = new URoutineContext(A, B);
            u = hashedKeysRoutine.computeU(config, hashedKeysContext);
        }
        else
        {
            u = SRP6Routines.computeU(digest, config.N, A, B);
            digest.reset();
        }

        //share session key
        S = SRP6Routines.computeSessionKey(config.N, v, u, A, b);

        BigInteger computedM1;
        SRP6ClientEvidenceContext cli_ctx = new SRP6ClientEvidenceContext(userID, s, A, B, S);
        computedM1 = _clientEvidenceRoutine.computeClientEvidence(config, cli_ctx);

        if (!computedM1.equals(M1))
        {
            throw new Exception("Pair: Bad client credentials");
            //return HttpReply.generateAuthError(4);
        }

        SRP6ServerEvidenceContext srv_ctx = new SRP6ServerEvidenceContext(A, M1, S);
        M2 = _serverEvidenceRoutine.computeServerEvidence(config, srv_ctx);
  
        return Encoder.bigIntToBytes(M2);
    }

    private byte[] step3_decrypt(byte[] authData, byte[] message) throws Exception
    {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(getK(), "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] okm = hkdf_enc_key = new byte[32];
        hkdf.generateBytes(okm, 0, 32);

        // decrypt

        ChachaDecoder chachaDec = new ChachaDecoder(okm, "PS-Msg05".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext;
        try
        {
            plaintext = chachaDec.decodeCiphertext(authData, message);
        }
        catch (Exception ex)
        {
            throw new Exception("pair M6: chacha decode error");
            //return HttpReply.generateAuthError(6);
        }
        return plaintext;
        
    }
    
    public byte[] step3(byte[] authData, byte[] message) throws Exception
    {
        byte[] plaintext = step3_decrypt(authData, message);
        Decoder d = new Decoder();
        d.decode(plaintext);
        byte[] username = d.getData(MessageType.USERNAME.getKey());
        byte[] longTermPublicKey = d.getData(MessageType.PUBLIC_KEY.getKey());
        byte[] proof = d.getData(MessageType.SIGNATURE.getKey());

        // create user
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        
        byte[] K = getK();
        
        hkdf.init(new HKDFParameters(K, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));

        byte[] okm = new byte[32];
        hkdf.generateBytes(okm, 0, 32);

        byte[] completeData = joinBytes(okm, username, longTermPublicKey);

        if (!new EdsaVerifier(longTermPublicKey).verify(completeData, proof))
        {
            throw new Exception("Pair M6: Invalid signature");
            //return HttpReply.generateAuthError(6);
        }
        
        cfg.createUser(new String(username, StandardCharsets.UTF_8), longTermPublicKey);

        System.out.println("paired, set discoverable to false");

        adv.setDiscoverable(false);

        // creating response

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(K, "Pair-Setup-Accessory-Sign-Salt".getBytes(StandardCharsets.UTF_8),
                "Pair-Setup-Accessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));

        okm = new byte[32];
        hkdf.generateBytes(okm, 0, 32);

        EdsaSigner signer = new EdsaSigner(cfg.getPrivateKey());

        byte[] material = joinBytes(okm, cfg.getMac().getBytes(StandardCharsets.UTF_8), signer.getPublicKey());

        proof = signer.sign(material);

        byte[] public_key = signer.getPublicKey();
        
        System.out.printf ("pair3: signer (public key) len=%d, sig (proof) len: %d\n", 
                public_key.length, proof.length);

        Encoder encoder = new Encoder();
        encoder.add(MessageType.USERNAME.getKey(), cfg.getMac().getBytes(StandardCharsets.UTF_8));
        encoder.add(MessageType.PUBLIC_KEY.getKey(), public_key);
        encoder.add(MessageType.SIGNATURE.getKey(), proof);
        plaintext = encoder.toByteArray();

        ChachaEncoder chachaEnc = new ChachaEncoder(hkdf_enc_key, "PS-Msg06".getBytes(StandardCharsets.UTF_8));
        return chachaEnc.encodeCiphertext(plaintext);
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////

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

    private byte[] getK()
    {
        //BigInteger S = getSessionKey(false);
        byte[] sBytes = bigIntegerToUnsignedByteArray(S);
        return digest.digest(sBytes);
    }

    private static byte[] bigIntegerToUnsignedByteArray(BigInteger i)
    {
        byte[] array = i.toByteArray();
        if (array[0] == 0)
        {
            array = Arrays.copyOfRange(array, 1, array.length);
        }
        return array;
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

            byte[] hS = digest.digest(PairSetup.bigIntegerToUnsignedByteArray(ctx.S));

            digest.update(PairSetup.bigIntegerToUnsignedByteArray(ctx.A));
            digest.update(PairSetup.bigIntegerToUnsignedByteArray(ctx.M1));
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
            digest.update(PairSetup.bigIntegerToUnsignedByteArray(cryptoParams.N));
            byte[] hN = digest.digest();

            digest.update(PairSetup.bigIntegerToUnsignedByteArray(cryptoParams.g));
            byte[] hg = digest.digest();

            byte[] hNhg = xor(hN, hg);

            digest.update(ctx.userID.getBytes(StandardCharsets.UTF_8));
            byte[] hu = digest.digest();

            digest.update(PairSetup.bigIntegerToUnsignedByteArray(ctx.S));
            byte[] hS = digest.digest();

            digest.update(hNhg);
            digest.update(hu);
            digest.update(PairSetup.bigIntegerToUnsignedByteArray(ctx.s));
            digest.update(PairSetup.bigIntegerToUnsignedByteArray(ctx.A));
            digest.update(PairSetup.bigIntegerToUnsignedByteArray(ctx.B));
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
