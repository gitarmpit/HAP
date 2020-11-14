#ifndef _HTTP_REPLY_H 
#define _HTTP_REPLY_H

#include <stdint.h>
#include "common.h"
#include <string.h>

class HttpReply 
{
private:

    int status;
    char hdr[256];
    buf msg;  //complete response
    buf body;

    void reset();
    HttpReply(int status, const char* contentType, const uint8_t* body, int len);

public: 
    HttpReply();
    HttpReply(const HttpReply& other);
    ~HttpReply() { reset(); }
    HttpReply& operator=(const HttpReply& other);

    const buf& get_body() const { return body; }
    const buf& get_msg() const { return msg; }
    const char* get_hdr() const { return hdr; }

    int get_status() const { return status; }

    static HttpReply generateOK(const char* contentType, const char* body);
    static HttpReply generateOK(const char* contentType, const uint8_t* body, int len);
    static HttpReply generateOK(const char* contentType, const buf& body);
    static HttpReply generate204(const char* contentType);
    static HttpReply generate400();


};

#endif
