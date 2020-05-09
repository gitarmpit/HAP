#ifndef _SESSION_CRYPTO_H
#define _SESSION_CRYPTO_H

#define MAX_ENCRYPTED_LENGTH 10

#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <memory.h>


class SessionCrypto
{
private:

    int inboundBinaryMessageCount;
    int outboundBinaryMessageCount;
    uint8_t* write_key;
    uint8_t* read_key;

    uint8_t* read_buf;
    uint8_t* write_buf;

    void     encryptChunk (const uint8_t* from, int from_len, uint8_t*& to, int& to_len);
    void     decryptChunk (const uint8_t* from, int from_len, uint8_t*& to, int& to_len);

public:

    SessionCrypto()
    {
        read_buf = 0;
        write_buf = 0;
        inboundBinaryMessageCount = 0;
        outboundBinaryMessageCount = 0;
        this->write_key = 0;
        this->read_key = 0;

    }
    ~SessionCrypto() {free(read_buf); free(write_buf);}

    void setKeys(uint8_t* write_key, uint8_t* read_key);

    void encrypt(const uint8_t* in, int in_len, uint8_t*& out, int& out_len);
    void decrypt(const uint8_t* from, int from_len, uint8_t*& to, int& to_len);
};



#endif
