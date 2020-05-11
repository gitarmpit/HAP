import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Cli 
{

    static byte[] s1;
    static byte[] s2;
    static byte[] s3;
    static byte[] v1;
    static byte[] v2;

    public static void recv(Socket s) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream  is = s.getInputStream(); 

        byte[] buffer = new byte[1024];
        int numRead;
        int totalRead = 0;

        while((numRead = is.read(buffer)) > -1) 
        {
            if (numRead > 0)
            {
                System.out.println ("part: " + numRead);
                 baos.write(buffer, 0, numRead);
                 totalRead += numRead;
            }
            if (is.available() <= 0) 
            {
               break;
            }
        }
        System.out.println ("total: " + totalRead);
    }

    public static void main(String args[]) throws Exception
    {
        s1 = Files.readAllBytes(Paths.get("_p1.bin"));
        s2 = Files.readAllBytes(Paths.get("_p2.bin"));
        s3 = Files.readAllBytes(Paths.get("_p3.bin"));
        v1 = Files.readAllBytes(Paths.get("_v1.bin"));
        v2 = Files.readAllBytes(Paths.get("_v2.bin"));


        Socket s = new Socket(InetAddress.getByName("localhost"), 9123);        
        OutputStream out = s.getOutputStream(); 
        InputStream  in = s.getInputStream(); 
        DataOutputStream dos = new DataOutputStream(out);

        out.write(s1);
        System.out.println ("sent s1\n");
        recv(s);

        out.write(s2);
        System.out.println ("sent s2\n");
        recv(s);

        out.write(s3);
        System.out.println ("sent s3\n");
        recv(s);

        s = new Socket(InetAddress.getByName("localhost"), 9123);        
        out = s.getOutputStream(); 
        in = s.getInputStream(); 
        dos = new DataOutputStream(out);

        out.write(v1);
        System.out.println ("sent v1\n");
        recv(s);

        out.write(v2);
        System.out.println ("sent v2\n");
        recv(s);
        s.close();

    }
}