#ifndef _HTTP_SESSION_H
#define _HTTP_SESSION_H
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include "sys.h"
#include "http_request.h"
#include "http_reply.h"

class HttpSession 
{
private:
    SOCKET  clientSocket;

    bool waitForIncomingReq(timeval* tv, buf& msg);
    HttpReply processRequest (const HttpRequest& req);
    bool sendReply(const buf& reply,  timeval* tv);

public:
    HttpSession (SOCKET clientSocket);
    void run();
};


#endif
