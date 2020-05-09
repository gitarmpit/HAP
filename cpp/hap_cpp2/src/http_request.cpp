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
    method = 0;
    uri = 0;
    body.data = 0;
    headers = 0;
    offset = 0;
    body.length = 0;
}


void HttpRequest::reset()
{
    free (method);
    free (uri);
    free (body.data);
    free (headers);
    init();
}

bool HttpRequest::parse(const uint8_t* req, int len)
{
    reset();
    parseMethod(req);
    parseUri(req);
    int content_length = getContentLength(req);

    int hdr_sz;
    if (content_length != -1)
    {
        printf ("Content-length: %d\n", content_length);
        int eoh = find("\r\n\r\n", (const char*)req);
        if (eoh == -1 || (len - eoh != content_length))
        {
            printf ("malformed request\n");
            return false;
        }
        body.length = content_length;
        body.data = (uint8_t*) malloc(content_length);
        for (int i = 0; i < content_length; ++i)
        {
            body.data[i] = req[eoh + i];
        }
        hdr_sz = eoh;
    }
    else
    {
        hdr_sz = len;
    }

    headers = (char*) malloc(hdr_sz);
    for (int i = 0; i <  hdr_sz; ++i)
    {
        headers[i] = req[i];
    }

    return true;

}

int HttpRequest::getContentLength(const uint8_t* buf)
{
    int pos = find("Content-Length:", (const char*) buf);
    if (pos == -1)
    {
        return -1;
    }

    while (buf[pos] == ' ')
    {
        ++pos;
    }

    offset = pos;

    static char n[5];
    int i = 0;
    for (; i < 5; ++i)
    {
        if ((buf[pos + i] == '\0') || (buf[pos + i] < '0') || (buf[pos + i] > '9'))
        {
            break;
        }
        n[i]  = buf[pos + i];
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
        method = (char* )malloc(len+1);
        memcpy (method, buf, len);
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
        uri = (char* )malloc(len+1);
        memcpy (uri, &buf[offset], len);
        uri[len] = '\0';
        offset += len;
        return true;
    }
    else
    {
        return false;
    }

}

int HttpRequest::find(const char* substr, const char* str)
{
    int strpos = 0;
    int str_len = strlen (str);
    int substr_len = strlen (substr);
    for (int i = offset; i < str_len; ++i)
    {
        if (str[i] != substr[0])
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
            if (str[strpos++] != substr[subpos++])
            {
                break;
            }

        }
    }

    return -1;
}
