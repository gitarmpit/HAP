#ifndef _PAIR_VERIFY_H
#define _PAIR_VERIFY_H

#include "common.h"
#include "http_reply.h"

class PairVerify
{
public:
    static HttpReply stage1(const buf& ios_Curve25519_pk);
    static HttpReply stage2(const buf& message, const buf& authData);
};


#endif
