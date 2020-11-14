#ifndef _HTTP_SESSION_H
#define _HTTP_SESSION_H
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include "sys.h"
#include "http_request.h"
#include "http_reply.h"
#include "session_crypto.h"



class HttpSession 
{
private:
    bool running;
    bool upgraded;
    SOCKET  clientSocket;
    bool pairingComplete;
    SessionCrypto sessionCrypto;

    bool waitForIncomingReq(timeval* tv, buf& msg);
    HttpReply processRequest (const HttpRequest& req);
    HttpReply processPairRequest(const buf& body);
    HttpReply processVerifyRequest(const buf& body);
    bool sendReply(const buf& reply,  timeval* tv);

public:
    HttpSession (SOCKET clientSocket);
    void run();
};


#endif
