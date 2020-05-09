#include "http_reply.h"
#include "stdio.h"
#include <memory.h>
#include <stdlib.h>

HttpReply::HttpReply()
{
    status  = 0;
    msg.data = 0;  //complete response
    msg.length = 0;
    body.data = 0;
    body.length = 0;
    memset (hdr, 0, sizeof(hdr));
}

HttpReply& HttpReply::operator=(const HttpReply& other) // copy assignment
{
    if (this != &other)
    {
        this->status = other.status;

        if (other.msg.length != 0)
        {
            this->msg.data = (uint8_t*)malloc(other.msg.length);
            this->msg.length = other.msg.length;
        }
        else
        {
            this->msg.data = 0;
            this->msg.length = 0;
        }
        if (other.body.length != 0)
        {
            this->body.data = (uint8_t*)malloc(other.body.length);
            this->body.length = other.body.length;
        }
        else
        {
            this->body.data = 0;
            this->body.length = 0;
        }
        strcpy (this->hdr, other.hdr);
    }
    return *this;
}

void HttpReply::reset()
{
    free(msg.data);
    free(body.data);
}

HttpReply HttpReply::generateOK(const char* contentType, const buf& body)
{
    return HttpReply::generateOK(contentType, body.data, body.length);
}


HttpReply HttpReply::generateOK(const char* contentType, uint8_t* body, int len)
{
    HttpReply reply;
    reply.status = 200;
    reply.body.length = len;
    const char* fmt = \
"HTTP/1.1 200 OK\r\nContent-type: %s\r\n\
Content-Length: %d\r\n\
Connection: keep-alive\r\n\r\n";

    sprintf (reply.hdr, fmt, contentType, len);
    reply.body.data = (uint8_t*) malloc (len);
    memcpy (reply.body.data, body, len);

    reply.msg.length = len + strlen(reply.hdr);
    reply.msg.data = (uint8_t*) malloc (reply.msg.length);
    memcpy (reply.msg.data, reply.hdr, strlen(reply.hdr));
    memcpy (&reply.msg.data[strlen(reply.hdr)], body, len);

//
//    reply.msg = new byte[reply.hdr.length + body.length];
//    System.arraycopy(reply.hdr, 0, reply.msg, 0, reply.hdr.length);
//    System.arraycopy(body, 0, reply.msg, reply.hdr.length, body.length);
    return reply;


}

HttpReply HttpReply::generate204(const char* contentType)
{
    HttpReply reply;
    reply.status = 204;
    const char* fmt = \
"HTTP/1.1 204 No Content\r\n\
Content-type: %s\r\n\
Content-Length: 0\r\n\
Connection: keep-alive\r\n\r\n";
    sprintf (reply.hdr, fmt, contentType);
    reply.msg.length = strlen(reply.hdr);
    reply.msg.data = (uint8_t*) malloc (reply.msg.length);
    memcpy (reply.msg.data, reply.hdr, strlen(reply.hdr));
    return reply;

}

HttpReply HttpReply::generate400()
{
    HttpReply reply;
    reply.status = 400;
    strcpy (reply.hdr, "HTTP/1.1 400 Bad Request\r\n\r\n");
    reply.msg.length = strlen(reply.hdr);
    reply.msg.data = (uint8_t*) malloc (reply.msg.length);
    memcpy (reply.msg.data, reply.hdr, strlen(reply.hdr));
    return reply;
}
