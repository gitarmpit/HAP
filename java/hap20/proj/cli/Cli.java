package cli;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import main.HttpRequest;
import main.MessageType;
import main.SessionCrypto;
import main.TLV_Decoder;
import main.TLV_Encoder;


public class Cli
{
    static byte[] recv(Socket s) throws Exception
    {
        InputStream is = s.getInputStream();
        while (true)
        {
            if (is.available() > 0)
            {
                break;
            }
            Thread.sleep(1);
        }

        System.out.println("==  msg from srv  ===============");
        int numRead;
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while ((numRead = is.read(buffer)) > -1)
        {
            if (numRead > 0)
            {
                baos.write(buffer, 0, numRead);
            }
            if (is.available() <= 0)
            {
                break;
            }
        }

        return baos.toByteArray();
    }
    
    public static void main(String args[]) throws Exception
    {
        final String ios_pairing_ID = "EC42D9A0-C0F8-4E36-A548-901DB31F29A8";
        byte[] ios_Ed25519_ltsk = { 59, 6, -127, 84, 56, 43, 0, 124, 90, 5, -45, -60, 7, -92, -1, 21, 46, -12, 62, 122, 2, 45, -62, -19, 14, 104, -117, 102, 23, -10, 115, 55 };
        
        
        Socket s = new Socket(InetAddress.getByName("localhost"), 9123);        
        OutputStream out = s.getOutputStream(); 
        BufferedOutputStream bos = new BufferedOutputStream(new DataOutputStream(out));
        
        System.out.println("Start pairing");
        
        ////////////////////////////////////////////////  send M1
        String msg = "POST /pair-setup HTTP/1.1\r\n";
        msg += "Host: Thermometer._hap._tcp.local\r\n";
        msg += "Content-Length: 6\r\n";
        msg += "Content-Type: application/pairing+tlv8\r\n\r\n";
        
        TLV_Encoder encoder = new TLV_Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 0x1);
        encoder.add(MessageType.METHOD.getKey(), (short) 0x0);
        
        bos.write(msg.getBytes());
        bos.write(encoder.toByteArray());
        bos.flush();

        
        ////////////////////////////////////////// recv M2
        byte[] resp = Cli.recv(s); 
        
        
        HttpRequest req = new HttpRequest(resp);
        byte[] body = req.getBody();
        
        //M3: iOS Device -> Accessory -- `SRP Verify Request'
        TLV_Decoder decoder = new TLV_Decoder();
        decoder.decode(body);
        byte[] acc_salt = decoder.getData(2);
        if (decoder.getStage() != 2)
        {
            System.out.printf("pairing stage: expected: 2, got: %", decoder.getStage());
            System.exit(1);
        }
        
        byte[] acc_pk = decoder.getData(3);
        System.out.println("\nM2: acc pk size:" + acc_pk.length);
        
        CliPairSetup pairSetup = new CliPairSetup(ios_pairing_ID.getBytes(), ios_Ed25519_ltsk, acc_salt);
        byte[] m4 = pairSetup.step_M3(acc_pk);
        
        ////////////////////////////////////////////////  send M3
        msg = "POST /pair-setup HTTP/1.1\r\n";
        msg += "Host: Thermometer._hap._tcp.local\r\n";
        msg += "Content-Length: " + m4.length + "\r\n";
        msg += "Content-Type: application/pairing+tlv8\r\n\r\n";
        
        bos.write(msg.getBytes());
        bos.write(m4);
        bos.flush();

        ////////////////////////////////////////// recv M4
        resp = Cli.recv(s); 

        System.out.println ("received from server: " + resp.length);
        
        req = new HttpRequest(resp);
        body = req.getBody();
        decoder = new TLV_Decoder();
        decoder.decode(body);
        if (decoder.getStage() != 4)
        {
            System.out.printf("pairing stage: expected: 4, got: %", decoder.getStage());
            System.exit(1);
        }
        
        
        byte[] acc_SRP_proof = decoder.getData(4);
        
        byte[] m5 = pairSetup.step_M5(acc_SRP_proof);
        ////////////////////////////////////////////////  send M5
        msg = "POST /pair-setup HTTP/1.1\r\n";
        msg += "Host: Thermometer._hap._tcp.local\r\n";
        msg += "Content-Length: " + m5.length + "\r\n";
        msg += "Content-Type: application/pairing+tlv8\r\n\r\n";
        
        bos.write(msg.getBytes());
        bos.write(m5);
        bos.flush();
        
        ////////////////////////////////////////// recv M6
        resp = Cli.recv(s); 
        System.out.println ("received from server: " + resp.length);
        
        req = new HttpRequest(resp);
        body = req.getBody();
        decoder = new TLV_Decoder();
        decoder.decode(body);
        if (decoder.getStage() != 6)
        {
            System.out.printf("pairing stage: expected: 6, got: %d", decoder.getStage());
            System.exit(1);
        }
        
        byte[] t5 = decoder.getData(5); //encrypted message
        
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
        
        //start M6
        if (!pairSetup.step_M6_verify(message, authData)) 
        {
            System.out.println();
        }
        
        s.close();
        
        byte[] acc_pairing_ID = pairSetup.getAccPairingID();
        byte[] acc_Ed25519_ltpk = pairSetup.getAccLTPK();
        
