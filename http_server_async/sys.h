#ifndef _SOCK_H
#define _SOCK_H

#include <errno.h>

#ifdef __GNUC__ 
#include <unistd.h>  //write
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/ioctl.h>
#include <sys/poll.h>
#include <signal.h>

typedef int SOCKET;
#define INVALID_SOCKET -1

#define _close_socket close

#else

#define _close_socket closesocket
#define poll WSAPoll

#pragma comment(lib, "Ws2_32.lib")
#include <winsock2.h>
#include <Ws2tcpip.h>


#endif


#endif

