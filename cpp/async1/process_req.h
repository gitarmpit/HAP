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

    buf msg_out;

    SessionCrypto sessionCrypto;

    
    HttpReply processPairRequest(const buf& body);
    HttpReply processVerifyRequest(const buf& body);
public:
    ProcessReq();
    buf& process(const buf& raw_in);
    HttpReply processRequest (const HttpRequest& req);
    bool isPairingComplete() { return pairingComplete; }
    bool isUpgraded() { return upgraded; }
    void reset();

    ~ProcessReq () { free (msg_out.data); }
};

#endif