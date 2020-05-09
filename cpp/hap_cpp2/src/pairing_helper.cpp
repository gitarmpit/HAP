#include "pairing_helper.h"
#include "tlv.h"

HttpReply PairingHelper::pair1_reply(const buf& acc_SRP_salt, const buf& acc_SRP_public_key)
{
    TLV_Encoder encoder;
    encoder.add(STATE, (short) 0x02);
    encoder.add(SALT, acc_SRP_salt);
    encoder.add(PUBLIC_KEY, acc_SRP_public_key);
    return HttpReply::generateOK("application/pairing+tlv8", encoder.get());

}

HttpReply PairingHelper::pair2_reply(const buf& acc_SRP_proof)
{
    TLV_Encoder encoder;
    encoder.add(STATE, (short) 4);
    encoder.add(PROOF, acc_SRP_proof);
    return HttpReply::generateOK("application/pairing+tlv8", encoder.get());
}


const buf& PairingHelper::pair3_encode(const char*  acc_pairingID, int id_sz, const buf& acc_Ed25519_ltpk, const buf& acc_proof)
{
    TLV_Encoder encoder;
    encoder.add(ID, (uint8_t*)acc_pairingID, id_sz);
    encoder.add(PUBLIC_KEY, acc_Ed25519_ltpk);
    encoder.add(SIGNATURE, acc_proof);
    return encoder.get();
}

HttpReply PairingHelper::pair3_reply(const buf& ciphertext)
{
    TLV_Encoder encoder;
    encoder.add(STATE, (short) 6);
    encoder.add(ENCRYPTED_DATA, ciphertext);
    printf ("pair3: encoded length: %d\n", encoder.get().length);

    return HttpReply::generateOK("application/pairing+tlv8", encoder.get());
}

const buf& PairingHelper::verify1_encode(const char* acc_pairingID, int id_sz, const buf& acc_signature)
{
    TLV_Encoder encoder;
    encoder.add(ID, (uint8_t*)acc_pairingID, id_sz);
    encoder.add(SIGNATURE, acc_signature);
    return encoder.get();
}


HttpReply PairingHelper::verify1_reply(const buf& ciphertext, const buf& acc_Curve25519_pk)
{
    TLV_Encoder encoder;
    encoder.add(STATE, (short) 2);
    encoder.add(ENCRYPTED_DATA, ciphertext);
    encoder.add(PUBLIC_KEY, acc_Curve25519_pk);

    return HttpReply::generateOK("application/pairing+tlv8", encoder.get());
}


HttpReply PairingHelper::verify2_reply()
{
    TLV_Encoder encoder;
    encoder.add(STATE, (short) 4);
    return HttpReply::generateOK("application/pairing+tlv8", encoder.get());
}
