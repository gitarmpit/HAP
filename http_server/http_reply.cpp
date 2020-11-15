#include "http_reply.h"
#include "stdio.h"
#include <memory.h>
#include <stdlib.h>

HttpReply::HttpReply()
{
//    own = true;
    status  = 0;
    msg.data = 0;  //complete response
    msg.length = 0;
    body.data = 0;
    body.length = 0;
    memset (hdr, 0, sizeof(hdr));
}

HttpReply& HttpReply::operator=(const HttpReply& other)
{
    if (this != &other)
    {
        this->msg.data = (uint8_t*)malloc(other.msg.length);
        memcpy(this->msg.data, other.msg.data, other.msg.length);
        this->msg.length = other.msg.length;

        this->body.data = (uint8_t*)malloc(other.body.length);
        memcpy(this->body.data, other.body.data, other.body.length);
        this->body.length = other.body.length;

        strcpy (this->hdr, other.hdr);
    }
        
    return *this;
}

HttpReply::HttpReply(const HttpReply& other)
{
        this->msg.data = (uint8_t*)malloc(other.msg.length);
        memcpy(this->msg.data, other.msg.data, other.msg.length);
        this->msg.length = other.msg.length;

        this->body.data = (uint8_t*)malloc(other.body.length);
        memcpy(this->body.data, other.body.data, other.body.length);
        this->body.length = other.body.length;

        strcpy (this->hdr, other.hdr);
}


void HttpReply::reset()
{
    //if (own) 
    {
        free(msg.data);
        free(body.data);
    }
}

void HttpReply::generateOK(const char* contentType, const char* body)
{
    generateOK(contentType, (const uint8_t*) body, (int)strlen(body));
}


void HttpReply::generateOK(const char* contentType, const buf& body)
{
    generateOK(contentType, body.data, body.length);
}

void HttpReply::generateOK(const char* contentType, const uint8_t* body, int len)
{
    reset();
    status = 200;
    const char* fmt = "HTTP/1.1 200 OK\r\nContent-type: %s\r\nContent-Length: %d\r\nConnection: close\r\n\r\n";
    sprintf (hdr, fmt, contentType, len);

    this->body.length = len;
    this->body.data = (uint8_t*) malloc (len);
    memcpy (this->body.data, body, len);

    msg.length = len + strlen(hdr);
    msg.data = (uint8_t*) malloc (msg.length);
    memcpy (msg.data, hdr, strlen(hdr));
    memcpy (&msg.data[strlen(hdr)], body, len);

}


void HttpReply::generate400()
{
    reset();
    status = 400;
    strcpy (hdr, "HTTP/1.1 400 Bad Request\r\n\r\n");
    msg.length = strlen(hdr);
    msg.data = (uint8_t*) malloc (msg.length);
    memcpy (msg.data, hdr, strlen(hdr));
}
