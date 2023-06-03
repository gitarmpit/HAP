package main;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HttpSession implements Runnable
{
    Socket clientSocket;
    boolean upgraded;
    PairSetup pairSetup;
    PairVerify pairVerify;
    AccessoryCfg cfg;
    SessionCrypto sessionCrypto;
    Adv adv;
    Accessory acc;
    EventSender eventSender;
    boolean running;
    
    public HttpSession(Socket clientSocket, AccessoryCfg cfg, Adv adv, Accessory acc)
    {
        this.clientSocket = clientSocket;
        this.cfg = cfg;
        pairSetup = new PairSetup(cfg, adv);
        pairVerify = new PairVerify(cfg);
        this.cfg = cfg;
        this.adv = adv;
        this.acc = acc;
        sessionCrypto = new SessionCrypto(cfg);
        upgraded = false;
        running = true;
    }
    

    private byte[] waitForIncomingReq() throws Exception
    {
        InputStream is = clientSocket.getInputStream();
        while (true)
        {
            if (is.available() > 0)
            {
                break;
            }
            Thread.sleep(1);
        }

        System.out.println("==  incoming req  ===============");
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
    
    private byte[] decryptRequest(byte[] raw_in)
    {
        int len = ((raw_in[1] & 0xff) << 8) + (raw_in[0] & 0xff);

        // System.out.println("encrypted message: total: " +
        // benc.length + ", enc: " + len);

        // for (int i = 0; i < benc.length; ++i)
        // {
        // System.out.printf ("0x%x ", benc[i]);
        // }

        byte[] msg = new byte[len + 16];
        for (int i = 0; i < len + 16; ++i)
        {
            msg[i] = raw_in[i + 2];
        }

        return sessionCrypto.decrypt(msg);
    }
    
    public void run()
    {
        try
        {
            while (true)
            {
                byte[] raw_in = waitForIncomingReq();
                boolean encrypt = false;
                
                if (upgraded)
                {
                    raw_in = decryptRequest(raw_in);
                    encrypt = true;
                }

                HttpRequest req = new HttpRequest(raw_in);
                
                for (int i = 0; i < raw_in.length; ++i)
                {
                    System.out.printf("%c", (char)raw_in[i]);
                }
                System.out.println();
                System.out.println ("req len: " + req.body.length);
                System.out.println();

                HttpReply reply = processRequest(req);

                System.out.println("=== Reply ====");
                
                byte[] msg = reply.msg;

                for (int i = 0; i < msg.length; ++i)
                {
                    if (upgraded || i < (msg.length - reply.bodyLength)) 
                    {
                        System.out.printf("%c", msg[i]);
                    }
                    else 
                    {
                        System.out.printf("0x%x ", msg[i]);
                    }
                }
                
                System.out.println();

                if (encrypt)
                {
                    msg = sessionCrypto.encrypt(reply.msg);
                }

                OutputStream stream = clientSocket.getOutputStream();
                stream.write(msg);
                stream.flush();

                if (!running)
                {
                    break;
                }

            }
            clientSocket.close();
            System.out.println("Session ended");
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
        }
        finally
        {
            try
            {
                clientSocket.close();
            }
            catch (Exception ex)
            {

            }
        }

    }

    private HttpReply processRequest(HttpRequest req) throws Exception
    {
        String uri = req.getUri();
        String httpMethod = req.getMethod();

        if (uri == null || httpMethod == null)
        {
            System.out.println("malformed request, no uri or method");
            return HttpReply.generate400();
        }
        
        System.out.println ("uri: " + uri);
        
        HttpReply reply;

        if (uri.equals("/pair-setup"))
        {
            reply = processPairRequest(req.body);
        }
        else if (uri.equals("/pair-verify"))
        {
            reply = processVerifyRequest(req.body);
        }
        else if (uri.equals("/identify")) 
        {
            System.out.println("identify");
            reply = HttpReply.generate204("application/hap+json");
        }
        else if (uri.equals("/accessories"))
        {
            System.out.println("accessory request");
            String accessoryList = acc.getAccessoryList();
            reply = HttpReply.generateOK("application/hap+json", accessoryList.getBytes());
        }
        else if (uri.equals("/characteristics") && httpMethod.equals("PUT"))
        {
            System.out.println("characteristics put");
            // example input:
            // {"characteristics":[{"aid":1,"iid":6,"value":true}]} //identify
            // {"characteristics":[{"aid":2,"iid":9,"value":1}]} //on
            // {"characteristics":[{"aid":2,"iid":9,"ev":true}]} //event on for 2.9

            String in = new String(req.body);
            if (in.contains("\"ev\""))
            {
                //send once immediately (don't have to do it?) 
                String body = acc.processEvent(in);
                EventSender.sendEvent(clientSocket, body, sessionCrypto);
                
                //schedule to send periodically
                eventSender = new EventSender(acc, sessionCrypto, clientSocket);
                Thread t = new Thread(eventSender);
                t.start();
            }
            else 
            {
                acc.setValue(uri);
            }

            reply = HttpReply.generate204("application/hap+json");
        }
        else if (uri.startsWith("/characteristics") && httpMethod.equals("GET"))
        {
            System.out.println("characteristics get");
            // input: ask the value of 2.9
            // GET /characteristics?id=2.9 HTTP/1.1
            // no body or content length
            
            // return:
            // {"characteristics":[{"value":false,"aid":2,"iid":9}]}
            
            String body = acc.getValue(uri);
            reply = HttpReply.generateOK("application/hap+json", body.getBytes());
        }
        else if (uri.equals("/pairings"))
        {
            byte[] body = processPairings(req); //only remove expected here
            reply = HttpReply.generateOK("application/hap+json", body);
        }
        else
        {
            System.out.println("Uri unimplemented: " + req.getUri());
            reply = HttpReply.generate400();
        }

        return reply;
    }
    
    private HttpReply processPairRequest(byte[] body)
    {
        HttpReply reply;

        Decoder d = new Decoder();
        d.decode(body);
        int stage = d.getStage();
        try
        {
            if (stage == 1)
            {
                System.out.println("invoking setup M2");
                reply = pairSetup.step1();
            }
            else if (stage == 3)
            {
                byte[] t3 = d.getData(3); // pk
                byte[] t4 = d.getData(4); // proof
                System.out.printf("invoking setup M4: t3 len=%d, t4 len=%d\n", t3.length, t4.length);
                reply = pairSetup.step2(t3, t4);
            }
            else if (stage == 5)
            {
                byte[] t5 = d.getData(5); // encoded
                System.out.printf("invoking setup M6, t5 len=%d \n", t5.length);
                reply = pairSetup.step3(t5);
                running = false;
            }
            else
            {
                System.out.println("Wrong setup/verify stage: " + stage);
                reply = HttpReply.generate400();
            }
        }
        catch (Exception ex)
        {
            System.out.println("Pairing Error: " + ex.getMessage());
            reply = HttpReply.generate400();
        }
        return reply;
    }
    
    private HttpReply processVerifyRequest(byte[] body)
    {
        HttpReply reply;
        Decoder d = new Decoder();
        d.decode(body);
        int stage = d.getStage();
        try
        {
            if (stage == 1)
            {
                byte[] t3 = d.getData(3); // pk
                System.out.printf("invoking verify M2, t3 len=%d \n", t3.length);
                reply = pairVerify.stage1(t3);
            }
            else if (stage == 3)
            {
                byte[] t5 = d.getData(5); // encoded
                System.out.printf("invoking verify M4, t5 len=%d \n", t5.length);
                upgraded = true;
                reply  = pairVerify.stage2(t5);
            }
            else
            {
                System.out.println("Wrong setup/verify stage: " + stage);
                reply = HttpReply.generate400();
            }
        }
        catch (Exception ex)
        {
            System.out.println("Verify error: " + ex.getMessage());
            reply = HttpReply.generate400();
        }
        
        return reply;
        
    }
    
    
    private byte[] processPairings(HttpRequest req) throws Exception 
    {
        Decoder d = new Decoder();
        d.decode(req.body);
        int pairingMethod = d.getPairingMethod();
        String userName = null;
        if (d.getData(1) != null)
        {
            userName = new String(d.getData(1), StandardCharsets.UTF_8);
        }

        System.out.printf("/pairings: stage = %d, method = %d, %s\n", d.getStage(), pairingMethod, userName);

        //remove pairings
        if (pairingMethod == 4 && userName != null)
        {
            cfg.removeUser(userName);
            adv.setDiscoverable(true);
            
            if (eventSender != null)
            {
                eventSender.stop();
                eventSender = null;
            }
            
            running = false;
        }        

        Encoder encoder = new Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 2);
        return encoder.toByteArray();

    }

}