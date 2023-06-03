package main;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;

import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import srp6.SRP6Routines;

public class Main 
{

    public static BigInteger generateSalt() {
        return new BigInteger(SRP6Routines.generateRandomSalt(16));
    }
    
    public static byte[] generateKey() throws InvalidAlgorithmParameterException {
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("ed25519-sha-512");
        byte[] seed = new byte[spec.getCurve().getField().getb()/8];
        new SecureRandom().nextBytes(seed);
        return seed;
    }
    
    public static void main (String args[]) throws Exception
    {
        
        Accessory acc = new Thermometer();
        
        String PIN = "111-11-111";
        //String mac = "ca:c5:41:bb:c1:5d";
        String mac = "5f:23:45:67:89:05";
        
        //BigInteger salt = generateSalt();
        //byte[] privateKey = generateKey();
        
        
        byte[] bsalt =  { 102, -50, 15, 74, 82, 15, -63, 109, -114, -72, 6, 108, 120, -1, -61, -19 };
        BigInteger salt = new BigInteger(bsalt);
        byte[] privateKey = { 59, 6, -127, 84, 56, 43, 0, 124, 90, 5, -45, -60, 7, -92, -1, 21, 46, -12, 62, 122, 2, 45, -62, -19, 14, 104, -117, 102, 23, -10, 115, 55 };

        AccessoryCfg cfg = new AccessoryCfg(acc.getName(), acc.getCategory(), PIN, mac, salt, privateKey);
        
        Adv adv = new Adv(cfg);
        Thread t = new Thread(adv);
        t.start();
        
        SimpleServer srv = new SimpleServer(9123, cfg, adv, acc);
        srv.start();
    }
}
