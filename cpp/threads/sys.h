#ifndef _SOCK_H
#define _SOCK_H

#ifdef __GNUC__ 
#include <unistd.h>  //write
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <pthread.h>

typedef int SOCKET;
#define INVALID_SOCKET -1

#define _close_socket close

#else

#define _close_socket closesocket

#pragma comment(lib, "Ws2_32.lib")
#include <winsock2.h>
#include <Ws2tcpip.h>


#endif


#endif

