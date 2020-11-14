#ifndef _HTTP_SRV_H
#define _HTTP_SRV_H
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <memory.h>
#include "sys.h"
#include "http_request.h"
#include "http_reply.h"
#include "process_req.h"

#define MAX_CONN 4


class HttpSrv 
{
private:
    int port;
    int max_conn;
    SOCKET  clientSocket;
    struct pollfd fds[MAX_CONN + 1];
    int    nfds;

    //need this to keep reading after getting incomplete messages from client
    //not sure if this is possible
    buf read_buffer[MAX_CONN];
    ProcessReq procArray[MAX_CONN];
    
    void removeClient (SOCKET client_s, int connectionId);

    void processAccept (SOCKET accept_s); 
    void processClient (SOCKET client_s, int connectionId);

    int readFromClient  (SOCKET client_s, buf& msg); 
    int readFromClient2 (SOCKET client_s, buf& msg); 
    bool sendReply(SOCKET client_s, const buf& reply) const;

public:
    HttpSrv (int port, int max_conn = MAX_CONN);
    void start();
};


#endif
