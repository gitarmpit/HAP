#include "session_crypto.h"

void test_crypto()
{
    SessionCrypto crypto;
    buf plain;
    //uint8_t* s = (uint8_t*)"1234567890abcd-1234567890-1234567890";
    uint8_t s[2048];
    plain.data = s;
    plain.length = 2048; //strlen((const char*)s);

loop:
    buf enc;
    for (int i = 0; i < 1000; ++i)
    {
        enc = crypto.encrypt(plain);
    }

/*
    for (int i = 0; i < enc.length; ++i)
    {
        printf ("%x ", enc.data[i]);
    }
*/
//    printf ("\n");

    buf dec;
    for (int i = 0; i < 1000; ++i)
    {
        if (!crypto.decrypt(enc, dec)) 
        {
            printf ("decrypt failed\n");
        }
    }

    goto loop;

/*
    printf ("\n");
    for (int i = 0; i < dec.length; ++i)
    {
        printf ("%c", dec.data[i]);
    }

    printf ("\n");
*/
}

void test_crypto2()
{
    //int16_t max = 8;
    //int8_t mac_size = 4;
    SessionCrypto crypto; //crypto (max, mac_size);
    buf enc = crypto.encrypt ((uint8_t*)"0000000011", 10);

    printf ("enc result:\n");
    for (int i = 0; i < enc.length; ++i) 
        printf ("%d ", enc.data[i]);
    printf ("\n");
}

void test_crypto3()
{
    buf enc;
    int16_t len = 1000;
    enc.data = (uint8_t*)malloc (len);
    int mlen = len - 2 - 16 + 1;
    enc.data[0] = (mlen & 0xff);
    enc.data[1] = (uint8_t)(mlen>>8);
    enc.length = len;

    buf dec;
    SessionCrypto crypto;
    if (!crypto.decrypt(enc, dec)) 
    {
       printf ("decrypt failed\n");
    }
    printf ("dec length: %d\n", dec.length);
}