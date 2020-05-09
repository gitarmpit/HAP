#ifndef _HTTP_REQUEST_H
#define _HTTP_REQUEST_H

#include <stdint.h>
#include "common.h"

class HttpRequest
{
private:
    char* uri;
    char* method;
    buf body;
    char* headers;
    int offset;

    int find(const char* substr, const char* str);
    int getContentLength(const uint8_t* buf);
    bool parseMethod(const uint8_t* buf);
    bool parseUri(const uint8_t* buf);

    void init();
    void reset();

public:

    HttpRequest ();
    bool parse(const uint8_t* req, int len);
    char* get_method() const { return method; }
    char* get_uri() const { return uri; }
    char* get_headers() const { return headers; }
    const buf& get_body () const { return body; }
    ~HttpRequest() { reset(); }
};


#endif
