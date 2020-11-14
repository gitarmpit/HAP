#include "tlv.h"
#include <string.h>
#include "common.h"

void test_tlv()
{
    TLV_Encoder e;
    const char* s = "abcdefghijklm";
    e.add(ID, (uint8_t*) s, strlen(s));
    e.add(STATE, 11);
    e.add(SALT, (uint8_t*) s, strlen(s));
    e.add(SALT, (uint8_t*) s, strlen(s));

    buf encoded = e.get();


    {
        TLV_Decoder d;
        const buf decoded;
        if (!d.decode(encoded)) 
            printf ("decode error\n");
        if (!d.decode(encoded)) 
            printf ("decode error\n");
        if (!d.decode(encoded)) 
            printf ("decode error\n");
        if (!d.decode(encoded)) 
            printf ("decode error\n");
        if (!d.decode(encoded)) 
            printf ("decode error\n");

        buf r;
        if (d.getData(ID, r)) 
        {
            for (int i = 0; i < r.length; ++i)
            {
                printf ("%c", r.data[i]);
            }
        }
        else 
        {
            printf ("no ID\n");
        }

        printf ("\n");

        if (d.getData(SALT, r)) 
        {
            for (int i = 0; i < r.length; ++i)
            {
                printf ("%c", r.data[i]);
            }
        }
        else 
        {
            printf ("no salt\n");
        }
        printf ("\n");

        if (d.getData (STATE, r)) 
        {
            printf ("stage: %d\n", r.data[0]);
        }
        else 
        {
            printf ("no STATE\n");
        }

    }

}

//fault tolerance
void test_tlv2()
{
    char b[256] = {0};
//    sprintf (b, "%c%c%c%c", 6, 1, 3, '\0'); ok
//    sprintf (b, "%c%c%c%c", 6, 2, 3, '\0');  //too long
//    sprintf (b, "%c%c%c", 6, 2, '\0');  //too short
    sprintf (b, "%c%c", 6, '\0');  //too short

    buf e;
    e.data = (uint8_t*)b;
    e.length = strlen(b);

    TLV_Decoder d;
    buf r;
    if (!d.decode(e)) 
    {
        printf ("decode error\n");
    }
    if (d.getData(STATE, r)) 
    {
        printf ("stage: %d\n", r.data[0]);
    }
    else 
    {
        printf ("no state\n");
    }

    printf ("done\n");
}

void test_tlv3()
{
    TLV_Encoder e(3);
    const char* s = "1234567890";
    e.add(ID, (uint8_t*) s, strlen(s));

    buf ebuf = e.get();
//    ebuf.length--; //error 1
    ebuf.length += 3; //error 2

    TLV_Decoder d;
    buf r;
    if (d.decode(ebuf)) 
    {
        if (d.getData(ID, r)) 
        {
            printf ("ID: %s\n", r.data);
        }
        else 
        {
         printf ("no ID\n");
        }
    }
    else 
    {
        printf ("decode error\n");
    }

    printf ("done\n");
}

void test_enc() 
{
    TLV_Encoder e;
    const char* s = "abcdefghijklm";
    if (!e.add(ID, (uint8_t*) s, strlen(s) + 2048)) 
    {
        printf ("encode error\n");
    }
}
