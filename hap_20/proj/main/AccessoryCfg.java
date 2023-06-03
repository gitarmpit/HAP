package main;

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

public class AccessoryCfg
{
    final static String keyStore = "acc.pairings";
    private final String name;
    private final int category;
    private final String acc_mac;        //Pairing ID,  pre-generated
    private final BigInteger acc_salt;   //pre-generated
    private final byte[] acc_privateKey; //pre-generated
    private final String acc_pin;       //pre-generated
    
    //iOS device pairing ID -> ios long-term PK  map
    private final ConcurrentMap<String, byte[]> userKeyMap = new ConcurrentHashMap<>();

    private byte[] readKey;  //calculated during handshake
    private byte[] writeKey; //calculated during handshake

    public AccessoryCfg(String name, int category, String acc_pin, String acc_mac, BigInteger acc_salt, BigInteger acc_privateKey)
            throws InvalidAlgorithmParameterException
    {
        System.out.println("The PIN for pairing is " + acc_pin);
        this.name = name;
        this.category = category;
        this.acc_pin = acc_pin;
        this.acc_mac = acc_mac;
        this.acc_salt = acc_salt;
        this.acc_privateKey = Util.bigIntegerToUnsignedByteArray(acc_privateKey);
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

    public String getAccPin()
    {
        return acc_pin;
    }

    public String getAccPairingID()
    {
        return acc_mac;
    }

    public BigInteger getAccSalt()
    {
        return acc_salt;
    }

    //Ed25519 long-term secret key, AccessoryLTSK
    public byte[] getAccPrivateKey()
    {
        return acc_privateKey;
    }

    public void createUser(String iosUsername, byte[] ios_Ed25519_ltpk)
    {
        if (!userKeyMap.containsKey(iosUsername))
        {
            userKeyMap.put(iosUsername, ios_Ed25519_ltpk);
            System.out.println("Added pairing for " + iosUsername);
            try(PrintWriter output = new PrintWriter(new FileWriter(keyStore,true))) 
            {
                output.println (iosUsername + " : " + new String(ios_Ed25519_ltpk));
            } 
            catch (Exception e) {}
        }
        else 
        {
            System.out.println("Pairing already exists for: " + iosUsername);
        }
    }

    public void removeIosUser(String iosUsername)
    {
        System.out.println("Removing pairing for " + iosUsername);
        if (userKeyMap.containsKey(iosUsername))
        {
            userKeyMap.remove(iosUsername);
            System.out.println("Removed pairing for " + iosUsername);
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

    public byte[] getIosPublicKey(String iosUsername)
    {
        return userKeyMap.get(iosUsername);
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
