package main;

import java.io.BufferedReader;
import java.io.FileReader;
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

    BigInteger acc_privateKey;
    BigInteger acc_salt;
    String     acc_pin;
    String     acc_pairingID;
    
    public Main() 
    {
        init();
    }
    
    private void init() 
    {
        try (BufferedReader br = new BufferedReader(new FileReader("acc.keys"))) 
        {
            String line;
            int lno = 0;
            while ((line = br.readLine()) != null) 
            {
                ++lno;
                if (lno == 1)
                {
                    acc_salt = new BigInteger (line);
                }
                else if (lno == 2)
                {
                    acc_privateKey = new BigInteger (line);
                }
                else if (lno == 3)
                {
                    acc_pairingID = line;
                }
                else if (lno == 4)
                {
                    acc_pin = line;
                }
            }
        }        
        catch (Exception ex)
        {
            System.out.println (ex.getMessage());
            System.exit(1);
        }
    }
    
    
    public static void main (String args[]) throws Exception
    {
        //System.out.println (System.getProperty("user.dir"));
        Main main = new Main();
        
//        String PIN = "111-11-111";
//        //String mac = "ca:c5:41:bb:c1:5d";
//        String mac = "5f:23:45:67:89:02";
//        
//        BigInteger salt2 = generateSalt();
//        BigInteger sk = new BigInteger(1, generateKey());
//        System.out.println (salt2);
//        System.out.println (sk);
//        
//        
//        byte[] bsalt =  { 102, -50, 15, 74, 82, 15, -63, 109, -114, -72, 6, 108, 120, -1, -61, -19 };
//        BigInteger salt = new BigInteger(bsalt);
//        byte[] privateKey = { 59, 6, -127, 84, 56, 43, 0, 124, 90, 5, -45, -60, 7, -92, -1, 21, 46, -12, 62, 122, 2, 45, -62, -19, 14, 104, -117, 102, 23, -10, 115, 55 };

        Accessory acc = new Thermometer();
        //Accessory acc = new Thermostat();
        //Accessory acc = new Lock();
        
        AccessoryCfg cfg = new AccessoryCfg(acc.getName(), acc.getCategory(), main.acc_pin, main.acc_pairingID, main.acc_salt, main.acc_privateKey);
        
        Adv adv = new Adv(cfg);
        Thread t = new Thread(adv);
        t.start();
        
        HttpServer srv = new HttpServer(9123, cfg, adv, acc);
        srv.start();
    }
}
