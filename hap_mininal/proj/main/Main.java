package main;

import impl.HomekitUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import adv.Adv;
import http.HomekitClientConnectionFactoryImpl;
import http.HomekitHttpServer;
import http.SimpleServer;


public class Main 
{

    public static void main (String args[]) throws Exception
    {
        //String mac = HomekitUtils.generateMac();
        //BigInteger salt = HomekitUtils.generateSalt();
        //byte[] privateKey = HomekitUtils.generateKey();

        String mac = "03:c5:41:bb:c1:5d";
        
        //BigInteger salt = generateSalt();
        //byte[] privateKey = generateKey();
        
        
        byte[] bsalt =  { 102, -50, 15, 74, 82, 15, -63, 109, -114, -72, 6, 108, 120, -1, -61, -19 };
        BigInteger salt = new BigInteger(bsalt);
        byte[] privateKey = { 59, 6, -127, 84, 56, 43, 0, 124, 90, 5, -45, -60, 7, -92, -1, 21, 46, -12, 62, 122, 2, 45, -62, -19, 14, 104, -117, 102, 23, -10, 115, 55 };


//        ByteBuffer bb = ByteBuffer.wrap(privateKey);
//        byte[] check = bb.array();
    
        
        Adv adv = new Adv(mac);
        Thread t = new Thread(adv);
        t.start();
        
        HomekitHttpServer srv = new HomekitHttpServer(9123, 3);
        srv.start(new HomekitClientConnectionFactoryImpl(new MockAuthInfo(mac, salt, privateKey)));
        
        
        
        
        //SimpleServer srv = new SimpleServer(9124);
        //srv.start();
    }
}
