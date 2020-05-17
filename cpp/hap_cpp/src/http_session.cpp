#include "http_session.h"
#include "tlv.h"
#include "common.h"
#include "pair_setup.h"
#include "pair_verify.h"

HttpSession::HttpSession(int clientSocket)
{
    running = true;
    upgraded = false;
    pairingComplete = false;
    this->clientSocket = clientSocket;
}

void HttpSession::run()
{
    setvbuf(stdout, NULL, _IONBF, 0);
    setvbuf(stderr, NULL, _IONBF, 0);
    printf("Http session started\n");
    struct timeval tv;
    tv.tv_sec = 1;
    tv.tv_usec = 0;

    uint8_t* msg;
    int msg_size;
    bool encrypt = false;
    while (running)
    {
        if (!waitForIncomingReq(&tv, msg, msg_size))
        {
            printf ("wait failed\n");
            break;
        }

        HttpRequest req;
        bool rc = req.parse(msg, msg_size);
        if (rc)
        {
            HttpReply reply = processRequest(req);
            printf("Plaintext reply: %d bytes\n", reply.msg.length);
            printf ("%s", reply.hdr);
            if (encrypt)
            {
                //msg = sessionCrypto.encrypt(reply.msg.data);
                //System.out.printf("\nEncrypted reply: %d bytes\n\n", msg.length);
            }

            if (!sendReply(reply.msg, &tv))
            {
                printf ("send failed\n");
                break;
            }

            if (pairingComplete)
            {
                printf ("pairing complete\n");
                break;
            }
        }
        else
        {
            printf ("malformed HTTP request\n");
            break;
       }
        free (msg);
    }
    printf ("session closed\n");
    close(clientSocket);
}

HttpReply HttpSession::processRequest (const HttpRequest& req)
{
    const char* uri = req.get_uri();
    const char* httpMethod = req.get_method();
    static int char_sz = strlen ("/characteristics");

    if (uri == 0 || httpMethod == 0)
    {
        printf("malformed request, no uri or method\n");
        return HttpReply::generate400();
    }

    if (!strcmp(uri, "/pair-setup"))
    {
        return processPairRequest(req.get_body());
    }
    else if (!strcmp(uri, "/pair-verify"))
    {
        return processVerifyRequest(req.get_body());
    }
    else if (!strcmp(uri, "/identify"))
    {
        printf("identify\n");
        return HttpReply::generate204("application/hap+json");
    }
    else if (!strcmp(uri, "/accessories"))
    {
        printf("accessory request\n");
        //const char* accessoryList = acc.getAccessoryList();
        //return HttpReply::generateOK("application/hap+json", accessoryList.getBytes());
    }
    else if (!strcmp(uri, "/characteristics") && !strcmp(httpMethod, "PUT"))
    {
        printf("characteristics put\n");
        // example input:
        // {"characteristics":[{"aid":1,"iid":6,"value":true}]} //identify
        // {"characteristics":[{"aid":2,"iid":9,"value":1}]} //on
        // {"characteristics":[{"aid":2,"iid":9,"ev":true}]} //event on for
        // 2.9

//        String in = new String(req.body);
//        if (in.contains("\"ev\""))
//        {
//            // send once immediately (don't have to do it?)
//            String body = acc.processEvent(in);
//            EventSender.sendEvent(clientSocket, body, sessionCrypto);
//
//            // schedule to send periodically
//            eventSender = new EventSender(acc, sessionCrypto, clientSocket);
//            Thread t = new Thread(eventSender);
//            t.start();
//        }
//        else
//        {
//            acc.setValue(in);
//        }

        return HttpReply::generate204("application/hap+json");
    }
    else if (!strncmp (uri, "/characteristics", char_sz) && !strcmp(httpMethod, "GET"))
    {
        printf("characteristics get\n");
        // input: ask the value of 2.9
        // GET /characteristics?id=2.9 HTTP/1.1
        // no body or content length

        // return:
        // {"characteristics":[{"value":false,"aid":2,"iid":9}]}

        //String body = acc.getValue(uri);
        //return HttpReply::generateOK("application/hap+json", body.getBytes());
    }
    else if (!strcmp(uri, "/pairings"))
    {
//        byte[] body = processPairings(req); // only remove expected here
//        return HttpReply::generateOK("application/hap+json", body);
    }
    else
    {
        printf("Uri unimplemented: %s\n", uri);
        return HttpReply::generate400();
    }

}

