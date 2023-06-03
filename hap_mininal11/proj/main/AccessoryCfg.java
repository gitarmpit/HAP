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
    final static String keyStore = "homekit.keys";
    private final String name;
    private final int category;
    private final String mac;
    private final BigInteger salt;
    private final byte[] privateKey;
    private final ConcurrentMap<String, byte[]> userKeyMap = new ConcurrentHashMap<>();
    private final String pin;

    private byte[] readKey;
    private byte[] writeKey;

    public AccessoryCfg(String name, int category, String pin, String mac, BigInteger salt, byte[] privateKey)
            throws InvalidAlgorithmParameterException
    {
        System.out.println("private key len: " + privateKey.length);
        System.out.println("The PIN for pairing is " + pin);
        this.name = name;
        this.category = category;
        this.pin = pin;
        this.mac = mac;
        this.salt = salt;
        this.privateKey = privateKey;
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

    public String getPin()
    {
        return pin;
    }

    public String getMac()
    {
        return mac;
    }

    public BigInteger getSalt()
    {
        return salt;
    }

    public byte[] getPrivateKey()
    {
        return privateKey;
    }

    public void createUser(String username, byte[] publicKey)
    {
        if (!userKeyMap.containsKey(username))
        {
            userKeyMap.put(username, publicKey);
            System.out.println("Added pairing for " + username);
            try(PrintWriter output = new PrintWriter(new FileWriter(keyStore,true))) 
            {
                output.println (username + " : " + new String(publicKey));
            } 
            catch (Exception e) {}
        }
        else 
        {
            System.out.println("Pairing already exists for: " + username);
        }
    }

    public void removeUser(String username)
    {
        System.out.println("Removing pairing for " + username);
        if (userKeyMap.containsKey(username))
        {
            userKeyMap.remove(username);
            System.out.println("Removed pairing for " + username);
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

    public byte[] getUserPublicKey(String username)
    {
        return userKeyMap.get(username);
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