        System.out.println ("Acc pairing ID: " + new String(acc_pairing_ID));
        System.out.println ("Acc LPTK: " + new BigInteger(1, acc_Ed25519_ltpk));
        
        System.out.println("Pairing complete, start verification");
        
        
        //start verify 
        s = new Socket(InetAddress.getByName("localhost"), 9123);        
        out = s.getOutputStream(); 
        bos = new BufferedOutputStream(new DataOutputStream(out));
        
        CliPairVerify verifier = new CliPairVerify(ios_pairing_ID.getBytes(), ios_Ed25519_ltsk, acc_pairing_ID, acc_Ed25519_ltpk);
        byte[] m1 = verifier.stage_M1();
        
        ////////////////////////////////////////////////  send M1
        msg = "POST /pair-verify HTTP/1.1\r\n";
        msg += "Host: Thermometer._hap._tcp.local\r\n";
        msg += "Content-Length: " + m1.length + "\r\n";
        msg += "Content-Type: application/pairing+tlv8\r\n\r\n";
        bos.write(msg.getBytes());
        bos.write(m1);
        bos.flush();
        
        ////////////////////////////////////////// recv M2
        resp = Cli.recv(s); 
        System.out.println ("received from server: " + resp.length);
        
        req = new HttpRequest(resp);
        body = req.getBody();
        decoder = new TLV_Decoder();
        decoder.decode(body);
        if (decoder.getStage() != 2)
        {
            System.out.printf("verify stage: expected: 2, got: %d", decoder.getStage());
            System.exit(1);
        }
        
        byte[] acc_Curve25519_pk =  decoder.getData(3);
        
        t5 = decoder.getData(5); //encrypted message
        authData = new byte[16];
        message = new byte[t5.length - 16];
        for (int i = 0; i < 16; ++i)
        {
            authData[i] = t5[message.length + i];
        }
        for (int i = 0; i < message.length; ++i)
        {
            message[i] = t5[i];
        }
        
        byte[] m3 = verifier.stage_M3(message, authData, acc_Curve25519_pk);
        msg = "POST /pair-verify HTTP/1.1\r\n";
        msg += "Host: Thermometer._hap._tcp.local\r\n";
        msg += "Content-Length: " + m3.length + "\r\n";
        msg += "Content-Type: application/pairing+tlv8\r\n\r\n";
        bos.write(msg.getBytes());
        bos.write(m3);
        bos.flush();

        ////////////////////////////////////////// recv M4
        resp = Cli.recv(s); 
        System.out.println ("received from server: " + resp.length);
        req = new HttpRequest(resp);
        body = req.getBody();
        decoder = new TLV_Decoder();
        decoder.decode(body);
        if (decoder.getStage() != 4)
        {
            System.out.printf("verify stage: expected: 4, got: %d", decoder.getStage());
            System.exit(1);
        }
        
        System.out.println ("verification complete!");
        
        SessionCrypto crypto = new SessionCrypto(verifier.getWriteKey(), verifier.getReadKey());
        
        /////////////////////////////////////////////// send get accessories
        msg = "GET /accessories HTTP/1.1\r\n";
        msg += "Host: Thermometer._hap._tcp.local\r\n\r\n";

        byte[] ciphertext = crypto.encrypt(msg.getBytes());
        bos.write(ciphertext);
        bos.flush();
        
        
        //////////////////////////////////////////// recv accessories
        resp = Cli.recv(s);
        String acclist = new String(crypto.decrypt(resp));
        System.out.println (acclist);
        acclist.replaceAll("\\s+","");
        AccessoryParser parser = new AccessoryParser(acclist);
        parser.scan();
        String tempId = parser.getTempId();
        if (tempId == null)
        {
            System.out.println ("No current temp property ID in the acccessory list");
            System.exit(1);
        }
        
        ////subscribe to event 
        String ev = "{\"characteristics\":[{\"aid\":1,\"iid\":9,\"ev\":true]}";
        msg = "PUT /characteristics HTTP/1.1\r\n";
        msg += "Host: Thermometer._hap._tcp.local\r\n";
        msg += "Content-Length: " + ev.length() + "\r\n";
        msg += "Content-Type: application/hap+json\r\n\r\n";
        msg += ev;
        
        ciphertext = crypto.encrypt(msg.getBytes());
        bos.write(ciphertext);
        bos.flush();
        
        System.out.println ("client port: " + ((InetSocketAddress)s.getLocalSocketAddress()).getPort());
        
        int i = 0;
        while (i == 0) 
        {
            resp = Cli.recv(s);
            String charact = new String(crypto.decrypt(resp));
            System.out.println (charact);
        }

        while (true)
        {
            /////////////////////////////////////////////// query temp
            msg = "GET /characteristics?id=" + tempId + "HTTP/1.1\r\n";
            msg += "Host: Thermometer._hap._tcp.local\r\n";            
            ciphertext = crypto.encrypt(msg.getBytes());
            bos.write(ciphertext);
            bos.flush();

            //////////////////////////////////////////// recv accessories
            resp = Cli.recv(s);
            String charact = new String(crypto.decrypt(resp));
            //System.out.println (charact);
            charact.replaceAll("\\s+","");
            int pos = charact.indexOf("value\":");
            String tempC = charact.substring(pos + 7, charact.indexOf(',', pos + 7));
            System.out.println (tempC);
            Thread.sleep(3000);
        }

        //s.close();
    }
    
}