HttpReply HttpSession::processPairRequest(const buf& body)
{
    TLV_Decoder d;
    d.decode(body);
    int stage = d.getStage();
    if (stage == 1) // received M1
    {
        printf("invoking setup M2\n");
        // start M2
        return pairSetup.step1();
    }
    else if (stage == 3) // received M3
    {
        buf t3 = d.getData(PUBLIC_KEY); // pk
        buf t4 = d.getData(PROOF); // proof
        printf("invoking setup M4: t3 len=%d, t4 len=%d\n", t3.length, t4.length);
        // start M4
        return pairSetup.step2(t3, t4);
    }
    else if (stage == 5) // received M5
    {
        buf t5 = d.getData(ENCRYPTED_DATA);
        printf("invoking setup M6, t5 len=%d \n", t5.length);
        buf authData;
        authData.length = 16;
        authData.data = &t5.data[t5.length - 16];

        buf message;
        message.data = t5.data;
        message.length = t5.length - 16;

        // start M6
        pairingComplete = true;
        return pairSetup.step3(message, authData);
    }
    else
    {
        printf("Wrong setup/verify stage: %d\n", stage);
        return HttpReply::generate400();
    }

    return HttpReply::generate400();
}

HttpReply HttpSession::processVerifyRequest(const buf& body)
{
    TLV_Decoder d;
    d.decode(body);
    int stage = d.getStage();
    if (stage == 1) // received M1
    {
        const buf t3 = d.getData(PUBLIC_KEY); // iOS device's Curve25519 public key
        printf("invoking verify M2, t3 len=%d \n", t3.length);
        // start M2
        return PairVerify::stage1(t3);
    }
    else if (stage == 3) // received M3
    {
        const buf t5 = d.getData(ENCRYPTED_DATA);
        printf("invoking verify M4, t5 len=%d \n", t5.length);

        buf authData;
        authData.length = 16;
        authData.data = &t5.data[t5.length - 16];

        buf message;
        message.data = t5.data;
        message.length = t5.length - 16;
        // start M4
        upgraded = true;
        return PairVerify::stage2(message, authData);
        //sessionCrypto = new SessionCrypto(cfg.getWriteKey(), cfg.getReadKey());
    }
    else
    {
        printf("Wrong setup/verify stage: %d\n", stage);
        return HttpReply::generate400();
    }

    return HttpReply::generate400();
}

bool HttpSession::sendReply(buf& reply, timeval* tv)
{
    fd_set writefds;
    FD_ZERO(&writefds);
    FD_SET(clientSocket, &writefds);

    bool rc = true;
    while (running)
    {
        int rc = select(clientSocket + 1, NULL, &writefds, NULL, tv);
        if (rc > 0)
        {
            break;
        }

    }

    int total_written = 0;
    while (running)
    {
        int written = write (clientSocket, &reply.data[total_written], reply.length - total_written);
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
bool HttpSession::waitForIncomingReq(timeval* tv, uint8_t*& msg, int& msg_size)
{
    static uint8_t buf[256];
    bool rc = true;
    msg = 0;
    msg_size = 0;
    fd_set readfds;
    FD_ZERO(&readfds);
    FD_SET(clientSocket, &readfds);

    printf ("wait for req, r: %x\n", readfds.fds_bits);

    while (running)
    {
        int rc = select(clientSocket + 1, &readfds, NULL, NULL, tv);
        if (rc > 0)
        {
            break;
        }

    }

    while (running)
    {
        int read_count = read(clientSocket, &buf, sizeof(buf));
        if (read_count == -1)
        {
            printf("socket read error\n");
            rc = false;
            break;
        }
        else if (read_count == 0)
        {
            printf("0 bytes read\n");
            rc = false;
            break;
        }
        else
        {
            printf("%d bytes read\n", read_count);
            msg = (uint8_t*) realloc(msg, msg_size + read_count);
            memcpy(&msg[msg_size], buf, read_count);
            msg_size += read_count;
            if (read_count != sizeof(buf))
            {
                printf("eof message\n");
                break;
            }
        }
    }
    return rc;
}
