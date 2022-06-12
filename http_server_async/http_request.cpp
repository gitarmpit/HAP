#include "http_request.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

HttpRequest::HttpRequest()
{
    msg.data = 0;
    body.data = 0;
    reset();
}

void HttpRequest::chunkReset() 
{
    free (body.data);
    body.data = 0;
    body.length = 0;
    memset (method, 0, sizeof(method));
    memset (uri, 0, sizeof(uri));
    memset (headers, 0, sizeof(headers));
}

void HttpRequest::reset()
{
    chunkReset();
    bufReset();
    content_length = -1;
}

void HttpRequest::bufReset() 
{
    free (msg.data);
    msg.data = 0;
    msg.length = 0;
}

void HttpRequest::addChunk(const uint8_t* data, int len)
{
    msg.data = (uint8_t*)realloc(msg.data, msg.length + len);
    memcpy (&msg.data[msg.length], data, len);  
    msg.length = msg.length + len;
}

void HttpRequest::addChunk(const buf& req)
{
    printf ("add chunk:%s\n", req.data);
    addChunk (req.data, req.length);
}

int HttpRequest::parse(bool expect_content_length)
{
    chunkReset();
    chunk_offset = 0;
    if (!parseMethod(msg.data))
    {
        printf ("parse: no method\n");
        bufReset();
        return -1;
    }
    if (!parseUri(msg.data))
    {
        printf ("parse: no URI\n");
        bufReset();
        return -1;
    }
    int eoh = find("\r\n\r\n", msg);
    if (eoh == -1)
    {
        return 1;
    }

    chunk_offset = 0;
    content_length = getContentLength(msg);
    printf ("parse: Content-length: %d\n", content_length);

    int hdr_sz = 0;
    int ret;
    if (content_length != -1)
    {
        if (msg.length - eoh > content_length)
        {
            printf ("malformed request: eoh=%d, msg.len=%d cl=%d\n", eoh, msg.length, content_length);
            ret = -1;
        }
        else if (msg.length - eoh < content_length)
        {
            printf ("request incomplete\n");
            ret = 1; //incomplete
        }
        else
        {
            body.length = content_length;
            body.data = (uint8_t*) malloc(content_length);
            for (int i = 0; i < content_length; ++i)
            {
                body.data[i] = msg.data[eoh + i];
            }
            hdr_sz = eoh;
            ret = 0;
        }
    }
    else
    {
        ret =  expect_content_length ? 1 : 0;
        hdr_sz = msg.length;
    }

    if (hdr_sz > (int)sizeof(headers) - 2)
    {
        hdr_sz = sizeof(headers) - 2;
    }

    if (hdr_sz)
    {
        strncpy (headers, (const char*)msg.data, hdr_sz);
        headers[hdr_sz] = '\0';
    }

    if (ret != 1)
    {
        bufReset();
    }
    return ret;
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

    chunk_offset = pos;

    static char n[5];
    int i = 0;
    for (; i < 5; ++i)
    {
        if ((buf.data[pos + i] == '\0') || (buf.data[pos + i] < '0') || (buf.data[pos + i] > '9'))
        {
            break;
        }
        n[i]  = buf.data[pos + i];
        ++chunk_offset;
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
        strncpy (method, (const char*)&buf[chunk_offset], len);
        method[len] = '\0';
        chunk_offset += len;
        return true;
    }
    else
    {
        return false;
    }

}


bool HttpRequest::parseUri(const uint8_t* buf)
{
    const uint8_t* p = &buf[chunk_offset];
    while (*p && *p++ == ' ')
        ++chunk_offset;

    while (*p && *p++ != ' ')
        ;

    if (*p)
    {
        int len = p - buf - chunk_offset - 1;
        //uri = (char* )malloc(len+1);
        //memcpy (uri, &buf[chunk_offset], len);
        //uri[len] = '\0';
        if (len > (int)sizeof(uri) - 2)
        {
            len = sizeof(uri) - 2;
        }

        strncpy (uri, (const char*)&buf[chunk_offset], len);
        uri[len] = '\0';
        chunk_offset += len;
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
    for (int i = chunk_offset; i < buf.length; ++i)
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
