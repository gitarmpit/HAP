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
    //bool own;

public: 
    HttpReply();
    ~HttpReply() { reset(); }
    HttpReply& operator=(const HttpReply& other);
    HttpReply(const HttpReply& other);

    const buf& get_body() const { return body; }
    const buf& get_msg() const { return msg; }
    const char* get_hdr() const { return hdr; }

    int get_status() const { return status; }

    void generateOK(const char* contentType, const char* body);
    void generateOK(const char* contentType, const uint8_t* body, int len);
    void generateOK(const char* contentType, const buf& body);
    void generate204(const char* contentType);
    void generate400();


};

#endif
