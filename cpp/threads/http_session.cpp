#include "http_session.h"
#include "tlv.h"
#include "common.h"
#include "pair_setup.h"
#include "pair_verify.h"



HttpSession::HttpSession(SOCKET clientSocket)
{
    running = true;
    upgraded = false;
    pairingComplete = false;
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

    buf raw_in;
    raw_in.data = 0;
    raw_in.length = 0;
    buf msg_in;
    bool encrypt = false;
    while (running)
    {
        printf ("start waiting\n");
        if (!waitForIncomingReq(&tv, raw_in))
        {
            printf ("wait failed\n");
            break;
        }

        printf("Raw request len: %d bytes\n", raw_in.length);

        if (upgraded)
        {
           msg_in = sessionCrypto.decrypt(raw_in);
           encrypt = true;
        }
        else 
        {
            msg_in = raw_in;
        }


        HttpRequest req;
        bool rc = req.parse(msg_in);
        if (rc)
        {
            printf ("Request headers:\n%s\n", req.get_headers());
            if (encrypt)
            {
               buf body = req.get_body();
               printf("decrypted request len: %d bytes\n", body.length);
               for (int i = 0; i < body.length; ++i )
               {
                    printf ("%c", body.data[i]);
               }
               printf ("\n");
            }

            HttpReply reply = processRequest(req);

            printf("\nPlaintext reply: %d bytes\n", reply.get_msg().length);
            printf ("Reply headers: %s\n", reply.get_hdr());
            if (encrypt)
            {
               for (int i = 0; i < reply.get_body().length; ++i )
               {
                    printf ("%c", reply.get_body().data[i]);
               }
               printf ("\n");
               buf msg = sessionCrypto.encrypt(reply.get_msg());
               printf("\nEncrypted reply: %d bytes\n\n", msg.length);
               if (!sendReply(msg, &tv))
               {
                   printf ("send failed\n");
                   break;
               }
            }
            else 
            {
                if (!sendReply(reply.get_msg(), &tv))
                {
                    printf ("send failed\n");
                    break;
                }
            }

            printf ("\n===================\n");

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
       free (raw_in.data);
       raw_in.data = 0;
    }
    free (raw_in.data);
    printf ("session closed\n");
    _close_socket(clientSocket);
}

HttpReply HttpSession::processRequest (const HttpRequest& req)
{
    const char* uri = req.get_uri();
    const char* httpMethod = req.get_method();
    static int char_sz = strlen ("/characteristics");

    if (uri == 0 || httpMethod == 0)
    {
        printf("process: malformed request, no uri or method\n");
       return HttpReply::generate400();
    }
    else if (!strcmp(uri, "/pair-setup"))
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
        static const char* a = "accessory list";
        return HttpReply::generateOK("application/hap+json", a);
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
        return HttpReply::generateOK("application/hap+json", "{\"characteristics\":[{\"value\":false,\"aid\":2,\"iid\":9}]}");
    }
    else if (!strcmp(uri, "/pairings"))
    {
//        byte[] body = processPairings(req); // only remove expected here
          buf body; //TODO
          return HttpReply::generateOK("application/hap+json", body);
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
        return PairSetup::step1();
    }
    else if (stage == 3) // received M3
    {
        buf t3 = d.getData(PUBLIC_KEY); // pk
        buf t4 = d.getData(PROOF); // proof
        printf("invoking setup M4: t3 len=%d, t4 len=%d\n", t3.length, t4.length);
        // start M4
        return PairSetup::step2(t3, t4);
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
        return PairSetup::step3(message, authData);
    }
    else
    {
        printf("Wrong setup/verify stage: %d\n", stage);
        return HttpReply::generate400();
    }

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

}

bool HttpSession::sendReply(const buf& reply, timeval* tv)
{
//    fd_set writefds;
//    FD_ZERO(&writefds);
//    FD_SET(clientSocket, &writefds);

    bool rc = true;
//    while (running)
//    {
//        int rc = select(clientSocket + 1, NULL, &writefds, NULL, tv);
//        if (rc > 0)
//        {
//            break;
//        }
//
//    }

    int total_written = 0;
    while (running)
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

    while (running)
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

    while (running)
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

//remove pairing when deleting accessory from controller
//byte[] processPairings(HttpRequest req) throws Exception
//{
//    TLV_Decoder d = new TLV_Decoder();
//    d.decode(req.body);
//    int pairingMethod = d.getPairingMethod();
//    String userName = null;
//    if (d.getData(1) != null)
//    {
//        userName = new String(d.getData(1), StandardCharsets.UTF_8);
//    }
//
//    System.out.printf("/pairings: stage = %d, method = %d, %s\n", d.getStage(), pairingMethod, userName);
//
//    //remove pairings
//    if (pairingMethod == 4 && userName != null)
//    {
//        cfg.removeIosUser(userName);
//        adv.setDiscoverable(true);
//
//        if (eventSender != null)
//        {
//            eventSender.stop();
//            eventSender = null;
//        }
//
//        running = false;
//    }
//
//    TLV_Encoder encoder = new TLV_Encoder();
//    encoder.add(MessageType.STATE.getKey(), (short) 2);
//    return encoder.toByteArray();
//
// }
