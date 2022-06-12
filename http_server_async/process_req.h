#ifndef _PROCESS_REQ_H
#define _PROCESS_REQ_H

#include "http_request.h"
#include "http_reply.h"

class ProcessReq 
{
private:

    HttpRequest req;
    HttpReply reply;

    int processPlainText(const buf& raw_in, buf& msg_out);    
public:
    ProcessReq();
    bool process(const buf& raw_in, buf& msg_out);
    HttpReply processRequest (const HttpRequest& req);
};

#endif