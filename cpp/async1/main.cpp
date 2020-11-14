#include "tlv.h"
#include <string.h>
#include "session_crypto.h"
#include <string.h>
#include "http_srv.h"
#include "http_request.h"
#include "http_reply.h"


static void test_http()
{
    const char* hdr = "POST /pair-setup HTTP/1.1\r\n\
Host: Thermometer._hap._tcp.local\r\n\
Content-Length: 6\r\n\
Content-Type: application/pairing+tlv8\r\n\r\nbody16";
    HttpRequest req;
    if (req.parse((const uint8_t*)hdr, strlen(hdr)))
    {
        printf ("method: %s, uri: %s\n", req.get_method(), req.get_uri());
        printf ("headers: %s\n", req.get_headers());
        buf body = req.get_body();
        for (int i = 0; i < body.length; ++i)
        {
            printf ("%c", body.data[i]);
        }

        printf ("\n");
    }
    else
    {
        printf ("parse error\n");
    }
}

static void test_reply() 
{
    const char* body = "this is the body";
    HttpReply r = HttpReply::generateOK("application/pairing+tlv8", (uint8_t*)body, strlen(body));
    printf ("headers:\n");
    printf ("%s\n", r.get_hdr());
    printf ("full message:\n");
    for (int i = 0; i < r.get_msg().length; ++i)
    {
        printf ("%c", r.get_msg().data[i]);
    }
    printf ("\nbody:\n");
    for (int i = 0; i < r.get_body().length; ++i)
    {
        printf ("%c", r.get_body().data[i]);
    }

    printf ("\nall done\n");

}

static HttpReply test_reply2() 
{
    const uint8_t body[1024] = {1};
    HttpReply r = HttpReply::generateOK("application/pairing+tlv8", body, 1024);
    const char* hdr = r.get_hdr();
    const buf msg = r.get_msg();
    const buf b = r.get_body();
    return r;
}

static void test_http_reply()
{
    for (int i = 0; i < 1000; ++i )
    {
        HttpReply r = test_reply2();
        const char* hdr = r.get_hdr();
        const buf msg = r.get_msg();
        const buf body = r.get_body();
    }
}

static void test_tlv()
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
        d.decode(encoded);
        d.decode(encoded);
        d.decode(encoded);
        d.decode(encoded);

        buf r = d.getData(ID);
        if (r.data != 0)
        {
            for (int i = 0; i < r.length; ++i)
            {
                printf ("%c", r.data[i]);
            }
        }

        printf ("\n");

        r = d.getData(SALT);
        if (r.data != 0)
        {
            for (int i = 0; i < r.length; ++i)
            {
                printf ("%c", r.data[i]);
            }
        }

        printf ("\n");

        printf ("stage: %d\n", d.getStage());

    }

}

static void test_crypto()
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
        dec = crypto.decrypt(enc);
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

class test
{
private:
    char name[32];
    char* buf;
    uint8_t* large_buf;
    bool own;
    static int cnt;
public:
    test (const char* name)
    {
        own = true;
        buf = 0;
        large_buf = 0;
        strcpy (this->name, name);
        ++cnt;
    }

    void alloc (const char* buf)
    {
        this->buf = (char*) malloc (strlen(buf));
        strcpy (this->buf, buf);
        large_buf = (uint8_t*) malloc (100000);
        memset (large_buf, 1, 100000);
    }

    void print()
    {
        printf ("%s\n", name);
        if (buf)
        {
            printf ("%s\n", buf);
        }
        for (int i = 0; i < 5; ++i)
        {
            printf ("%d ", large_buf[100000 - 1 - i]);
        }
        printf ("\n");
    }


    ~test()
    {
        if (own)
        {
            printf ("freeing %d\n", cnt);
            free (buf);
            free(large_buf);
        }
    }

    test& operator=(test& other)
    {
        printf ("=\n");
        if (this != &other)
        {
            this->own = true;
            this->buf = other.buf;
            this->large_buf = other.large_buf;
            other.own = false;
            strcpy (this->name, other.name);
        }
        return *this;
    }
};

int test::cnt = 0;


int main(void)
{

#if defined(_WIN32) || defined(WIN32)
    printf ("initializing winsock\n");
    WSADATA wsaData;
    int iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
    if (iResult != 0) {
        printf("WSAStartup failed: %d\n", iResult);
        return 1;
    }
#else
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);
#endif
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);


//    while(true)
//    {
//      test t3 ("name3");
//      {
//          test t2 ("name2");
//          {
//              test t ("name1");
//              t.alloc ("alloced");
//              t.print();
//              t2 = t;
//              t2.print();
//          }
//          t2.print();
//          t2.print();
//          t3 = t2;
//      }
//      t3.print();
//      sleep(1);
//    }

//   test_crypto();


/*
    while(1)
    {
        test_tlv();
        test_http();
        test_http_reply();
    }
*/

    //advertise("Test Bridge", "11:11:11:11:11:11", "192.168.1.109");
    //advertise2();

    HttpSrv srv(9123);
    srv.start();

    printf ("process terminating\n");
}
