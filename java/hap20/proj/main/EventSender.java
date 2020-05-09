package main;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class EventSender implements Runnable
{
    Accessory acc;
    Socket clientSocket;
    SessionCrypto crypto;
    boolean running = true;
    
    public EventSender(Accessory acc, SessionCrypto crypto, Socket clientSocket)
    {
        this.acc = acc;
        this.crypto = crypto;
        this.clientSocket = clientSocket;
        System.out.println ("client port: " + ((InetSocketAddress)clientSocket.getRemoteSocketAddress()).getPort());
    }

    public void stop() 
    {
        running = false;
        System.out.println ("stopping event handler");
    }
    
    public static void sendEvent(Socket clientSocket, String body, SessionCrypto crypto) throws Exception
    {
        String plainText = "EVENT/1.0 200 OK\r\nContent-type: application/hap+json\r\nContent-Length: "
                + body.length() + "\r\nConnection: keep-alive\r\n\r\n";
        plainText += body;
        System.out.println(plainText);
        System.out.println();
        byte[] encryptedResp = crypto.encrypt(plainText.getBytes());
        OutputStream stream = clientSocket.getOutputStream();
        stream.write(encryptedResp);
        stream.flush();
    }
    
    public void run()
    {
        while (running)
        {
            try
            {
                Thread.sleep(10000);
                if (running)
                {
                    String body = acc.processEvent("");
                    EventSender.sendEvent(clientSocket, body, crypto);
                }
            }
            catch (Exception ex)
            {
            	System.out.println ("event sender exception: " + ex.getMessage());
            	//session.stop();
            	try
                {
	                clientSocket.close();
                }
                catch (IOException e)
                {
                }
            	break;
            }
        }
        System.out.println ("event sender stopped");
    }
}
