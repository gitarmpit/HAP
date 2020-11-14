#ifndef _SESSION_CRYPTO_H
#define _SESSION_CRYPTO_H

#define MAX_ENCRYPTED_LENGTH 1024
#define MAC_SIZE 16

#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <memory.h>
#include "common.h"

class SessionCrypto
{
private:

    int inboundBinaryMessageCount;
    int outboundBinaryMessageCount;
    uint8_t* write_key;
    uint8_t* read_key;

    buf read_buf;
    buf write_buf;

    void     encryptChunk (const uint8_t* from, int from_len, uint8_t*& to, int& to_len);
    void     decryptChunk (const uint8_t* from, int from_len, uint8_t*& to, int& to_len);

public:

    SessionCrypto()
    {
        read_buf.data = 0;
        read_buf.length = 0;
        write_buf.data = 0;
        write_buf.length = 0;
        inboundBinaryMessageCount = 0;
        outboundBinaryMessageCount = 0;
        this->write_key = 0;
        this->read_key = 0;

    }
    ~SessionCrypto() {free(read_buf.data); free(write_buf.data);}

    void setKeys(uint8_t* write_key, uint8_t* read_key);

    buf& encrypt(const buf& in);
    buf& decrypt(const buf& in);
};



#endif
