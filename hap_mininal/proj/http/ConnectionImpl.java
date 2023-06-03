package http;

import impl.HomekitAuthInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;


import pairing.UpgradeResponse;
import bouncy.Pack;
import crypto.ChachaDecoder;
import crypto.ChachaEncoder;

public class ConnectionImpl implements HomekitClientConnection {

	private final HttpSession httpSession;
	private LengthPrefixedByteArrayProcessor binaryProcessor;
	private int inboundBinaryMessageCount = 0;
	private int outboundBinaryMessageCount = 0;
	private byte[] readKey;  //read from controller
	private byte[] writeKey; //send to controller
	private boolean isUpgraded = false;
	
	
	public ConnectionImpl(HomekitAuthInfo authInfo)
	{
		httpSession = new HttpSession(authInfo, this);
	}

	@Override
	public synchronized HttpResponse handleRequest(HttpRequest request) throws IOException 
	{
		HttpResponse response = isUpgraded ? 
				httpSession.handleAuthenticatedRequest(request) : httpSession.handleRequest(request);

		if (response instanceof UpgradeResponse) {
			isUpgraded = true;
			readKey = ((UpgradeResponse) response).getReadKey().array();
			writeKey = ((UpgradeResponse) response).getWriteKey().array();
		}
		System.out.println(response.getStatusCode()+" "+request.getUri());
		return response;
	}
	
	@Override
	public byte[] decryptRequest(byte[] ciphertext) {
		if (!isUpgraded) {
			throw new RuntimeException("Cannot handle binary before connection is upgraded");
		}
		if (binaryProcessor == null) {
			binaryProcessor = new LengthPrefixedByteArrayProcessor();
		}
		Collection<byte[]> res = binaryProcessor.handle(ciphertext);
		if (res.isEmpty()) {
			return new byte[0];
		} else {
			try(ByteArrayOutputStream decrypted = new ByteArrayOutputStream()) {
				res.stream().map(msg -> decrypt(msg))
						.forEach(bytes -> {
							try {
								decrypted.write(bytes);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						});
				return decrypted.toByteArray();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	

	@Override
	public byte[] encryptResponse(byte[] response) throws IOException {
		int offset=0;
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			while(offset < response.length) {
				short length = (short) Math.min(response.length - offset, 0x400);
				byte[] lengthBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
						.putShort(length).array();
				baos.write(lengthBytes);
				
				byte[] nonce = Pack.longToLittleEndian(outboundBinaryMessageCount++);
				byte[] plaintext;
				if (response.length == length) {
					plaintext = response;
				} else {
					plaintext = new byte[length];
					System.arraycopy(response, offset, plaintext, 0, length);
				}
				offset += length;
				baos.write(new ChachaEncoder(writeKey, nonce).encodeCiphertext(plaintext, lengthBytes));
			}
			return baos.toByteArray();
		}
	}
	
	private byte[] decrypt(byte[] msg) {
		byte[] mac = new byte[16];
		byte[] ciphertext = new byte[msg.length - 16];
		System.arraycopy(msg, 0, ciphertext, 0, msg.length - 16);
		System.arraycopy(msg, msg.length - 16, mac, 0, 16);
		byte[] additionalData = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) (msg.length - 16)).array();
		try {
			byte[] nonce = Pack.longToLittleEndian(inboundBinaryMessageCount++);
			return new ChachaDecoder(readKey, nonce)
				.decodeCiphertext(mac, additionalData, ciphertext);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
	}

	@Override
	public void outOfBand(HttpResponse message) {
	    
	    System.out.println ("out of band");
//		outOfBandMessageCallback.accept(message);
	}


}
