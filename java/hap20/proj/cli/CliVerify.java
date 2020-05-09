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
import main.SessionCrypto;
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
    	if (args.length != 2)
    	{
    		System.out.println ("args: id ltpk");
    		System.exit(1);
    	}
    	
        final String acc_pairing_ID = args[0];
        final BigInteger acc_Ed25519_ltpk = new BigInteger (args[1]);
    	
        final String ios_pairing_ID = "EC42D9A0-C0F8-4E36-A548-901DB31F29A8";
        byte[] ios_Ed25519_ltsk = { 59, 6, -127, 84, 56, 43, 0, 124, 90, 5, -45, -60, 7, -92, -1, 21, 46, -12, 62, 122, 2, 45, -62, -19, 14, 104, -117, 102, 23, -10, 115, 55 };
//        String acc_pairing_ID = "5f:23:45:67:89:02";
//        BigInteger acc_Ed25519_ltpk = new BigInteger ("19650185835570259407593278157857435305072627048054473160222274103438508850594");
        
    	
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
        
        SessionCrypto crypto = new SessionCrypto(verifier.getWriteKey(), verifier.getReadKey());
        
        /////////////////////////////////////////////// send get accessories
        msg = "GET /accessories HTTP/1.1\r\n";
        msg += "Host: Thermometer._hap._tcp.local\r\n\r\n";

        byte[] ciphertext = crypto.encrypt(msg.getBytes());
        bos.write(ciphertext);
        bos.flush();
        
        
        //////////////////////////////////////////// recv accessories
        resp = CliAcc.recv(s);
        String acclist = new String(crypto.decrypt(resp));
        System.out.println (acclist);
        acclist.replaceAll("\\s+","");
        

//      AccessoryParser2 p = new AccessoryParser2(acclist);
//      p.scan();
//      String aid = p.getAid();
//      System.out.println ("aid: " + aid);
//      System.out.println ("current state ID: " + p.getCurrentStateId());
//      System.out.println ("target state ID: " + p.getTargetStateId());
//      System.out.println ("current temp ID: " + p.getCurrentTempId());
//      System.out.println ("target temp ID: " + p.getTargetTempId());
//      System.out.println ("display units ID: " + p.getDisplayUnitsId()); //0=C, 1=F
      

      AccessoryParser3 p = new AccessoryParser3(acclist);
      p.scan();
      String aid = p.getAid();
      System.out.println ("current state ID: " + p.getCurrentStateId());
      System.out.println ("target state ID: " + p.getTargetStateId());
      
      
      //////////////////
      msg = "GET /characteristics?id=" + aid + "." + p.getCurrentStateId()+ " HTTP/1.1\r\n";
      msg += "Host: Thermometer._hap._tcp.local\r\n\r\n";
      ciphertext = crypto.encrypt(msg.getBytes());
      bos.write(ciphertext);
      bos.flush();
      
      resp = CliAcc.recv(s);
      System.out.println(new String(crypto.decrypt(resp)));
//
//      
      String val = "{\"characteristics\":[{\"aid\":1,\"iid\":10,\"value\":true}]}";
      val = "{\"characteristics\":[{\"aid\":1,\"iid\":11,\"value\":23.2}]}";
      val = "{\"characteristics\":[{\"aid\":1,\"iid\":11,\"value\":1}]}";
      String iid = p.getTargetStateId();
      val = "{\"characteristics\":[{\"aid\":" + aid + ",\"iid\":" + iid +  ",\"value\":1}]}";
      
      msg = "PUT /characteristics HTTP/1.1\r\n";
      msg += "Host: Thermometer._hap._tcp.local\r\n";
      msg += "Content-Length: " + val.length() + "\r\n";
      msg += "Content-Type: application/hap+json\r\n\r\n";
      msg += val;
      
      ciphertext = crypto.encrypt(msg.getBytes());
      bos.write(ciphertext);
      bos.flush();
      
      resp = CliAcc.recv(s);
      System.out.println(new String(crypto.decrypt(resp)));
      
        
        
        s.close();
    }
    
}
