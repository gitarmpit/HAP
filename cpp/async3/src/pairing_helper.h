#ifndef _PAIRING_HELPER_H
#define _PAIRING_HELPER_H
#include "http_reply.h"
#include <stdint.h>

class PairingHelper 
{
public:
    static HttpReply  pair1_reply(const buf& acc_SRP_salt, const buf& acc_SRP_public_key);
    static HttpReply  pair2_reply(const buf& acc_SRP_proof);
    //static const buf& pair3_encode(const char* acc_pairingID, int id_sz, const buf& acc_Ed25519_ltpk, const buf& acc_proof);
    static HttpReply  pair3_reply(const buf& ciphertext);
    //static const buf& verify1_encode(const char* acc_pairingID, int id_sz, const buf& acc_signature);
    static HttpReply  verify1_reply(const buf& ciphertext, const buf& acc_Curve25519_pk);
    static HttpReply  verify2_reply();
};


#endif
