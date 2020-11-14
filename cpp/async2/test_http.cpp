#include "http_request.h"
#include "http_reply.h"
#include <stdio.h>

void test_http()
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

void test_reply() 
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

HttpReply test_reply2() 
{
    const uint8_t body[1024] = {1};
    HttpReply r = HttpReply::generateOK("application/pairing+tlv8", body, 1024);
    const char* hdr = r.get_hdr();
    const buf msg = r.get_msg();
    const buf b = r.get_body();
    UNUSED(hdr);
    UNUSED(msg);
    UNUSED(b);
    return r;
}

void test_http_reply()
{
    for (int i = 0; i < 1000; ++i )
    {
        HttpReply r = test_reply2();
        const char* hdr = r.get_hdr();
        const buf msg = r.get_msg();
        const buf body = r.get_body();
        UNUSED(hdr);
        UNUSED(msg);
        UNUSED(body);
    }
}

