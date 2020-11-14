#ifndef _HTTP_REQUEST_H
#define _HTTP_REQUEST_H

#include <stdint.h>
#include "common.h"

class HttpRequest
{
private:
    char uri[64];
    char method[16];
    buf  body;
    char headers[256];
    int offset;

    int find(const char* substr, const buf& buf);
    int getContentLength(const buf& buf);
    bool parseMethod(const uint8_t* buf);
    bool parseUri(const uint8_t* buf);

    void init();
    void reset();

public:

    HttpRequest ();
    bool parse(const buf& req);
    bool parse(const uint8_t* req, int len);
    const char* get_method() const { return method; }
    const char* get_uri() const { return uri; }
    const char* get_headers() const { return headers; }
    const buf& get_body () const { return body; }
    ~HttpRequest() { reset(); }
};


#endif
