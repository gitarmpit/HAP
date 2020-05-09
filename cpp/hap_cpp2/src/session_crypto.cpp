#include "session_crypto.h"

#define MAC_SIZE 4

void SessionCrypto::encrypt(const uint8_t* in, int in_len, uint8_t*& out, int& out_len)
{
    free(write_buf);
    int offset = 0;
    int to_len = 0;
    out = 0;
    while (offset < in_len)
    {
        int chunk_len = in_len - offset;
        if (chunk_len > MAX_ENCRYPTED_LENGTH)
        {
            chunk_len = MAX_ENCRYPTED_LENGTH;
        }
        printf ("session encrypt: processing chunk len = %d\n", chunk_len);
        encryptChunk(&in[offset], chunk_len, out, to_len);
        offset += chunk_len;
    }

    out_len = to_len;
    write_buf = out;
}

void SessionCrypto::encryptChunk(const uint8_t* from, int from_len, uint8_t*& to, int& to_len)
{
    uint8_t additionalData[2];
    additionalData[0] = from_len & 0xff;
    additionalData[1] = from_len >> 8;

//    uint8_t* nonce = Pack.longToLittleEndian(outboundBinaryMessageCount++);
//    uint8_t* ciphertext = new ChachaEncoder(write_key, nonce).encodeCiphertext(chunk, additionalData);

    const uint8_t* ciphertext = from;
    int ciphertext_len = from_len + MAC_SIZE;  //TODO has to have real ciphertext len (from_len + 16)

    to = (uint8_t*) realloc (to, to_len + ciphertext_len + 2);
    to[to_len] = additionalData[0];
    to[to_len + 1] = additionalData[1];
    memcpy (&to[to_len + 2], ciphertext, from_len);  //TODO has to be ciphertext_len
    //fake auth array of 16 elements
    for (int i = 0; i < MAC_SIZE; ++i)
    {
        to[to_len + from_len + 2 + i] = i+1;
    }
    to_len += ciphertext_len + 2;
}


void SessionCrypto::decrypt(const uint8_t* from, int from_len, uint8_t*& to, int& to_len)
{
    free (read_buf);
    to_len = 0;
    to = 0;
    int offset = 0;
    while (offset < from_len)
    {
        int chunk_len = ((from[offset + 1] & 0xff) << 8) + (from[offset + 0] & 0xff);
        printf ("session decrypt: processing chunk len = %d\n", chunk_len);
        decryptChunk(&from[offset + 2], chunk_len, to, to_len);
        offset += chunk_len + 2 + MAC_SIZE;
    }
    read_buf = to;

}

void SessionCrypto::decryptChunk (const uint8_t* from, int from_len, uint8_t*& to, int& to_len)
{
    static uint8_t mac[MAC_SIZE];  //TODO depends on input parameters of our real decrypt code.
    memcpy (mac, &from[from_len], MAC_SIZE); //so may not need this

//    uint8_t additionalData[2];
//    additionalData[0] = from_len & 0xff;
//    additionalData[1] = from_len >> 8;

    //    byte[] nonce = Pack.longToLittleEndian(inboundBinaryMessageCount++);
//    plaintextChunk = new ChachaDecoder(read_key, nonce).decodeCiphertext(mac, additionalData, cipherchunk);

    const uint8_t* plaintextChunk = from;
    to = (uint8_t*) realloc (to, to_len + from_len);
    memcpy (&to[to_len], plaintextChunk, from_len); //TODO plainchunk len
    to_len += from_len; //TODO plainchunk len
}

void SessionCrypto::setKeys(uint8_t* write_key, uint8_t* read_key)
{
    this->write_key = write_key;
    this->read_key = read_key;
}
