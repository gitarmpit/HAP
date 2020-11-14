#ifndef _TLV_H
#define _TLV_H
#include <stdint.h>
#include <stdio.h>
#include <memory.h>
#include <stdlib.h>
#include "common.h"

#define TLV_MAX_LENGTH 255

class TLV_Encoder 
{
private:
    buf out;
    int max_length; 

    void addChunk (uint8_t type, uint8_t len, uint8_t* from);

public:

    TLV_Encoder(int max_length = TLV_MAX_LENGTH);

    const buf& get() const { return out; }

    void add (MESSAGE_TYPE type, uint8_t b);
    bool add (MESSAGE_TYPE type, uint8_t* array, int size);
    bool add (MESSAGE_TYPE type, const buf& from);
    ~TLV_Encoder() { free (out.data); }

};


typedef struct _tlv_map
{
    uint8_t type;
    uint8_t* val;
    uint8_t rec_len;
    _tlv_map* next;
} tlv_map;


class TLV_Decoder
{
private:
    uint8_t method; //such as 4=delete pairing
    uint8_t stage;
    int     decode_offset;

    tlv_map*  map;

    void init();
    void reset();

    void add_fragment (uint8_t type, uint8_t* fragment, uint8_t rec_len);
public:

    TLV_Decoder ()
    {
        init();
    }
    ~TLV_Decoder(){reset();}

    bool decode (const buf& array);

    bool getData (MESSAGE_TYPE type, buf& result) const;


};

#endif
