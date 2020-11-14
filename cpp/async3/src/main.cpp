#include "tlv.h"
#include <string.h>
#include "session_crypto.h"
#include <string.h>
#include "http_srv.h"
#include "http_request.h"
#include "http_reply.h"

void test_tlv2();
void test_tlv();
void test_tlv3();
void test_enc();
void test_crypto2();
void test_crypto3();
void test_http();
void test_http_chunks();
void test_http4();

int main(void)
{
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);
    //test_http();
    //test_http_chunks();
    test_http4();
    //test_enc();
    //test_crypto2();
    //test_crypto3();
    //exit(1);

    //advertise("Test Bridge", "11:11:11:11:11:11", "192.168.1.109");
    //advertise2();

    HttpSrv srv(9123);
    srv.start();

    printf ("process terminating\n");
}
