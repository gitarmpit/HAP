package main;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MockAuthInfo implements AuthInfo {
    
    
    private final String mac;
    private final BigInteger salt;
    private final byte[] privateKey;
    private final ConcurrentMap<String, byte[]> userKeyMap = new ConcurrentHashMap<>();
    private final String pin;
    
    private byte[] readKey;
    private byte[] writeKey;
    
    public MockAuthInfo(String pin, String mac, BigInteger salt, byte[] privateKey) throws InvalidAlgorithmParameterException 
    {
        System.out.println ("private key len: " + privateKey.length);
        System.out.println("The PIN for pairing is " + pin);
        this.pin = pin;
        this.mac = mac;
        this.salt = salt;
        this.privateKey = privateKey;
    }

    @Override
    public String getPin() {
        return pin;
    }

    @Override
    public String getMac() {
        return mac;
    }

    @Override
    public BigInteger getSalt() {
        return salt;
    }

    @Override
    public byte[] getPrivateKey() {
        return privateKey;
    }

    @Override
    public void createUser(String username, byte[] publicKey) {
        userKeyMap.putIfAbsent(username, publicKey);
        System.out.println("Added pairing for "+username);
    }

    @Override
    public void removeUser(String username) {
        userKeyMap.remove(username);
        System.out.println("Removed pairing for "+username);
    }

    @Override
    public byte[] getUserPublicKey(String username) {
        return userKeyMap.get(username);
    }

    @Override
    public void storeReadKey(byte[] key)
    {
        readKey = key;
    }

    @Override
    public byte[] getReadKey()
    {
        return readKey;
    }

    @Override
    public void storeWriteKey(byte[] key)
    {
        writeKey = key;
    }

    @Override
    public byte[] getWriteKey()
    {
        return writeKey;
    }

}
