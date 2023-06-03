package main;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import bouncy.Pack;
import crypto.ChachaDecoder;
import crypto.ChachaEncoder;

public class SimpleServer
{
    int port;
    ServerSocket serverSocket;
    int inboundBinaryMessageCount = 0;
    int outboundBinaryMessageCount = 0;
    PairSetup pairSetup;
    PairVerify pairVerify;
    AuthInfo authInfo;

    boolean upgraded;

    public SimpleServer(int port, AuthInfo authInfo) throws Exception
    {
        this.port = port;

        pairSetup = new PairSetup(authInfo);
        pairVerify = new PairVerify(authInfo);
        this.authInfo = authInfo;
        upgraded = false;

        try
        {
            serverSocket = new ServerSocket(port);
        } catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            throw ex;
        }
    }

    class Session implements Runnable
    {
        Socket clientSocket;

        public Session(Socket clientSocket)
        {
            this.clientSocket = clientSocket;
        }

        public void run()
        {
            try
            {
                int numRead;

                while (true)
                {
                    byte[] buffer = new byte[1024];
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    InputStream is = clientSocket.getInputStream();
                    if (is.available() <= 0)
                    {
                        continue;
                    }

                    System.out.println("incoming req");

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

                    SimpleRequest req;

                    if (upgraded)
                    {
                        byte[] benc = baos.toByteArray();

                        int len = (benc[1] << 8) + benc[0];

                        System.out.println("encrypted message: total: " + benc.length + ", enc: " + len);

                        byte[] msg = new byte[len + 16];
                        for (int i = 0; i < len + 16; ++i)
                        {
                            msg[i] = benc[i + 2];
                        }
                        
                        byte[] plaintext = decrypt(msg);
                        String str = new String(plaintext);
                        System.out.println("encrypted request: " + str);
                    } else
                    {
                        req = new SimpleRequest(baos.toByteArray());
                        System.out.println("==  incoming req  ===============");
                        System.out.println(baos.toString());
                        System.out.printf("\nBody bytes: %d\n", req.body.length);
                        for (int i = 0; i < req.body.length; ++i)
                        {
                            System.out.printf("%2x ", req.body[i]);
                        }

                        System.out.println();
                        byte[] b = processRequest(req);

                        OutputStream stream = clientSocket.getOutputStream();
                        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream)));
                        out.write("HTTP/1.1 200 OK\r\n");
                        out.write("Content-type: application/pairing+tlv8\r\n");
                        out.write("Connection: keep-alive\r\n");
                        out.write("Content-Length: " + b.length + "\r\n\r\n");
                        out.flush();
                        stream.write(b);
                        stream.flush();

                        System.out.printf("\nreply: %d bytes\n", b.length);
                        for (int i = 0; i < b.length; ++i)
                        {
                            System.out.printf("%2x ", b[i]);
                        }
                        System.out.println("\nresponse sent\n");

                    }

                }
            } catch (Exception ex)
            {
                System.out.println(ex.getMessage());
            } finally
            {
                try
                {
                    clientSocket.close();
                } catch (Exception ex)
                {

                }
            }
        }
    }

    private byte[] decrypt(byte[] msg)
    {
        byte[] mac = new byte[16];
        byte[] ciphertext = new byte[msg.length - 16];
        System.arraycopy(msg, 0, ciphertext, 0, msg.length - 16);
        System.arraycopy(msg, msg.length - 16, mac, 0, 16);
        int msglen = msg.length - 16;
        byte[] additionalData = new byte[2];
        additionalData[0] = (byte)(msglen & 0xff);
        additionalData[1] = (byte)(msglen >> 8);
        System.out.println("additional data: ");
        for (int i = 0; i < additionalData.length; ++i)
        {
            System.out.printf("%x ", additionalData[i]);

        }
        System.out.println("key size: " + authInfo.getReadKey().length);

        try
        {
            byte[] nonce = Pack.longToLittleEndian(inboundBinaryMessageCount++);
            return new ChachaDecoder(authInfo.getReadKey(), nonce).decodeCiphertext(mac, additionalData, ciphertext);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private byte[] encrypt (byte[] plaintext) throws Exception
    {
        byte[] nonce = Pack.longToLittleEndian(outboundBinaryMessageCount++);
        byte[] additionalData = new byte[2];
        additionalData[0] = (byte)(plaintext.length & 0xff);
        additionalData[1] = (byte)(plaintext.length >> 8);
        byte[] ciphertext = new ChachaEncoder(authInfo.getWriteKey(), nonce).encodeCiphertext(plaintext, additionalData);            
        byte[] msg = new byte[ciphertext.length + 2];
        msg[0] = additionalData[0];
        msg[0] = additionalData[1];
        System.arraycopy(ciphertext, 0, msg, 2, ciphertext.length);

        
        return ciphertext;
    }

    private byte[] processUpgradedRequest (byte[] req) throws Exception 
    {
        
    }
    
    private byte[] processRequest(SimpleRequest req) throws Exception
    {
        byte[] out = new byte[0];
        if (req.getUri() != null && req.getUri().equals("/pair-setup") || req.getUri().equals("/pair-verify"))
        {
            boolean verify = false;
            if (req.getUri().equals("/pair-verify"))
            {
                verify = true;
            }

            Decoder d = new Decoder();
            d.decode(req.body);
            int stage = d.getStage();
            try
            {
                if (!verify && stage == 1)
                {
                    System.out.println("invoking setup M1");
                    out = pairSetup.step1();

                } else if (!verify && stage == 3)
                {
                    byte[] t3 = d.getData(3); // pk
                    byte[] t4 = d.getData(4); // proof
                    System.out.printf("invoking setup M3: t3 len=%d, t4 len=%d\n", t3.length, t4.length);
                    out = pairSetup.step2(t3, t4);
                } else if (!verify && stage == 5)
                {
                    byte[] t5 = d.getData(5); // encoded
                    System.out.printf("invoking setup M5, t5 len=%d \n", t5.length);
                    out = pairSetup.step3(t5);
                } else if (verify && stage == 1)
                {
                    byte[] t3 = d.getData(3); // pk
                    System.out.printf("invoking verify M1, t3 len=%d \n", t3.length);
                    out = pairVerify.stage1(t3);
                } else if (verify && stage == 3)
                {
                    byte[] t5 = d.getData(5); // encoded
                    System.out.printf("invoking verify M3, t5 len=%d \n", t5.length);
                    out = pairVerify.stage2(t5);
                    upgraded = true;
                } else
                {
                    throw new RuntimeException("Wrong stage: " + stage);
                }
            } catch (Exception ex)
            {
                System.out.println("Error: " + ex.getMessage());
            }
        } 
        else if (req.getUri() != null && req.getUri().equals("/accessories"))
        {
            String plainText = "HTTP/1.1 200 OK\r\nContent-type: application/hap+json\r\nContent-Length: 2617\r\nConnection: keep-alive\r\n\r\n";
            plainText += "{\"accessories\":[{\"aid\":1,\"services\":[{\"iid\":1,\"type\":\"0000003E-0000-1000-8000-0026BB765291\",\"characteristics\":[{\"iid\":2,\"type\":\"00000023-0000-1000-8000-0026BB765291\",\"perms\":[\"pr\"],\"format\":\"string\",\"events\":false,\"bonjour\":false,\"description\":\"Name of the accessory\",\"value\":\"Test Bridge\",\"maxLen\":255},{\"iid\":3,\"type\":\"00000020-0000-1000-8000-0026BB765291\",\"perms\":[\"pr\"],\"format\":\"string\",\"events\":false,\"bonjour\":false,\"description\":\"The name of the manufacturer\",\"value\":\"TestBridge, Inc.\",\"maxLen\":255},{\"iid\":4,\"type\":\"00000021-0000-1000-8000-0026BB765291\",\"perms\":[\"pr\"],\"format\":\"string\",\"events\":false,\"bonjour\":false,\"description\":\"The name of the model\",\"value\":\"G6\",\"maxLen\":255},{\"iid\":5,\"type\":\"00000030-0000-1000-8000-0026BB765291\",\"perms\":[\"pr\"],\"format\":\"string\",\"events\":false,\"bonjour\":false,\"description\":\"The serial number of the accessory\",\"value\":\"222abe234\",\"maxLen\":255},{\"iid\":6,\"type\":\"00000014-0000-1000-8000-0026BB765291\",\"perms\":[\"pw\"],\"format\":\"bool\",\"events\":false,\"bonjour\":false,\"description\":\"Identifies the accessory via a physical action on the accessory\"}]}]},{\"aid\":2,\"services\":[{\"iid\":1,\"type\":\"0000003E-0000-1000-8000-0026BB765291\",\"characteristics\":[{\"iid\":2,\"type\":\"00000023-0000-1000-8000-0026BB765291\",\"perms\":[\"pr\"],\"format\":\"string\",\"events\":false,\"bonjour\":false,\"description\":\"Name of the accessory\",\"value\":\"Test Lightbulb\",\"maxLen\":255},{\"iid\":3,\"type\":\"00000020-0000-1000-8000-0026BB765291\",\"perms\":[\"pr\"],\"format\":\"string\",\"events\":false,\"bonjour\":false,\"description\":\"The name of the manufacturer\",\"value\":\"none\",\"maxLen\":255},{\"iid\":4,\"type\":\"00000021-0000-1000-8000-0026BB765291\",\"perms\":[\"pr\"],\"format\":\"string\",\"events\":false,\"bonjour\":false,\"description\":\"The name of the model\",\"value\":\"none\",\"maxLen\":255},{\"iid\":5,\"type\":\"00000030-0000-1000-8000-0026BB765291\",\"perms\":[\"pr\"],\"format\":\"string\",\"events\":false,\"bonjour\":false,\"description\":\"The serial number of the accessory\",\"value\":\"none\",\"maxLen\":255},{\"iid\":6,\"type\":\"00000014-0000-1000-8000-0026BB765291\",\"perms\":[\"pw\"],\"format\":\"bool\",\"events\":false,\"bonjour\":false,\"description\":\"Identifies the accessory via a physical action on the accessory\"}]},{\"iid\":7,\"type\":\"00000043-0000-1000-8000-0026BB765291\",\"characteristics\":[{\"iid\":8,\"type\":\"00000023-0000-1000-8000-0026BB765291\",\"perms\":[\"pr\"],\"format\":\"string\",\"events\":false,\"bonjour\":false,\"description\":\"Name of the accessory\",\"value\":\"Test Lightbulb\",\"maxLen\":255},{\"iid\":9,\"type\":\"00000025-0000-1000-8000-0026BB765291\",\"perms\":[\"pw\",\"pr\",\"ev\"],\"format\":\"bool\",\"events\":false,\"bonjour\":false,\"description\":\"Turn on and off\",\"value\":false}]}]}]}";
            return encrypt(plainText.getBytes());
        }
        else
        {
            System.out.println("Uri unimplemented: " + req.getUri());
        }

        return out;
    }

    public void start()
    {
        while (true)
        {
            try
            {
                Session sess = new Session(serverSocket.accept());
                System.out.println("New connection");
                Thread t = new Thread(sess);
                t.start();
            } catch (Exception ex)
            {
            }
        }
    }

}
