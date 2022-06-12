#include "http_srv.h"

#ifdef __GNUC__        
volatile sig_atomic_t stopping = 0;
static void int_handler(int sig)
{ 
  stopping = 1; 
}
#else 
static bool stopping = false;
BOOL WINAPI int_handler(DWORD signal) 
{

    if (signal == CTRL_C_EVENT)
    {
        stopping = true;
        return TRUE;
    }
    else 
        return FALSE;
}

#endif

HttpSrv::HttpSrv(int port, int max_conn)
{
    this->clientSocket = clientSocket;
    this->max_conn = max_conn;
    this->port = port;
    nfds = 0;
    memset (read_buffer, 0, sizeof(read_buffer));
#ifdef __GNUC__        
    signal(SIGINT, int_handler); 
#else
    if (!SetConsoleCtrlHandler(int_handler, TRUE)) 
    {
        printf("Could not set control handler\n"); 
    }
#endif
}

static void error (const char* what, bool fatal) 
{
#ifdef __GNUC__        
        static char buf[256];
        sprintf (buf, "%s error", what);
        perror (buf);
        if (errno == EINTR)
        {
            fatal = false;
        }
#else
        printf ("%s error: %d\n", what, WSAGetLastError());
#endif
        if (fatal)
            exit(1);
}

static void setup(int s) 
{
  int on = 1;  
  if (setsockopt(s, SOL_SOCKET,  SO_REUSEADDR,  (char *)&on, sizeof(on)) < 0)
  {
    error ("setsockopt", true);
  }

#ifdef __GNUC__
  if (ioctl(s, FIONBIO, (char*)&on) < 0)
#else
  ULONG on2 = 1;
  if (ioctlsocket(s, FIONBIO, &on2) < 0)
#endif
  {
    error ("ioctl", true);
  }
}

