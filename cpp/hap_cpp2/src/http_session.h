#ifndef _HTTP_SESSION_H
#define _HTTP_SESSION_H
#include <stdint.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <memory.h>
#include "http_request.h"
#include "http_reply.h"
#include "session_crypto.h"

class HttpSession 
{
private:
    bool running;
    bool upgraded;
    int  clientSocket;
    bool pairingComplete;
    SessionCrypto crypto;

    bool waitForIncomingReq(timeval* tv, uint8_t*& msg, int& msg_size);
    HttpReply processRequest (const HttpRequest& req);
    HttpReply processPairRequest(const buf& body);
    HttpReply processVerifyRequest(const buf& body);
    bool sendReply(buf& reply,  timeval* tv);

public:
    HttpSession (int clientSocket);
    void run();
};


#endif
