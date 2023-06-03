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
		upgraded = false;
		running = true;
	}
	
//	public void stop() 
//	{
//		running = false;
//	}

	private byte[] waitForIncomingReq() throws Exception
	{
		InputStream is = clientSocket.getInputStream();
		while (true)
		{
			if (!running)
			{
				throw new Exception("session stopping");
			}
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
				System.out.println("numread: " + numRead);
			}
			if (is.available() <= 0)
			{
				break;
			}
		}

		return baos.toByteArray();
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
					raw_in = sessionCrypto.decrypt(raw_in);
					encrypt = true;
				}

				HttpRequest req = new HttpRequest(raw_in);

				// for (int i = 0; i < raw_in.length; ++i)
				// {
				// System.out.printf("%c", (char)raw_in[i]);
				// }

				System.out.println();
				System.out.println("Content length: " + req.body.length);
				System.out.println();
				System.out.println(req.getHeaders());

				HttpReply reply = processRequest(req);

				System.out.println("=== Reply ====");

				byte[] msg = reply.msg;

				// for (int i = 0; i < msg.length; ++i)
				// {
				// if (upgraded || i < (msg.length - reply.bodyLength))
				// {
				// System.out.printf("%c", msg[i]);
				// }
				// else
				// {
				// System.out.printf("0x%x ", msg[i]);
				// }
				// }

				System.out.printf("\nPlaintext reply: %d bytes\n\n", msg.length);
				System.out.println(new String(reply.hdr) + "\n");

				if (encrypt)
				{
					msg = sessionCrypto.encrypt(reply.msg);
					System.out.printf("\nEncrypted reply: %d bytes\n\n", msg.length);
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
		}
		catch (Exception ex)
		{
			System.out.println("Run loop exception:  " + ex.getMessage());
		}
		finally
		{
			System.out.println("Session ended");
			try
			{
				clientSocket.close();
			}
			catch (Exception ex)
			{
				System.out.println(ex.getMessage());
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

		System.out.println("uri: " + uri);

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
			// {"characteristics":[{"aid":2,"iid":9,"ev":true}]} //event on for
			// 2.9

			String in = new String(req.body);
			if (in.contains("\"ev\""))
			{
				// send once immediately (don't have to do it?)
				String body = acc.processEvent(in);
				EventSender.sendEvent(clientSocket, body, sessionCrypto);

				// schedule to send periodically
				eventSender = new EventSender(acc, sessionCrypto, clientSocket);
				Thread t = new Thread(eventSender);
				t.start();
			}
			else
			{
				acc.setValue(in);
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
			byte[] body = processPairings(req); // only remove expected here
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

		TLV_Decoder d = new TLV_Decoder();
		d.decode(body);
		int stage = d.getStage();
		try
		{
			if (stage == 1) // received M1
			{
				System.out.println("invoking setup M2");
				// start M2
				reply = pairSetup.step1();
			}
			else if (stage == 3) // received M3
			{
				byte[] t3 = d.getData(3); // pk
				byte[] t4 = d.getData(4); // proof
				System.out.printf("invoking setup M4: t3 len=%d, t4 len=%d\n", t3.length, t4.length);
				// start M4
				reply = pairSetup.step2(t3, t4);
			}
			else if (stage == 5) // received M5
			{
				byte[] t5 = d.getData(5); // encoded
				System.out.printf("invoking setup M6, t5 len=%d \n", t5.length);
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
				// start M6
				reply = pairSetup.step3(message, authData);
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
		TLV_Decoder d = new TLV_Decoder();
		d.decode(body);
		int stage = d.getStage();
		try
		{
			if (stage == 1) // received M1
			{
				byte[] t3 = d.getData(3); // iOS device's Curve25519 public key
				System.out.printf("invoking verify M2, t3 len=%d \n", t3.length);
				// start M2
				reply = pairVerify.stage1(t3);
			}
			else if (stage == 3) // received M3
			{
				byte[] t5 = d.getData(5); // encoded
				System.out.printf("invoking verify M4, t5 len=%d \n", t5.length);
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
				// start M4
				reply = pairVerify.stage2(message, authData);
				upgraded = true;
				sessionCrypto = new SessionCrypto(cfg.getWriteKey(), cfg.getReadKey());
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

	// remove pairing when deleting accessory from controller
	private byte[] processPairings(HttpRequest req) throws Exception
	{
		TLV_Decoder d = new TLV_Decoder();
		d.decode(req.body);
		int pairingMethod = d.getPairingMethod();
		String userName = null;
		if (d.getData(1) != null)
		{
			userName = new String(d.getData(1), StandardCharsets.UTF_8);
		}

		System.out.printf("/pairings: stage = %d, method = %d, %s\n", d.getStage(), pairingMethod, userName);

		// remove pairings
		if (pairingMethod == 4 && userName != null)
		{
			cfg.removeIosUser(userName);
			adv.setDiscoverable(true);

			if (eventSender != null)
			{
				eventSender.stop();
				eventSender = null;
			}

			running = false;
		}

		TLV_Encoder encoder = new TLV_Encoder();
		encoder.add(MessageType.STATE.getKey(), (short) 2);
		return encoder.toByteArray();

	}

}