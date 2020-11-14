#ifndef _PAIR_SETUP_H
#define _PAIR_SETUP_H
#include "http_reply.h"
#include "pairing_helper.h"

class PairSetup
{
public:
    static HttpReply step1();
    static HttpReply step3(const buf& message, const buf& authData);
    static HttpReply step2(const buf& ios_SRP_public_key, const buf& ios_SRP_proof);
};


#endif
