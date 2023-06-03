package main;

import java.net.ServerSocket;

public class SimpleServer
{
    int port;
    ServerSocket serverSocket;
    AccessoryCfg cfg;
    Adv adv;
    Accessory acc;

    public SimpleServer(int port, AccessoryCfg cfg, Adv adv, Accessory acc) throws Exception
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
                HttpSession sess = new HttpSession(serverSocket.accept(), cfg, adv, acc);
                System.out.println("New connection");
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
