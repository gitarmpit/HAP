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

    const int16_t max_encrypted_length;
    const int8_t mac_size;

    buf read_buf;
    buf write_buf;

    void     encryptChunk (const uint8_t* from, int from_len, uint8_t*& to, int& to_len);
    void     decryptChunk (const uint8_t* from, int from_len, uint8_t*& to, int& to_len);

    SessionCrypto& operator=(const SessionCrypto& other);

public:
    SessionCrypto(int16_t _max = MAX_ENCRYPTED_LENGTH, int8_t _mac_size = MAC_SIZE);
    ~SessionCrypto() {free(read_buf.data); free(write_buf.data);}

    void setKeys(uint8_t* write_key, uint8_t* read_key);

    buf& encrypt(const buf& in);
    buf& encrypt(const uint8_t* buf, int len);
    bool decrypt(const buf& in, buf& out);
};



#endif
