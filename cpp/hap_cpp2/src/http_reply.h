#ifndef _HTTP_REPLY_H 
#define _HTTP_REPLY_H

#include <stdint.h>
#include "common.h"

class HttpReply 
{
private:

    void reset();
    HttpReply();

public: 
    ~HttpReply() { reset(); }
    HttpReply& operator=(const HttpReply& other);

    static HttpReply generateOK(const char* contentType, uint8_t* body, int len);
    static HttpReply generateOK(const char* contentType, const buf& body);
    static HttpReply generate204(const char* contentType);
    static HttpReply generate400();
    int status;
    char hdr[256];
    buf msg;  //complete response
    buf body;


};

#endif
