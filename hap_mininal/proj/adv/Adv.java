package adv;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.io.ByteArrayOutputStream;
import java.io.*;

public class Adv implements Runnable
{
    MulticastSocket s;
    InetAddress group;
    ByteArrayOutputStream out;
    byte[] header = { 0, 0, (byte)0x84, 0, 0, 0, 0, 1, 0, 0, 0, 0 };
    String hostName = "volo.local";  //A
    String serviceName = "Test Bridge";
    String hap = "_hap._tcp.local";  
    String fullServiceName;  //SRV name, TXT name, PTR data
    String txtProps;

    public static boolean discoverable = true;
    
    byte[] ip = { (byte)192, (byte)168, 1, (byte)109 };
    int port = 9123;

    byte[] buf2 = null;

    int ttl = 60;  //sec
    
    public Adv(String mac)
    {
     try 
     {
         s = new MulticastSocket(5353);
         s.setTimeToLive(255);
         group = InetAddress.getByName("224.0.0.251");
         fullServiceName = serviceName + "." + hap;
         txtProps = "md=" + serviceName + ".c#=1.s#=1.ff=0.ci=1.id=" + mac + ".sf=1";
         //System.out.println (txtProps);
         out = new ByteArrayOutputStream();
     }
     catch (Exception e) 
     {
     }
    }
    

    void storeName (String name, ByteArrayOutputStream out) 
    {
        String names[] = name.split("\\.");
        for (int i = 0; i < names.length; ++i) 
        {
            //System.out.println (names[i]);
            out.write(names[i].length());
            out.write(names[i].getBytes(), 0, names[i].length());
        }
    }

    void writeShort (int v, ByteArrayOutputStream out)
    {
        v &= 0xffff;
        out.write (v>>8);
        out.write (v&0xff);
    } 

    void writeInt (int v, ByteArrayOutputStream out)
    {
        writeShort (v>>16, out);
        writeShort (v&0xffff, out);
    } 


    public void run() 
    {
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        header[7] = 1;
        out1.write(header, 0, header.length);
        storeName (hostName, out1);
        out1.write(0);

        out1.write(0);
        out1.write(1);
        out1.write(0x80);
        out1.write(1);

        writeInt(ttl, out1);
        writeShort(4, out1); //reclen
        out1.write(ip, 0, ip.length);

        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        header[7] = 3;
        out2.write(header, 0, header.length);

        
        //PTR
        storeName (hap, out2);
        out2.write(0);

        out2.write(0);
        out2.write(12);  //PTR
        out2.write(0);
        out2.write(1);

        writeInt(ttl, out2);
        writeShort(fullServiceName.length() + 2, out2); //reclen
        storeName (fullServiceName, out2);
        out2.write(0);
        

        //SRV
        storeName (fullServiceName, out2);
        out2.write(0);

        out2.write(0);
        out2.write(33);  //SRV
        out2.write(0x80);
        out2.write(1);

        writeInt(ttl, out2);
        writeShort(hostName.length() + 2 + 6, out2); //reclen
        writeShort(1, out2); //prio
        writeShort(1, out2); //weight
        writeShort(port, out2); //port
        storeName (hostName, out2);
        out2.write(0);

        //TXT
        storeName (fullServiceName, out2);
        out2.write(0);

        out2.write(0);
        out2.write(16);  //TXT
        out2.write(0x80);
        out2.write(1);

        writeInt(ttl, out2);
        writeShort(txtProps.length() + 1, out2); //reclen
        storeName (txtProps, out2);



        byte[] buf1 = out1.toByteArray();
        
        buf2 = out2.toByteArray();
        
        for (int i = 0; i < buf1.length; ++i) 
        {
            //System.out.printf ("%x ", buf1[i]);
        }

        //System.out.println();
        
        for (int i = 0; i < buf2.length; ++i) 
        {
            //System.out.printf ("%x ", buf2[i]);
        }



        if (!discoverable)
        {
            buf2 [ buf2.length - 1 ] = (byte)'0';        
        }


        DatagramPacket p1 = new DatagramPacket(buf1, buf1.length, group, 5353);
        DatagramPacket p2 = new DatagramPacket(buf2, buf2.length, group, 5353);
        
        //DatagramPacket p1 = new DatagramPacket(data, data.length, group, 5353);
        //DatagramPacket p2 = new DatagramPacket(data2, data2.length, group, 5353);

        int i = 0;
        while(true) 
        {
            try 
            {
                //buf2 [ buf2.length - 1 ] =  ((i++ % 2) == 0)  ? (byte) '0' : (byte) '1';
                s.send(p1);
                s.send(p2);
                Thread.sleep(1000);
                s.send(p1);
                s.send(p2);
                Thread.sleep(1000);
                s.send(p1);
                s.send(p2);
                Thread.sleep(10000);
            }
            catch (Exception e) 
            {
                System.out.println (e.getMessage());
            }
        }
    }

//    public void setDiscoverable(boolean isDisco) 
//    {
//    }

 }