void HttpSrv::start()
{
#if defined(_WIN32) || defined(WIN32)
    printf ("initializing winsock\n");
    WSADATA wsaData;
    int iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
    if (iResult != 0) {
        printf("WSAStartup failed: %d\n", iResult);
        return;
    }
#endif

    SOCKET accept_s = socket(AF_INET, SOCK_STREAM, 0);

    printf ("accept fd: %d\n", accept_s);
    setup (accept_s);

    struct sockaddr_in serv_addr;
    memset(&serv_addr, '0', sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    serv_addr.sin_port = htons(port);

    if (bind(accept_s, (struct sockaddr*)&serv_addr, sizeof(serv_addr)))
    {
        error ("bind", true);
    }

    if (listen(accept_s, 10))
    {
        error ("listen", true);
    }

    printf ("server started\n");

    memset(fds, 0 , sizeof(fds));

    fds[0].fd = accept_s;
    fds[0].events = POLLIN;

    int timeout = 10; //ms
    while (!stopping)
    {
        int rc = poll(fds, max_conn+1, timeout);
        if (rc <= 0) 
        {
            if (rc == -1)
            {
                error ("poll", true);
            }
            continue;
        }

        printf ("poll returned: rc=%d\n", rc);
        for (int i = 0; i <= max_conn; i++)
        {
            printf ("%d: fd=%d revents=%d\n", i, fds[i].fd, fds[i].revents);
        }


        for (int i = 1; i <= max_conn; i++)
        {
            //if (fds[i].revents == POLLIN && fds[i].fd != 0) 
            if (fds[i].fd != 0) 
            {
                bool error = false;
                if (fds[i].revents & POLLHUP)
                {
                    error = true;
                    printf ("pollhup\n");
                }
                if (fds[i].revents & POLLERR)
                {
                    error = true;
                    printf ("pollherr\n");
                }
                if (fds[i].revents & POLLIN)
                {
                    printf ("pollin\n");
                }
                if (error)
                {
                    removeClient (fds[i].fd, i); 
                }
                else 
                {
                    processClient (fds[i].fd, i);
                }
            }
        }

        if((fds[0].revents & POLLIN) && fds[0].fd == accept_s)
        {
           processAccept(accept_s);
        }

    }

    for (int i = 0; i < max_conn; ++i)
    {
        free(read_buffer[i].data);
    }

    printf ("server stopped\n");

}

void HttpSrv::processAccept(SOCKET accept_s) 
{
   printf ("accept: total: %d\n", nfds);
   SOCKET client_s = accept(accept_s, NULL, NULL);
#ifdef __GNUC__
   if (client_s < 0 && errno != EWOULDBLOCK)
#else
   if (client_s < 0 &&  WSAGetLastError() != WSAEWOULDBLOCK)
#endif
   {
        error ("accept", true);
   }

   if (nfds == max_conn)
   {
      printf ("too many connections: %d, accept ignored\n", nfds);
      _close_socket (client_s);
      return;
   }

   int id = -1;
   for (int i = 1; i <= max_conn; ++i)
   {
       if (fds[i].fd == 0) 
       {
            id = i;
            break;
       }
   }

   if (id == -1) 
   {
       printf ("no  available connection slots\n");
      _close_socket (client_s);
      return;
   }

#ifdef __GNUC__
  int on = 1;
  if (ioctl(client_s, FIONBIO, (char*)&on) < 0)
#else
  ULONG on2 = 1;
  if (ioctlsocket(client_s, FIONBIO, &on2) < 0)
#endif
  {
    error ("ioctl", true);
  }

   printf("new incoming connection: %d, connection id %d:\n", client_s, id);
   fds[id].fd = client_s;
   fds[id].events = POLLIN;
   nfds++;
}

void HttpSrv::removeClient (SOCKET client_s, int connectionId) 
{
        printf ("remove client socket %d from connection id: %d\n", client_s, connectionId);
        fds[connectionId].fd = 0;
        fds[connectionId].events = 0;
        _close_socket (client_s);
        nfds--;
}

void HttpSrv::processClient (SOCKET client_s, int connectionId)
{
    printf ("conn from client: %d\n", client_s);

    buf& raw_in = read_buffer[connectionId-1];

    int rc = readFromClient2 (client_s, raw_in);
    if (rc == -1)
    {
        printf ("read failed, remove client: %d\n", client_s);
        removeClient (client_s, connectionId);
    }
    else if (rc != 1) //not would block 
    {
        printf("Raw request len: %d bytes\n", raw_in.length);
        buf msg_out;
        if (procArray[connectionId-1].process (raw_in, msg_out)) 
        {
            printf("Reply len: %d bytes\n", msg_out.length);
            if (!sendReply(client_s, msg_out)) 
            {
                removeClient (client_s, connectionId);
            }
        }

    }

    free (raw_in.data);
    raw_in.data = 0;
    raw_in.length = 0;

    printf ("\n===================\n");

}


//a little less half-assed
int HttpSrv::readFromClient2 (SOCKET s, buf& msg) 
{
    static char buf[256];
#ifdef __GNUC__
    int bytes_available = 0;
    if (ioctl(s, FIONREAD, &bytes_available) == 0)
#else
    ULONG bytes_available = 0;
    if (ioctlsocket(s, FIONREAD, &bytes_available) == 0)
#endif
    {
        printf ("recv: available:  %d\n", bytes_available); 
    }
    else
    {
        error ("ioctl", false);
    }       
    int rc = 0; 
    while (true) 
    {
        rc = recv(s, buf, sizeof(buf), 0);
        if (rc < 0)
        {
#ifdef __GNUC__
           if (errno == EWOULDBLOCK)
#else
           if (WSAGetLastError() == WSAEWOULDBLOCK)
#endif
           {
               printf ("recv: would block\n");
               rc = msg.length ? 0 : 1;  //blocked after reading some data, assuming all is read
                                         //otherwise, back to polling 
           }
           else 
           {
               //error ("recv", false);
               printf ("recv: cli closed\n");
               rc = -1; 
           }
           break;
        }
        else if (rc == 0) 
        {
            printf ("recv: 0, cli closed\n");
            rc = -1;
            break;
        }

        int len = rc;
        printf("recv: %d bytes read\n", len);
        msg.data = (uint8_t*) realloc(msg.data, msg.length + len);
        memcpy(&msg.data[msg.length], buf, len);
        msg.length += len;
    }
    
    return rc;
}


bool HttpSrv::sendReply(SOCKET s, const buf& reply) const
{
    bool rc = true;

    int total_written = 0;
    while (total_written < reply.length)
    {
        int written = send (s, (char*)&reply.data[total_written], reply.length - total_written, 0);
        if (errno == EWOULDBLOCK) 
        {
            printf ("write: wouldblock\n");
        }
        if (written == -1)
        {
            printf ("socket write error\n");
            rc = false;
            break;
        }
        else if (written == 0)
        {
            printf ("written 0 bytes\n");
            break;
        }
        printf ("written %d bytes\n", written);
        total_written += written;
    }

    return rc;
}


