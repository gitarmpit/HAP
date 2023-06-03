package main;

import impl.HomekitAuthInfo;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This is a simple implementation that should never be used in actual production. The mac, salt, and privateKey
 * are being regenerated every time the application is started. The user store is also not persisted. This means pairing
 * needs to be re-done every time the app restarts.
 *
 * @author Andy Lintner
 */
public class MockAuthInfo implements HomekitAuthInfo {
    
    private static final String PIN = "111-11-111";
    
    private final String mac;
    private final BigInteger salt;
    private final byte[] privateKey;
    private final ConcurrentMap<String, byte[]> userKeyMap = new ConcurrentHashMap<>();
    
    public MockAuthInfo(String mac, BigInteger salt, byte[] privateKey) throws InvalidAlgorithmParameterException {
        //System.out.println("Auth info is generated each time the sample application is started. Pairings are not persisted.");
        System.out.println ("private key len: " + privateKey.length);
    System.out.println("The PIN for pairing is "+PIN);
        this.mac = mac;
        this.salt = salt;
        this.privateKey = privateKey;
    }

    @Override
    public String getPin() {
        return PIN;
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

}
