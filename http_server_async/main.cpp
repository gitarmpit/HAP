#include <string.h>
#include <string.h>
#include "http_srv.h"
#include "http_request.h"
#include "http_reply.h"


int main(void)
{
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);

    HttpSrv srv(80);
    srv.start();

    printf ("process terminating\n");
}
