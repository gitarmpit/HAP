#ifndef _HTTP_SERVER_H
#define _HTTP_SERVER_H
#define MAX_SESSIONS 10

#include <stdint.h>
#include <stdio.h>
#include <string.h>

#include "sys.h"


class HttpSession;
//typedef struct _session
//{
//    pthread_t tid;
//    HttpSession* session;
//} session_t;

class HttpServer;


typedef struct _thread_ctx
{
//    TID tid;
    HttpServer* srv;
    int clientSocket;
} thread_ctx;


class HttpServer
{
private:
    int port;
    int nsessions;
    //session_t sessions[MAX_SESSIONS];

#ifdef __GNUC__
    static void* thread_handler (void* arg);
#else 
    static DWORD WINAPI HttpServer::thread_handler(void* arg); 
#endif

public:
    HttpServer (int port);
    void start();
};


#endif
