#include "tlv.h"

TLV_Encoder::TLV_Encoder()
{
    out.data = 0;
    out.length = 0;
}

void TLV_Encoder::add (MESSAGE_TYPE type, uint8_t b)
{
    add (type, &b, 1);
}

void TLV_Encoder::add (MESSAGE_TYPE type, const buf& from)
{
    add (type, from.data, from.length);
}

void TLV_Encoder::add (MESSAGE_TYPE type, uint8_t* from, int size)
{
    int offset = 0;
    while (offset < size)
    {
        int len = size - offset;
        if (len > TLV_MAX_LENGTH )
        {
            len = TLV_MAX_LENGTH;
        }
        if (len == 1)
        {
            printf ("encoder: added type=%d, len=%d val=%d\n", type, len, from[0]);
        }
        else
        {
            printf ("encoder: added type=%d, len=%d\n", type, len);
        }
        addChunk ((uint8_t)type, (uint8_t)len, &from[offset]);
        offset += len;
    }
}


void TLV_Encoder::addChunk (uint8_t type, uint8_t sz, uint8_t* from)
{
    out.data = (uint8_t*) realloc (out.data, out.length + sz + 2);
    out.data[out.length] = type;
    out.data[out.length+1] = sz;
    memcpy (&out.data[out.length+2], from, sz);
    out.length += sz + 2;

}

void TLV_Decoder::decode (const buf& b)
{
    reset();
    while (offset < b.length)
    {
        uint8_t type = b.data[offset++];
        uint8_t rec_len = b.data[offset++];
        if (rec_len == 1 && type == 6)
        {
            stage = b.data[offset];
            printf ("decoder:  stage: %d\n", stage);
        }
        else
        {
            add_fragment (type, &b.data[offset], rec_len);
        }
        offset += rec_len;
    }
}


const buf TLV_Decoder::getData (MESSAGE_TYPE type) const
{
    buf result;
    result.data = 0;
    result.length = 0;
    for (tlv_map* m = this->map; m != 0;  m = m->next)
    {
        if (m->type == type)
        {
            result.length = m->rec_len;
            result.data = m->val;
        }
    }
    return result;
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
    offset = 0;
}
