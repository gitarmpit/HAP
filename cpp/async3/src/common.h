#ifndef _COMMON_H
#define _COMMON_H

#define UNUSED(x) (void)(x)

#include <stdint.h>

typedef struct _rec
{
    _rec() 
    {
        data = 0;
        length = 0;
    }
    uint8_t* data;
    int length;
} buf;

enum MESSAGE_TYPE
{
    METHOD = 0,
    ID = 1,
    SALT = 2,
    PUBLIC_KEY = 3,
    PROOF = 4,
    ENCRYPTED_DATA = 5,
    STATE = 6,  //(pairing stage such M1, M2 etc)
    ERR = 7,
    SIGNATURE = 10
};

#endif
