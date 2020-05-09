package main;


public class PairHelper
{
	public static HttpReply pair1_reply(byte[] acc_SRP_salt, byte[] acc_SRP_public_key)
	{
        TLV_Encoder encoder = new TLV_Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 0x02);
        encoder.add(MessageType.SALT.getKey(), acc_SRP_salt);
        encoder.add(MessageType.PUBLIC_KEY.getKey(), acc_SRP_public_key);
        return HttpReply.generateOK("application/pairing+tlv8", encoder.toByteArray());
	}
	
	public static HttpReply pair2_reply(byte[] acc_SRP_proof)
	{
        TLV_Encoder encoder = new TLV_Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 4);
        encoder.add(MessageType.PROOF.getKey(), acc_SRP_proof);
        return HttpReply.generateOK("application/pairing+tlv8", encoder.toByteArray());
	}
	
	public static byte[] pair3_encode(byte[] acc_pairingID, byte[] acc_Ed25519_ltpk, byte[] acc_proof)
	{
        TLV_Encoder encoder = new TLV_Encoder();
        encoder.add(MessageType.ID.getKey(), acc_pairingID);
        encoder.add(MessageType.PUBLIC_KEY.getKey(), acc_Ed25519_ltpk);
        encoder.add(MessageType.SIGNATURE.getKey(), acc_proof);
        return encoder.toByteArray();
	}
	
	public static HttpReply pair3_reply(byte[] ciphertext) 
	{
        TLV_Encoder encoder = new TLV_Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 6);
        encoder.add(MessageType.ENCRYPTED_DATA.getKey(), ciphertext);

        return HttpReply.generateOK("application/pairing+tlv8", encoder.toByteArray());
	}
	
	
	public static byte[] verify1_encode(byte[] acc_PairingID, byte[] acc_signature)
	{
        TLV_Encoder encoder = new TLV_Encoder();
        encoder.add(MessageType.ID.getKey(), acc_PairingID);
        encoder.add(MessageType.SIGNATURE.getKey(), acc_signature);
        return encoder.toByteArray();
	}
	
	public static HttpReply verify1_reply(byte[] ciphertext, byte[] acc_Curve25519_pk)
	{
        TLV_Encoder encoder = new TLV_Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 2);
        encoder.add(MessageType.ENCRYPTED_DATA.getKey(), ciphertext);
        encoder.add(MessageType.PUBLIC_KEY.getKey(), acc_Curve25519_pk);

        return HttpReply.generateOK("application/pairing+tlv8", encoder.toByteArray());
	}
	
	
	public static HttpReply verify2_reply()
	{
        TLV_Encoder encoder = new TLV_Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) 4);
        return HttpReply.generateOK("application/pairing+tlv8", encoder.toByteArray());
	}
	
}
