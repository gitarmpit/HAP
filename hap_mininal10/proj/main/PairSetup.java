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
import srp6.SRP6Exception;
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
    public static enum State
    {
        INIT, STEP_1, STEP_2
    }

    private final static BigInteger N_3072 = new BigInteger(
            "5809605995369958062791915965639201402176612226902900533702900882779736177890990861472094774477339581147373410185646378328043729800750470098210924487866935059164371588168047540943981644516632755067501626434556398193186628990071248660819361205119793693985433297036118232914410171876807536457391277857011849897410207519105333355801121109356897459426271845471397952675959440793493071628394122780510124618488232602464649876850458861245784240929258426287699705312584509625419513463605155428017165714465363094021609290561084025893662561222573202082865797821865270991145082200656978177192827024538990239969175546190770645685893438011714430426409338676314743571154537142031573004276428701433036381801705308659830751190352946025482059931306571004727362479688415574702596946457770284148435989129632853918392117997472632693078113129886487399347796982772784615865232621289656944284216824611318709764535152507354116344703769998514148343807");
    private final static BigInteger G = BigInteger.valueOf(5);
    private final static String IDENTIFIER = "Pair-Setup";

    private final SRP6CryptoParams config;
    private final String pin;

    private BigInteger v = null;
    private BigInteger b = null;
    private State state;

    protected ClientEvidenceRoutineImpl _clientEvidenceRoutine = null;
    protected ServerEvidenceRoutineImpl _serverEvidenceRoutine = null;

    private AuthInfo authInfo;

    private final byte _b[] = { 0, -58, 29, -97, 52, -96, 57, -25, -99, 53, -105, -21, 28, -75, -67, 5, 75, -121, -117, 122, -56, -64, -124, 127, -15, 19, -4, 20, -91, -97, 72,
            37, 84, 70, 79, -21, 2, 42, -59, -12, 18, 30, 65, -68, 91, -25, -69, 125, -24, -88, 61, -118, -110, 110, -95, 63, -17, 26, 49, -84, -17, 17, 103, -84, -40, -9, -107,
            -117, 20, -104, 27, 50, -66, 39, 113, -64, 72, -20, -63, -43, -93, -67, 58, 40, -74, 69, 115, -2, -76, 50, 31, -59, -67, -100, 32, -65, -111, -120, 5, 56, 84, -32,
            -56, -28, 14, -125, 95, 0, -108, 11, -41, -37, 27, -20, 109, 26, -120, 46, -66, -108, 57, -3, -122, -11, -111, -67, -49, 114, 24, -55, -114, 66, 9, 118, 66, 4, 38, 86,
            -21, -82, 46, 25, -92, -10, 125, 105, -51, 11, -67, 106, 125, -11, 66, 102, 12, 110, 97, -79, 120, -86, -6, -9, 19, -84, 42, -37, -33, 92, -96, 102, 34, 111, 67, -97,
            15, 72, -27, -6, -92, 0, 60, 99, 3, 66, -40, -52, -31, 51, 13, 66, 53, 33, -117, -10, 12, -70, 90, 28, 30, -71, 20, 122, 75, 85, 24, -76, -14, 89, -43, 45, 75, 66, 46,
            -25, -95, -26, -63, 95, -103, -74, 10, 3, -21, 53, 66, -71, 65, -81, 68, 48, -51, -45, -7, -26, -52, 97, -52, 18, 40, -65, 90, 125, -26, 67, 109, 43, -86, 98, 54, -60,
            95, 14, -89, 16, 21, -77, -35, -28, 0, -1, -85, -89, -64, -69, -6, -104, 119, -59, 41, 95, 67, -83, -98, -70, 104, -107, -97, 125, 75, 52, 72, -15, -1, 103, -110, 77,
            -52, -128, -48, 58, 25, -88, 45, -11, -4, 79, -43, -62, -51, -36, -10, -111, 77, -28, 99, 3, 18, -58, -71, 114, 46, -16, 21, -79, -54, 82, 120, 107, 57, 89, 103, -88,
            68, 0, 6, 8, 9, -2, -68, -87, -65, -126, -73, -3, -63, -79, -46, 127, -97, -52, -43, -2, -100, 126, -101, -5, 68, 103, -25, -29, 6, -62, 80, -48, 74, -26, 17, 9, 51,
            7, -66, -10, -5, 88, 47, 108, -99, 31, 4, 83, 34, -38, -64, 88, -62, 99, 49, -86, -60, -60, 0, -116, 51, -102, -71 };

    // pair step3
    private byte[] hkdf_enc_key;

    public PairSetup(AuthInfo authInfo)
    {
        super(0);

        _clientEvidenceRoutine = new ClientEvidenceRoutineImpl();
        _serverEvidenceRoutine = new ServerEvidenceRoutineImpl();

        this.authInfo = authInfo;

        config = new SRP6CryptoParams(N_3072, G, "SHA-512");
        digest = config.getMessageDigestInstance();

        if (digest == null)
            throw new IllegalArgumentException("Unsupported hash algorithm 'H': " + config.H);

        state = State.INIT;

        updateLastActivityTime();
        this.pin = authInfo.getPin();
        this.s = authInfo.getSalt();
        this.userID = IDENTIFIER;
    }

    public byte[] step1() throws Exception
    {
        if (state != State.INIT)
        {
            System.out.println("Session is not in state INIT when receiving step1");
            return new byte[0];
        }

        SRP6VerifierGenerator verifierGenerator = new SRP6VerifierGenerator(config);
        verifierGenerator.setXRoutine(new XRoutineWithUserIdentity());
        v = verifierGenerator.generateVerifier(this.s, IDENTIFIER, pin);

        k = SRP6Routines.computeK(digest, config.N, config.g);
        digest.reset();

        b = generatePrivateValue(config.N, random);
        //b = new BigInteger(_b);
        digest.reset();

        // System.out.println ("step1: b: ");
        // for (byte _b : b.toByteArray())
        // {
        // System.out.printf ("%d, ", _b);
        // }
        // System.out.println();

        B = SRP6Routines.computePublicServerValue(config.N, config.g, k, v, b);

        state = State.STEP_1;

        updateLastActivityTime();

        Encoder encoder = new Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 0x02);
        encoder.add(MessageType.SALT.getKey(), this.s);
        encoder.add(MessageType.PUBLIC_KEY.getKey(), B);
        return encoder.toByteArray();

    }

    public byte[] step2(byte[] pkA, byte[] proofM1) throws Exception
    {
        if (state != State.STEP_1)
            throw new IllegalStateException("State violation: Session must be in STEP_1 state");

        BigInteger A = new BigInteger(1, pkA);
        BigInteger M1 = new BigInteger(1, proofM1);
        System.out.println("step2: A: " + A);
        System.out.println("step2: M1: " + M1);

        this.A = A;
        this.M1 = M1;

        if (hasTimedOut())
            throw new SRP6Exception("Session timeout", SRP6Exception.CauseType.TIMEOUT);

        if (!SRP6Routines.isValidPublicValue(config.N, A))
            throw new SRP6Exception("Bad client public value 'A'", SRP6Exception.CauseType.BAD_PUBLIC_VALUE);

        if (hashedKeysRoutine != null)
        {
            URoutineContext hashedKeysContext = new URoutineContext(A, B);
            u = hashedKeysRoutine.computeU(config, hashedKeysContext);
        } else
        {
            u = SRP6Routines.computeU(digest, config.N, A, B);
            digest.reset();
        }

        S = SRP6Routines.computeSessionKey(config.N, v, u, A, b);

        BigInteger computedM1;
        if (_clientEvidenceRoutine != null)
        {
            SRP6ClientEvidenceContext ctx = new SRP6ClientEvidenceContext(userID, s, A, B, S);
            computedM1 = _clientEvidenceRoutine.computeClientEvidence(config, ctx);
        } else
        {
            computedM1 = SRP6Routines.computeClientEvidence(digest, A, B, S);
            digest.reset();
        }

        if (!computedM1.equals(M1))
            throw new SRP6Exception("Bad client credentials", SRP6Exception.CauseType.BAD_CREDENTIALS);

        state = State.STEP_2;

        if (_serverEvidenceRoutine != null)
        {

            SRP6ServerEvidenceContext ctx = new SRP6ServerEvidenceContext(A, M1, S);
            M2 = _serverEvidenceRoutine.computeServerEvidence(config, ctx);
        } else
        {
            M2 = SRP6Routines.computeServerEvidence(digest, A, M1, computedM1);
        }

        updateLastActivityTime();
        Encoder encoder = new Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 0x04);
        encoder.add(MessageType.PROOF.getKey(), M2);
        System.out.println("step2 complete");
        return encoder.toByteArray();
    }

    public byte[] step3(byte[] t5) throws Exception
    {
        byte[] K = getK();
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(K, "Pair-Setup-Encrypt-Salt".getBytes(StandardCharsets.UTF_8), "Pair-Setup-Encrypt-Info".getBytes(StandardCharsets.UTF_8)));
        byte[] okm = hkdf_enc_key = new byte[32];
        hkdf.generateBytes(okm, 0, 32);

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

        // decrypt
        
        ChachaDecoder chachaDec = new ChachaDecoder(okm, "PS-Msg05".getBytes(StandardCharsets.UTF_8));
        byte[] plaintext;
        try 
        {
            plaintext = chachaDec.decodeCiphertext(authData, message);
        }
        catch (Exception ex)
        {
            throw new Exception ("pair M5: chacha decode error");
        }
        Decoder d = new Decoder();
        d.decode(plaintext);
        byte[] username = d.getData(MessageType.USERNAME.getKey());
        byte[] ltpk = d.getData(MessageType.PUBLIC_KEY.getKey());
        byte[] proof = d.getData(MessageType.SIGNATURE.getKey());
        // decrypted

        // create user
        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(K, "Pair-Setup-Controller-Sign-Salt".getBytes(StandardCharsets.UTF_8), "Pair-Setup-Controller-Sign-Info".getBytes(StandardCharsets.UTF_8)));

        okm = new byte[32];
        hkdf.generateBytes(okm, 0, 32);

        byte[] completeData = joinBytes(okm, username, ltpk);

        if (!new EdsaVerifier(ltpk).verify(completeData, proof))
        {
            throw new Exception("Invalid signature");
        }

        authInfo.createUser(authInfo.getMac() + new String(username, StandardCharsets.UTF_8), ltpk);

        System.out.println("paired, set discoverable to false");
        Adv.discoverable = false;
        // advertiser.setDiscoverable(false);

        // creating response

        hkdf = new HKDFBytesGenerator(new SHA512Digest());
        hkdf.init(new HKDFParameters(K, "Pair-Setup-Accessory-Sign-Salt".getBytes(StandardCharsets.UTF_8), "Pair-Setup-Accessory-Sign-Info".getBytes(StandardCharsets.UTF_8)));

        okm = new byte[32];
        hkdf.generateBytes(okm, 0, 32);

        EdsaSigner signer = new EdsaSigner(authInfo.getPrivateKey());

        byte[] material = joinBytes(okm, authInfo.getMac().getBytes(StandardCharsets.UTF_8), signer.getPublicKey());

        proof = signer.sign(material);

        Encoder encoder = new Encoder();
        encoder.add(MessageType.USERNAME.getKey(), authInfo.getMac().getBytes(StandardCharsets.UTF_8));
        encoder.add(MessageType.PUBLIC_KEY.getKey(), signer.getPublicKey());
        encoder.add(MessageType.SIGNATURE.getKey(), proof);
        plaintext = encoder.toByteArray();

        ChachaEncoder chachaEnc = new ChachaEncoder(hkdf_enc_key, "PS-Msg06".getBytes(StandardCharsets.UTF_8));
        byte[] ciphertext = chachaEnc.encodeCiphertext(plaintext);

        encoder = new Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 6);
        encoder.add(MessageType.ENCRYPTED_DATA.getKey(), ciphertext);

        return encoder.toByteArray();
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

    private static BigInteger createRandomBigIntegerInRange(final BigInteger min, final BigInteger max, final SecureRandom random)
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
        //MessageDigest digest = getCryptoParams().getMessageDigestInstance();
        BigInteger S = getSessionKey(false);
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
            } catch (NoSuchAlgorithmException e)
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
            } catch (NoSuchAlgorithmException e)
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
