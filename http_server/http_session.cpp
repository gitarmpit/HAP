#include "http_session.h"

HttpSession::HttpSession(SOCKET clientSocket)
{
    this->clientSocket = clientSocket;
}

void HttpSession::run()
{

#ifdef __GNUC__    
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);
#endif

    printf("Http session started\n");

    struct timeval tv;
    tv.tv_sec = 1;
    tv.tv_usec = 0;
    int cnt = 0;
    buf msg_in;
    while (true)
    {
        printf ("start waiting: %d\n", ++cnt);
        if (!waitForIncomingReq(&tv, msg_in))
        {
            printf ("wait failed\n");
            break;
        }

        printf("Raw request len: %d bytes\n", msg_in.length);

        HttpRequest req;
        bool rc = req.parse(msg_in);
        if (rc)
        {
            printf ("Request headers:\n%s\n", req.get_headers());
            buf body = req.get_body();
            printf("request len: %d bytes\n", body.length);
            for (int i = 0; i < body.length; ++i )
            {
                printf ("%c", body.data[i]);
            }
            printf ("\n");

            HttpReply reply = processRequest(req);

            printf("\nPlaintext reply: %d bytes\n", reply.get_msg().length);
            printf ("Reply headers: %s\n", reply.get_hdr());
            if (!sendReply(reply.get_msg(), &tv))
            {
                printf ("send failed\n");
                break;
            }

            printf ("\n===================\n");
            break;

        }
        else
        {
            printf ("malformed HTTP request\n");
            break;
       }
       free (msg_in.data);
       msg_in.data = 0;
    }
    free (msg_in.data);
    printf ("session closed\n");
    _close_socket(clientSocket);
}

HttpReply HttpSession::processRequest (const HttpRequest& req)
{
    const char* uri = req.get_uri();
    const char* httpMethod = req.get_method();
    char body[256];
    static int cnt = 1;

    printf ("uri: %s, method: %s\n", uri, httpMethod);

    HttpReply reply;

    if (uri == 0 || httpMethod == 0)
    {
        printf("process: malformed request, no uri or method\n");
        reply.generate400();
    }
    else 
    {
        memset (body, 0, sizeof body);
        sprintf (body, "<html><body>Test-%d<p></body></html>", ++cnt);    
        reply.generateOK("text/html; charset=utf-8", body);
    }


    return reply;

}

bool HttpSession::sendReply(const buf& reply, timeval* tv)
{
    bool rc = true;

    int total_written = 0;
    while (true)
    {
        int written = send (clientSocket, (char*)&reply.data[total_written], reply.length - total_written, 0);
        if (written == -1)
        {
            printf ("socket write error\n");
            rc = false;
        }
        else if (written == 0)
        {
            printf ("written 0 bytes\n");
            break;
        }
        printf ("written %d bytes\n", written);
        total_written += written;
        if (total_written == reply.length)
        {
            break;
        }
    }

    return rc;
}
bool HttpSession::waitForIncomingReq(timeval* tv, buf& msg)
{
    bool rc = true;
    fd_set readfds;
    FD_ZERO(&readfds);
    FD_SET(clientSocket, &readfds);

    while (true)
    {
        int rc = select(clientSocket + 1, &readfds, NULL, NULL, tv);
        if (rc > 0)
        {
            break;
        }
        else if (rc < 0) 
        {
#ifdef __GNUC__        
            perror ("select error");
#else
            printf ("select error: %d\n", WSAGetLastError());
#endif
            exit(1);
        }

    }

    printf ("select returned\n");
    msg.data = 0;
    msg.length = 0;
    static char buf[256];
    while (true)
    {
        int read_count = recv(clientSocket, buf, sizeof(buf), 0);
        if (read_count == -1)
        {
            printf("socket read error\n");
            rc = false;
            break;
        }
        else if (read_count == 0)
        {
            printf("0 bytes read\n");
            break;
        }
        else
        {
            printf("%d bytes read\n", read_count);
            msg.data = (uint8_t*) realloc(msg.data, msg.length + read_count);
            memcpy(&msg.data[msg.length], buf, read_count);
            msg.length += read_count;
            if (read_count != sizeof(buf))
            {
                printf("recv: eof message\n");
                break;
            }
        }
    }
    return rc;
}

