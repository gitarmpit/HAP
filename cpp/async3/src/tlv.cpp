#include "tlv.h"

TLV_Encoder::TLV_Encoder(int max_length)
{
    this->max_length = max_length;
    out.data = 0;
    out.length = 0;
}

void TLV_Encoder::add (MESSAGE_TYPE type, uint8_t b)
{
    add (type, &b, 1);
}

bool TLV_Encoder::add (MESSAGE_TYPE type, const buf& from)
{
    return add (type, from.data, from.length);
}

bool TLV_Encoder::add (MESSAGE_TYPE type, uint8_t* from, int size)
{
    //some sanity checks
    if (size > 2048)
    {
        printf ("enc add: record to encode is too long\n");
        return false;
    } 

    if (from == 0 || size == 0) 
    {
        printf ("enc add: buffer is 0\n");
        return false;
    }

    int enc_offset = 0;
    while (enc_offset < size)
    {
        int len = size - enc_offset;
        if (len > max_length )
        {
            len = max_length;
        }
            
        printf ("encoder: added type=%d, len=%d\n", type, len);

        addChunk ((uint8_t)type, (uint8_t)len, &from[enc_offset]);
        enc_offset += len;
    }
    return true;
}


void TLV_Encoder::addChunk (uint8_t type, uint8_t sz, uint8_t* from)
{
    out.data = (uint8_t*) realloc (out.data, out.length + sz + 2);
    out.data[out.length] = type;
    out.data[out.length+1] = sz;
    memcpy (&out.data[out.length+2], from, sz);
    out.length += sz + 2;

}

bool TLV_Decoder::decode (const buf& b)
{
    reset();
    while (decode_offset < b.length)
    {
        if (decode_offset + 2 >= b.length)
        {
            printf ("decode error 1\n"); 
            return false;
        }

        uint8_t type = b.data[decode_offset++];
        uint8_t rec_len = b.data[decode_offset++];
        
        if (rec_len + decode_offset > b.length) 
        {
            printf ("decode error 2\n"); 
            return false;
        }

        add_fragment (type, &b.data[decode_offset], rec_len);
        decode_offset += rec_len;
    }
    return true;
}


bool TLV_Decoder::getData (MESSAGE_TYPE type, buf& result) const
{
    for (tlv_map* m = this->map; m != 0;  m = m->next)
    {
        if (m->type == type)
        {
            result.length = m->rec_len;
            result.data = m->val;
            return true;
        }
    }
    return false;
}


void TLV_Decoder::add_fragment (uint8_t type, uint8_t* fragment, uint8_t rec_len)
{
    tlv_map* last = 0;
    for (tlv_map* m = this->map; m != 0;  m = m->next)
    {
        last = m;
        if (m->type == type)
        {
            m->val = (uint8_t*) realloc (m->val, m->rec_len + rec_len);
            memcpy (&m->val[m->rec_len], fragment, rec_len);
            m->rec_len += rec_len;
            printf ("decoder: adding %d bytes to type %d\n", rec_len, type);
            return;
        }
    }

    tlv_map* el = (tlv_map*) malloc (sizeof(tlv_map));
    el->next = 0;
    el->type = type;
    el->rec_len = rec_len;
    el->val = (uint8_t*) malloc (rec_len);
    memcpy (el->val, fragment, rec_len);

    if (map == 0)
    {
        map = el;
    }
    else
    {
        last->next = el;
    }

    printf ("decoder: adding %d bytes to new type %d\n", rec_len, type);
    if (rec_len == 1) 
    {
        printf ("decoder: type=%d, value=%d\n", type, *fragment);
    }
}

void TLV_Decoder::reset()
{
    for (tlv_map* m = this->map; m != 0; )
    {
        //printf ("freeing type %d, %d bytes\n", m->type, m->rec_len);
        free (m->val);
        tlv_map* tmp = m;
        m = tmp->next;
        free (tmp);
    }
    init();
}

void TLV_Decoder::init()
{
    map = 0;
    method = 0;
    stage = 0;
    decode_offset = 0;
}
