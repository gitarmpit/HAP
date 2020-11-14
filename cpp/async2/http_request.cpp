#include "http_request.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

HttpRequest::HttpRequest()
{
    init();
}

void HttpRequest::init()
{
    //method = 0;
    //uri = 0;
    //headers = 0;
    offset = 0;
    body.data = 0;
    body.length = 0;
}


void HttpRequest::reset()
{
    //free (method);
    //free (uri);
    //free (headers);
    free (body.data);
    init();
}

bool HttpRequest::parse(const uint8_t* data, int len)
{
    buf req;
    req.data = (uint8_t*)data;
    req.length = len;
    return parse(req); 
}
bool HttpRequest::parse(const buf& req)
{
    reset();
    parseMethod(req.data);
    parseUri(req.data);
    int content_length = getContentLength(req);

    int hdr_sz;
    if (content_length != -1)
    {
        printf ("Content-length: %d\n", content_length);
        int eoh = find("\r\n\r\n", req);
        if (eoh == -1 || (req.length - eoh != content_length))
        {
            printf ("malformed request\n");
            return false;
        }
        body.length = content_length;
        body.data = (uint8_t*) malloc(content_length);
        for (int i = 0; i < content_length; ++i)
        {
            body.data[i] = req.data[eoh + i];
        }
        hdr_sz = eoh;
    }
    else
    {
        hdr_sz = req.length;
    }

    //headers = (char*) malloc(hdr_sz+1);
    //for (int i = 0; i <  hdr_sz; ++i)
    //{
    //    headers[i] = req.data[i];
    //}
    //headers[hdr_sz] = '\0';
    if (hdr_sz > (int)sizeof(headers) - 2  )
    {
        hdr_sz = sizeof(headers) - 2;
    }
    strncpy (headers, (const char*)req.data, hdr_sz);
    headers[hdr_sz] = '\0';

    return true;

}

int HttpRequest::getContentLength(const buf& buf)
{
    int pos = find("Content-Length:", buf);
    if (pos == -1)
    {
        return -1;
    }

    while (buf.data[pos] == ' ')
    {
        ++pos;
    }

    offset = pos;

    static char n[5];
    int i = 0;
    for (; i < 5; ++i)
    {
        if ((buf.data[pos + i] == '\0') || (buf.data[pos + i] < '0') || (buf.data[pos + i] > '9'))
        {
            break;
        }
        n[i]  = buf.data[pos + i];
        ++offset;
    }
    n[i] = '\0';

    return atoi (n);

}

bool HttpRequest::parseMethod(const uint8_t* buf)
{
    const uint8_t* p = buf;
    while (*p && *p++ != ' ')
        ;

    if (*p)
    {
        int len = p - buf - 1;
        //method = (char* )malloc(len+1);
        //memcpy (method, buf, len);
        //method[len] = '\0';
        if (len > (int)sizeof(method) - 2)
        {
            len = sizeof(method) - 2;
        }
        strncpy (method, (const char*)&buf[offset], len);
        method[len] = '\0';
        offset += len;
        return true;
    }
    else
    {
        return false;
    }

}


bool HttpRequest::parseUri(const uint8_t* buf)
{
    const uint8_t* p = &buf[offset];
    while (*p && *p++ == ' ')
        ++offset;

    while (*p && *p++ != ' ')
        ;

    if (*p)
    {
        int len = p - buf - offset - 1;
        //uri = (char* )malloc(len+1);
        //memcpy (uri, &buf[offset], len);
        //uri[len] = '\0';
        if (len > (int)sizeof(uri) - 2)
        {
            len = sizeof(uri) - 2;
        }

        strncpy (uri, (const char*)&buf[offset], len);
        uri[len] = '\0';
        offset += len;
        return true;
    }
    else
    {
        return false;
    }

}

int HttpRequest::find(const char* substr, const buf& buf)
{
    int strpos = 0;
    int substr_len = strlen (substr);
    for (int i = offset; i < buf.length; ++i)
    {
        if (buf.data[i] != substr[0])
        {
            continue;
        }
        strpos = i;
        int subpos = 0;
        while (true)
        {
            if (subpos == substr_len)
            {
                return strpos;
            }
            if (buf.data[strpos++] != substr[subpos++])
            {
                break;
            }

        }
    }

    return -1;
}
