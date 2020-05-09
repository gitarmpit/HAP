#ifndef _HTTP_SERVER_H
#define _HTTP_SERVER_H
#define MAX_SESSIONS 10

#include <stdint.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdio.h>
//#include <stdlib.h>
//#include <unistd.h>
#include <string.h>
//#include <sys/types.h>
#include <pthread.h>

class HttpSession;
//typedef struct _session
//{
//    pthread_t tid;
//    HttpSession* session;
//} session_t;

class HttpServer;
typedef struct _thread_ctx
{
    pthread_t tid;
    HttpServer* srv;
    int clientSocket;
} thread_ctx;

class HttpServer
{
private:
    int port;
    int nsessions;
    //session_t sessions[MAX_SESSIONS];

    static void* thread_handler(void* arg);

public:
    HttpServer (int port);
    void start();
};


#endif
