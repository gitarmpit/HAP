package cli;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MdnsListener
{
    private static final String MDNS_GROUP_ADDRESS = "224.0.0.251";
    
    public static void main (String[]  args) throws Exception
    {
        MulticastSocket s = new MulticastSocket(5353);
        InetAddress group  = InetAddress.getByName(MDNS_GROUP_ADDRESS);
        s.joinGroup(group);
        
        
        while (true)
        {
            try 
            {
                byte[] recvBuf = new byte[15000];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                s.receive(packet);
                System.out.println ("received bytes: " + packet.getLength());
                byte[] data = packet.getData();
//              ParsePacket pp = new  ParsePacket();
//              pp.parsePacket(data);
                String str = new String (data);
                //if (str.contains("Thermo"))
                {
                for (int i = 0; i < packet.getLength(); ++i)
                {
                    System.out.printf ("%c", (char)data[i]);
                }
                System.out.println ();
                for (int i = 0; i < packet.getLength(); ++i)
                {
                    System.out.printf ("%d ", (int)(data[i] & 0xff));
                }
                System.out.println ("\n====================\n");
                }
                Thread.sleep(1000);
            }
            catch (Exception ex)
            {
                System.out.println (ex.getMessage());
            }
        }
        
        
    }
    
    static class ParsePacket 
    {
        int offset;
        String name = "";
        
        void parsePacket (byte[] data)
        {
            int cnt = data[7];
            
            for (int i = 0; i < cnt ; ++i)
            {
                offset = 12;
                parseName(data);
            }
            
        }
        
        void parseName (byte[] data)
        {
            while (true)
            {
                int len = data[offset++];
                if (len == 0)
                {
                    break;
                }

                if (name.length() > 0)
                {
                    name += ".";
                }
                
                for (int i = 0; i < len; ++i)
                {
                    name += (char)data[offset++];
                }
            }
        }
        
    }
    
    
}
