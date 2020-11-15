#include "http_server.h"
#include <memory.h>
#include "http_session.h"
#include <stdlib.h>

HttpServer::HttpServer(int port)
{
    this->port = port;
    nsessions = 0;
    //memset (&sessions, 0, sizeof (sessions));
}

#ifdef __GNUC__
void* HttpServer::thread_handler (void* arg)
#else 
DWORD WINAPI HttpServer::thread_handler(void* arg) 
#endif
{
    thread_ctx* ctx = (thread_ctx*)arg;
    HttpServer* srv = ctx->srv;
    srv->nsessions++;
    HttpSession sess (ctx->clientSocket);
    sess.run();
    printf ("Thread terminated\n");
    srv->nsessions--;
    free(ctx);
#ifdef __GNUC__
    pthread_exit(NULL);
#endif
    return NULL;
}



void HttpServer::start()
{
    SOCKET accept_s = socket(AF_INET, SOCK_STREAM, 0);
#ifdef __GNUC__
    int on = 1;  
    if (setsockopt(accept_s, SOL_SOCKET,  SO_REUSEADDR,  (char *)&on, sizeof(on)) < 0)
    {
        perror("setsockopt() failed");
        close(accept_s);
        exit(-1);
    }
#endif

    struct sockaddr_in serv_addr;
    memset(&serv_addr, '0', sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    serv_addr.sin_port = htons(port);

    if (bind(accept_s, (struct sockaddr*)&serv_addr, sizeof(serv_addr)))
    {
#ifdef __GNUC__        
        perror ("bind error");
#else
        printf ("bind error: %d\n", WSAGetLastError());
#endif

        exit(1);
    }

    if (listen(accept_s, 10))
    {
#ifdef __GNUC__        
        perror ("listen error");
#else
        printf ("listen error: %d\n", WSAGetLastError());
#endif
        exit(1);
    }
    printf ("server started\n");
    int cnt = 0;

    while (true)
    {
        SOCKET client_s = accept(accept_s, NULL, NULL);
        if (client_s == INVALID_SOCKET) 
        {   
#ifdef __GNUC__        
            perror ("accept error");
#else
            printf ("accept error: %d\n", WSAGetLastError());
#endif
            exit(1);
        }

        thread_ctx* ctx = (thread_ctx*) malloc (sizeof(thread_ctx));
        ctx->clientSocket = client_s;
        ctx->srv = this;

        printf ("accept: creating thread: %d\n", ++cnt);  

#ifdef __GNUC__
        pthread_t tid;
        pthread_create(&tid, NULL, HttpServer::thread_handler, ctx);
        pthread_detach(tid);
#else
        HANDLE thread = CreateThread(NULL, 0, HttpServer::thread_handler, ctx, 0, NULL);
        CloseHandle(thread);
#endif
    }

}


