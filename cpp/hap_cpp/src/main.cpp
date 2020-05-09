
void advertise (const char* service_name, const char* id, const char* ip);
void advertise2();

#include "tlv.h"
#include <string.h>
#include "session_crypto.h"
#include <string.h>
#include "http_request.h"
#include "http_reply.h"
#include "http_server.h"
#include "common.h"

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
        int sz;
        buf body = req.get_body();
        for (int i = 0; i < sz; ++i)
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

static void test_http_reply()
{
    const char* body = "this is the body";
    HttpReply r = HttpReply::generateOK("application/pairing+tlv8", (uint8_t*)body, strlen(body));
    printf ("headers:\n");
    printf ("%s\n", r.hdr);
    printf ("full message:\n");
    for (int i = 0; i < r.msg.length; ++i)
    {
        printf ("%c", r.msg.data[i]);
    }
    printf ("\nbody:\n");
    for (int i = 0; i < r.body.length; ++i)
    {
        printf ("%c", r.body.data[i]);
    }

    printf ("\nall done\n");

}

static void test_tlv()
{
    TLV_Encoder e;
    const char* s = "abcdefghijklm";
    e.add(ID, (uint8_t*) s, strlen(s));
    e.add(STATE, 11);
    e.add(SALT, (uint8_t*) s, strlen(s));
    e.add(SALT, (uint8_t*) s, strlen(s));

    int len;
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
            for (int i = 0; i < len; ++i)
            {
                printf ("%c", r.data[i]);
            }
        }

        printf ("\n");

        r = d.getData(SALT);
        if (r.data != 0)
        {
            for (int i = 0; i < len; ++i)
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
    const uint8_t* s = (const uint8_t*)"1234567890abcd-1234567890-1234567890";
    uint8_t* enc = 0;
    int enc_sz, dec_sz;
    crypto.encrypt(s, strlen((const char*)s), enc, enc_sz);

    for (int i = 0; i < enc_sz; ++i)
    {
        printf ("%x ", enc[i]);
    }

    printf ("\n");

    uint8_t* dec;
    crypto.decrypt(enc, enc_sz, dec, dec_sz);
    crypto.encrypt(dec, dec_sz, enc, enc_sz);
    crypto.decrypt(enc, enc_sz, dec, dec_sz);

    printf ("\n");
    for (int i = 0; i < dec_sz; ++i)
    {
        printf ("%c", dec[i]);
    }

}

int main(void)
{
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);


//    while(1)
//    {
//        test_crypto();
//        test_tlv();
//        test_http();
//        test_http_reply();
//    }

    //advertise("Test Bridge", "11:11:11:11:11:11", "192.168.1.109");
    //advertise2();

    HttpServer srv(9123);
    srv.start();


}
