#ifndef _PROCESS_REQ_H
#define _PROCESS_REQ_H

#include "http_request.h"
#include "http_reply.h"
#include "session_crypto.h"

class ProcessReq 
{
private:
    bool pairingComplete;
    bool upgraded;

    SessionCrypto sessionCrypto;
    HttpRequest req;
    HttpReply reply;

    int processPlainText(const buf& raw_in, buf& msg_out);    
    HttpReply processPairRequest(const buf& body);
    HttpReply processVerifyRequest(const buf& body);
public:
    ProcessReq();
    bool process(const buf& raw_in, buf& msg_out);
    HttpReply processRequest (const HttpRequest& req);
    bool isPairingComplete() { return pairingComplete; }
    bool isUpgraded() { return upgraded; }
    void reset();
};

#endif