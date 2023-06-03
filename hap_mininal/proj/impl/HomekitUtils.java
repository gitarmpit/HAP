package impl;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import srp6.SRP6Routines;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;

public class HomekitUtils {
    
    private static volatile SecureRandom secureRandom;

    public static BigInteger generateSalt() {
        return new BigInteger(SRP6Routines.generateRandomSalt(16));
    }
    
    public static byte[] generateKey() throws InvalidAlgorithmParameterException {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
        byte[] seed = new byte[spec.getCurve().getField().getb()/8];
        getSecureRandom().nextBytes(seed);
        return seed;
    }
    
    public static String generateMac() {
        int byte1 = ((getSecureRandom().nextInt(255) + 1) | 2) & 0xFE; //Unicast locally administered MAC;
        return Integer.toHexString(byte1) + ":" + Stream.generate(() -> getSecureRandom().nextInt(255) + 1)
            .limit(5)
            .map(i -> Integer.toHexString(i))
            .collect(Collectors.joining(":"));
    }
    
    private static SecureRandom getSecureRandom() {
        if (secureRandom == null) {
            synchronized(HomekitUtils.class) {
                if (secureRandom == null) {
                    secureRandom = new SecureRandom();
                }
            }
        }
        return secureRandom;
    }
}
