package http;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleServer 
{
	
	int port; 
	ServerSocket serverSocket;
	
	public SimpleServer(int port) throws Exception
	{
		this.port = port;
		try 
		{
			serverSocket = new ServerSocket(port);
		}
		catch (Exception ex)
		{
			System.out.println (ex.getMessage());
			throw ex;
		}
	}

	public void start() 
	{
		Socket clientSocket = null;
		String response = "<TITLE>Exemple</TITLE><P>Ceci est une page d'exemple.</P>";
		try 
		{
			while (true)
			{
				clientSocket = serverSocket.accept();
				BufferedReader in = null;
				BufferedWriter out = null;
				try 
				{
					in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
					
					///////////////////////
//					String s;
//					while ((s = in.readLine()) != null) 
//					{
//						System.out.println(s);
//						if (s.isEmpty()) {
//							break;
//						}
//					}
					//////////////////////
					
			        ByteArrayOutputStream baos = new ByteArrayOutputStream();
			        InputStream  is = clientSocket.getInputStream(); 

			        byte[] reply = new byte[1024];
			        byte[] buffer = new byte[1024];
			        int numRead;

			        while((numRead = is.read(buffer)) > -1) {
			        	if (numRead > 0)
			        	{
			        		baos.write(buffer, 0, numRead);
			        	}
			        	if (is.available() <= 0) 
			        	{
			        		break;
			        	}
			        }
			        
			        System.out.println(baos.toString());
					
					

					out.write("HTTP/1.0 200 OK\r\n");
					out.write("Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n");
					out.write("Server: Apache/0.8.4\r\n");
					out.write("Content-Type: text/html\r\n");
					//out.write("Content-Length: 59\r\n");
					out.write("Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n");
					out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
					out.write("\r\n");
					//out.write("<TITLE>Exemple</TITLE>");
					//out.write("<P>Ceci est une page d'exemple.</P>");
					out.write(response);
					System.out.println ("done :" + response.length());
					
				}
				catch (Exception ex)
				{
					System.out.println (ex.getMessage());
				}
				finally 
				{
					if (out != null)
						out.close();
					if (in != null)
						in.close();
					clientSocket.close();
				}
			}
		}
		catch (Exception ex)
		{
		}
	}
	
}
