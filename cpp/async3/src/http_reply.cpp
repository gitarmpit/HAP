#include "http_reply.h"
#include "stdio.h"
#include <memory.h>
#include <stdlib.h>

HttpReply::HttpReply()
{
    status = 0;
    memset(hdr, 0, sizeof(hdr));
}

HttpReply::HttpReply(int status, const char* contentType, const uint8_t* body, int body_len)
{
    this->status = status;

    if (status == 400)
    {
        strcpy(this->hdr, "HTTP/1.1 400 Bad Request\r\n\r\n");
    }
    else if (status == 204)
    {
        const char* fmt = "HTTP/1.1 204 No Content\r\nContent-type: %s\r\nContent-Length: 0\r\nConnection: keep-alive\r\n\r\n";
        sprintf(this->hdr, fmt, contentType);
    }
    else if (status == 200)
    {
        const char* fmt = "HTTP/1.1 200 OK\r\nContent-type: %s\r\nContent-Length: %d\r\nConnection: keep-alive\r\n\r\n";
        sprintf(this->hdr, fmt, contentType, body_len);
    }

    this->body.length = body_len;
    if (body_len)
    {
        this->body.data = (uint8_t*) malloc(body_len);
        memcpy(this->body.data, body, body_len);
    }
    else
    {
        this->body.data = 0;
    }

    msg.length = body_len + strlen(this->hdr);
    msg.data = (uint8_t*) malloc(msg.length);
    memcpy(msg.data, this->hdr, strlen(hdr));
    memcpy(&msg.data[strlen(this->hdr)], body, body_len);

}

HttpReply& HttpReply::operator=(const HttpReply& other)
{
    if (this != &other)
    {
        reset();
        this->msg.data = (uint8_t*) malloc(other.msg.length);
        memcpy(this->msg.data, other.msg.data, other.msg.length);
        this->msg.length = other.msg.length;

        this->body.data = (uint8_t*) malloc(other.body.length);
        memcpy(this->body.data, other.body.data, other.body.length);
        this->body.length = other.body.length;

        strcpy(this->hdr, other.hdr);
    }

    return *this;
}

HttpReply::HttpReply(const HttpReply& other)
{
    reset();
    this->msg.data = (uint8_t*) malloc(other.msg.length);
    memcpy(this->msg.data, other.msg.data, other.msg.length);
    this->msg.length = other.msg.length;

    this->body.data = (uint8_t*) malloc(other.body.length);
    memcpy(this->body.data, other.body.data, other.body.length);
    this->body.length = other.body.length;

    this->status = other.status;
    strcpy(this->hdr, other.hdr);
}

void HttpReply::reset()
{
    free(msg.data);
    free(body.data);
}

HttpReply HttpReply::generateOK(const char* contentType, const char* body)
{
    return generateOK(contentType, (const uint8_t*) body, (int) strlen(body));
}

HttpReply HttpReply::generateOK(const char* contentType, const buf& body)
{
    return generateOK(contentType, body.data, body.length);
}

HttpReply HttpReply::generateOK(const char* contentType, const uint8_t* body, int len)
{
    HttpReply reply(200, contentType, body, len);
    return reply;

}

HttpReply HttpReply::generate204(const char* contentType)
{
    HttpReply reply(204, contentType, 0, 0);
    return reply;
}

HttpReply HttpReply::generate400()
{
    HttpReply reply(400, 0, 0, 0);
    return reply;
}
