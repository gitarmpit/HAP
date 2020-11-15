#include <string.h>
#include <string.h>
#include "http_server.h"
#include "http_request.h"
#include "http_reply.h"

int main(void)
{

#if defined(_WIN32) || defined(WIN32)
    printf ("initializing winsock\n");
    WSADATA wsaData;
    int iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
    if (iResult != 0) {
        printf("WSAStartup failed: %d\n", iResult);
        return 1;
    }
#else
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);
#endif
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);
    HttpServer srv(80);
    srv.start();
    printf ("process terminating\n");
}
