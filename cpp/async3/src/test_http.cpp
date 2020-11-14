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
    req.addChunk((const uint8_t*)hdr, strlen(hdr));
    
    int rc = req.parse(true);
    if (rc == 0)
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
        printf ("parse error: %d\n", rc);
    }
}

static void print (HttpRequest& req) 
{
        printf ("content-length: %d\n", req.getContentLength());
        printf ("method: %s, uri: %s\n", req.get_method(), req.get_uri());
        printf ("headers: %s\n", req.get_headers());
        buf body = req.get_body();
        for (int i = 0; i < body.length; ++i)
        {
            printf ("%c", body.data[i]);
        }

        printf ("\n");
}
void test_http_chunks()
{
    const char* hdr1 = "POST /pair-setup HTTP/1.1\r\nHost: Thermometer._hap._tcp.local\r\n";
    const char* hdr2 = "Content-Length: 6\r\n";
    const char* hdr3 = "Content-Type: application/pairing+tlv8\r\n\r\n";
    HttpRequest req;
    req.addChunk((const uint8_t*)hdr1, strlen(hdr1));
    //req.addChunk((const uint8_t*)hdr2, strlen(hdr2));
    int rc = req.parse(true);
    if (rc == 0)
    {
        print (req);
    }
    else
    {
        printf ("parse error: %d\n", rc);
    }
    
    req.addChunk((const uint8_t*)hdr2, strlen(hdr2));
    rc = req.parse(true);
    if (rc == 0)
    {
        print (req);
    }
    else
    {
        printf ("parse error: %d\n", rc);
    }

    req.addChunk((const uint8_t*)hdr3, strlen(hdr3));
    rc = req.parse(true);
    if (rc == 0)
    {
        print (req);
    }
    else
    {
        printf ("parse error: %d\n", rc);
    }

    req.addChunk((uint8_t*)"55555", 5);
    rc = req.parse(true);
    if (rc == 0)
    {
        print (req);
    }
    else
    {
        printf ("parse error: %d\n", rc);
    }

    req.addChunk((uint8_t*)"1", 1);
    rc = req.parse(true);
    if (rc == 0)
    {
        print (req);
    }
    else
    {
        printf ("parse error: %d\n", rc);
    }
}

void test_http4() 
{
    const char* hdr1 = "GET /accessories HTTP/1.1\r\n";
    const char* hdr2 = "Host: Thermometer._hap._tcp.local\r\n\r\n";
    HttpRequest req;
    req.addChunk((const uint8_t*)hdr1, strlen(hdr1));
    //req.addChunk((const uint8_t*)hdr2, strlen(hdr2));
    int rc = req.parse(false);
    if (rc == 0)
    {
        print (req);
    }
    else
    {
        printf ("parse error: %d\n", rc);
    }
    
    req.addChunk((const uint8_t*)hdr2, strlen(hdr2));
    rc = req.parse(false);
    if (rc == 0)
    {
        print (req);
    }
    else
    {
        printf ("parse error: %d\n", rc);
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

