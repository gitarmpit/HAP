package cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import main.Util;

public class ControllerCfg
{
    final static String keyStore = "ios.pairings";
    private final String name;
    private final int category;
    private final String ios_mac;        //Pairing ID,  pre-generated
    private final BigInteger ios_salt;   //pre-generated
    private final byte[] ios_privateKey; //pre-generated
    private final String ios_pin;       //pre-generated
    
    //iOS device pairing ID -> ios long-term PK  map
    private final ConcurrentMap<String, byte[]> userKeyMap = new ConcurrentHashMap<>();

    private byte[] readKey;  //calculated during handshake
    private byte[] writeKey; //calculated during handshake

    public ControllerCfg(String name, int category, String ios_pin, String ios_mac, BigInteger ios_salt, BigInteger ios_privateKey)
            throws InvalidAlgorithmParameterException
    {
        System.out.println("The PIN for pairing is " + ios_pin);
        this.name = name;
        this.category = category;
        this.ios_pin = ios_pin;
        this.ios_mac = ios_mac;
        this.ios_salt = ios_salt;
        this.ios_privateKey = Util.bigIntegerToUnsignedByteArray(ios_privateKey);
        try 
        {
            File f = new File (keyStore);
            if (!f.exists())
            {
                f.createNewFile();
            }
            BufferedReader br = new BufferedReader(new FileReader(keyStore));
            String line;
            while (true)
            {   
                line = br.readLine();
                if (line == null)
                {
                    break;
                }
                String[] parts = line.split(" : ");
                userKeyMap.put(parts[0], parts[1].getBytes());
            }
            br.close();
        }
        catch (Exception ex)
        {
            
        }
    }
    
    public String getName() 
    {
        return name;
    }
    
    public int getCategory() 
    {
        return category;
    }

    public String getIosPin()
    {
        return ios_pin;
    }

    public String getIosPairingID()
    {
        return ios_mac;
    }

    public BigInteger getIosSalt()
    {
        return ios_salt;
    }

    //Ed25519 long-term secret key, ControllerLTSK
    public byte[] getIsoPrivateKey()
    {
        return ios_privateKey;
    }

    public void createUser(String acc_pairingID, byte[] acc_Ed25519_ltpk)
    {
        if (!userKeyMap.containsKey(acc_pairingID))
        {
            userKeyMap.put(acc_pairingID, acc_Ed25519_ltpk);
            System.out.println("Added pairing for " + acc_pairingID);
            try(PrintWriter output = new PrintWriter(new FileWriter(keyStore,true))) 
            {
                output.println (acc_pairingID + " : " + new String(acc_Ed25519_ltpk));
            } 
            catch (Exception e) {}
        }
        else 
        {
            System.out.println("Pairing already exists for: " + acc_pairingID);
        }
    }

    public void removeAccUser(String acc_pairingID)
    {
        System.out.println("Removing pairing for " + acc_pairingID);
        if (userKeyMap.containsKey(acc_pairingID))
        {
            userKeyMap.remove(acc_pairingID);
            System.out.println("Removed pairing for " + acc_pairingID);
            try(PrintWriter output = new PrintWriter(new FileWriter(keyStore,false))) 
            {
                for (Entry<String, byte[]> e : userKeyMap.entrySet())
                {
                    output.println (e.getKey() + " : " + new String(e.getValue(), StandardCharsets.UTF_8));
                }
                output.close();
            } 
            catch (Exception e) {}
        }
    }

    public byte[] getAccPublicKey(String acc_pairingID)
    {
        return userKeyMap.get(acc_pairingID);
    }

    public void storeReadKey(byte[] key)
    {
        readKey = key;
    }

    public byte[] getReadKey()
    {
        return readKey;
    }

    public void storeWriteKey(byte[] key)
    {
        writeKey = key;
    }

    public byte[] getWriteKey()
    {
        return writeKey;
    }

}
