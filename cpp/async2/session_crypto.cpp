#include "session_crypto.h"

SessionCrypto::SessionCrypto(int16_t _max, int8_t _mac_size) 
: max_encrypted_length (_max), mac_size (_mac_size) 
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

buf& SessionCrypto::encrypt(const buf& in)
{
    return encrypt(in.data, in.length);
}


buf& SessionCrypto::encrypt(const uint8_t* in, int len)
{
    free(write_buf.data);
    int offset = 0;
    write_buf.length = 0;
    write_buf.data = 0;
    while (offset < len)
    {
        int chunk_len = len - offset;
        if (chunk_len > max_encrypted_length)
        {
            chunk_len = max_encrypted_length;
        }
        printf ("session encrypt: processing chunk len = %d\n", chunk_len);
        encryptChunk(&in[offset], chunk_len, write_buf.data, write_buf.length);
        offset += chunk_len;
    }

    return write_buf;
}

void SessionCrypto::encryptChunk(const uint8_t* from, int from_len, uint8_t*& to, int& to_len)
{
    uint8_t additionalData[2];
    additionalData[0] = from_len & 0xff;
    additionalData[1] = (uint8_t)(from_len >> 8);

//    uint8_t* nonce = Pack.longToLittleEndian(outboundBinaryMessageCount++);
//    uint8_t* ciphertext = new ChachaEncoder(write_key, nonce).encodeCiphertext(chunk, additionalData);

    const uint8_t* ciphertext = from;
    int ciphertext_len = from_len + mac_size;  //TODO has to have real ciphertext len (from_len + 16)

    to = (uint8_t*) realloc (to, to_len + ciphertext_len + 2);
    to[to_len] = additionalData[0];
    to[to_len + 1] = additionalData[1];
    memcpy (&to[to_len + 2], ciphertext, from_len);  //TODO has to be ciphertext_len
    //fake auth array of 16 elements
    for (uint8_t i = 0; i < mac_size; ++i)
    {
        to[to_len + from_len + 2 + i] = 255;
    }
    to_len += ciphertext_len + 2;
}


//TODO validate lengths in case of an incomplete message
bool SessionCrypto::decrypt(const buf& from, buf& out)
{
    free (read_buf.data);
    read_buf.data = 0;
    read_buf.length = 0;
    int offset = 0;
    while (offset < from.length)
    {
        int chunk_len = ((from.data[offset + 1] & 0xff) << 8) + (from.data[offset + 0] & 0xff);
        if (offset + chunk_len + 2 + mac_size > from.length || chunk_len > max_encrypted_length) 
        {
            printf ("decrypt: chunk len too long\n");
            return false;
        }
        printf ("session decrypt: processing chunk len = %d\n", chunk_len);
        decryptChunk(&from.data[offset + 2], chunk_len, read_buf.data, read_buf.length);
        offset += chunk_len + 2 + mac_size;
    }

    out = read_buf;
    return true;

}

void SessionCrypto::decryptChunk (const uint8_t* from, int from_len, uint8_t*& to, int& to_len)
{
    //uint8_t mac[mac_size];  //TODO depends on input parameters of our real decrypt code.
    //memcpy (mac, &from[from_len], mac_size); //so may not need this
    for (int i = 0; i < mac_size; ++i) 
    {
        printf ("%d ", from[from_len]);
    }
    printf ("\n");

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
