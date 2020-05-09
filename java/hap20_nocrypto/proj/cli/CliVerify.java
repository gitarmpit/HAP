package cli;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;

import main.HttpRequest;
import main.TLV_Decoder;


public class CliVerify
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
        String acc_pairing_ID = "5f:23:45:67:89:02";
        BigInteger acc_Ed25519_ltpk = new BigInteger ("50800926979663088354671403557872387264250469005851133548636894099613091395563");
    	
        Socket s = new Socket(InetAddress.getByName("localhost"), 9123);        
        OutputStream out = s.getOutputStream(); 
        BufferedOutputStream bos = new BufferedOutputStream(new DataOutputStream(out));
        
        
        //start verify 
        s = new Socket(InetAddress.getByName("localhost"), 9123);        
        out = s.getOutputStream(); 
        bos = new BufferedOutputStream(new DataOutputStream(out));
        
        CliPairVerify verifier = new CliPairVerify(ios_pairing_ID.getBytes(), 
                ios_Ed25519_ltsk, acc_pairing_ID.getBytes(), acc_Ed25519_ltpk.toByteArray());
        byte[] m1 = verifier.stage_M1();
        
        ////////////////////////////////////////////////  send M1
        String msg = "POST /pair-verify HTTP/1.1\r\n";
        msg += "Host: Thermometer._hap._tcp.local\r\n";
        msg += "Content-Length: " + m1.length + "\r\n";
        msg += "Content-Type: application/pairing+tlv8\r\n\r\n";
        bos.write(msg.getBytes());
        bos.write(m1);
        bos.flush();
        
        ////////////////////////////////////////// recv M2
        byte[] resp = CliVerify.recv(s); 
        System.out.println ("received from server: " + resp.length);
        
        HttpRequest req = new HttpRequest(resp);
        byte[] body = req.getBody();
        TLV_Decoder decoder = new TLV_Decoder();
        decoder.decode(body);
        if (decoder.getStage() != 2)
        {
        	System.out.printf("verify stage: expected: 2, got: %d", decoder.getStage());
        	s.close();
        	System.exit(1);
        }
        
        byte[] acc_Curve25519_pk =  decoder.getData(3);
        
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
        
        byte[] m3 = verifier.stage_M3(message, authData, acc_Curve25519_pk);
        msg = "POST /pair-verify HTTP/1.1\r\n";
        msg += "Host: Thermometer._hap._tcp.local\r\n";
        msg += "Content-Length: " + m3.length + "\r\n";
        msg += "Content-Type: application/pairing+tlv8\r\n\r\n";
        bos.write(msg.getBytes());
        bos.write(m3);
        bos.flush();

        ////////////////////////////////////////// recv M4
        resp = CliVerify.recv(s); 
        System.out.println ("received from server: " + resp.length);
        req = new HttpRequest(resp);
        body = req.getBody();
        decoder = new TLV_Decoder();
        decoder.decode(body);
        if (decoder.getStage() != 4)
        {
        	System.out.printf("verify stage: expected: 4, got: %d", decoder.getStage());
        }
        else
        {
            System.out.println ("verification complete!");
        }
        
        s.close();
    }
    
}
