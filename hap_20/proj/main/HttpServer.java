package main;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer
{
    int port;
    ServerSocket serverSocket;
    AccessoryCfg cfg;
    Adv adv;
    Accessory acc;

    public HttpServer(int port, AccessoryCfg cfg, Adv adv, Accessory acc) throws Exception
    {
        this.port = port;
        this.cfg = cfg;
        this.adv = adv;
        this.acc = acc;

        try
        {
            serverSocket = new ServerSocket(port);
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
            throw ex;
        }
    }

    public void start()
    {
        while (true)
        {
            try
            {
            	Socket clientSocket = serverSocket.accept();
            	String remoteIP = ((InetSocketAddress)clientSocket.getRemoteSocketAddress()).getAddress().getHostAddress();
                HttpSession sess = new HttpSession(clientSocket, cfg, adv, acc);
                System.out.println("New connection from: " + remoteIP);
                Thread t = new Thread(sess);
                t.start();
            }
            catch (Exception ex)
            {
                System.out.println (ex.getMessage());
                break;
            }
        }
    }


}
