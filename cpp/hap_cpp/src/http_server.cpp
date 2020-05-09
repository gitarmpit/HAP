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

void* HttpServer::thread_handler (void* arg)
{
    thread_ctx* ctx = (thread_ctx*)arg;
    HttpServer* srv = ctx->srv;
    srv->nsessions++;
    HttpSession sess (ctx->clientSocket);
    sess.run();
    printf ("Thread terminated\n");
    srv->nsessions--;
    free(ctx);
    pthread_exit(NULL);
}

void HttpServer::start()
{
    int accept_s = socket(AF_INET, SOCK_STREAM, 0);

    struct sockaddr_in serv_addr;
    memset(&serv_addr, '0', sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    serv_addr.sin_port = htons(port);

    bind(accept_s, (struct sockaddr*)&serv_addr, sizeof(serv_addr));
    listen(accept_s, 10);
    printf ("server started\n");

    static char ip[16];

    while (true)
    {
        struct sockaddr cli;
        int len;
        int client_s = accept(accept_s, &cli, &len);
        struct sockaddr_in *s = (struct sockaddr_in *)&cli;
        int cli_port = ntohs(s->sin_port);
        inet_ntop(AF_INET, &s->sin_addr, ip, sizeof ip);

        printf ("connect from: %s:%d\n", ip, cli_port);

        pthread_t tid;
        thread_ctx* ctx = (thread_ctx*) malloc (sizeof(thread_ctx));
        ctx->clientSocket = client_s;
        ctx->srv = this;
        pthread_create(&tid, NULL, HttpServer::thread_handler, ctx);
        pthread_detach(tid);
    }

}


