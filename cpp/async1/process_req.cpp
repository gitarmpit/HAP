#include <stdio.h>
#include "process_req.h"
#include "common.h"
#include "tlv.h"
#include "pair_setup.h"
#include "pair_verify.h"

ProcessReq::ProcessReq() 
{
    msg_out.data = 0;
    reset();
}

void ProcessReq::reset() 
{
    pairingComplete = false;
    upgraded = false;
    free (msg_out.data);
    msg_out.data = 0;
    msg_out.length = 0;

}

buf& ProcessReq::process(const buf& raw_in) 
{
   buf msg_in;
   bool encrypt = false;

   if (isUpgraded())
   {
       msg_in = sessionCrypto.decrypt(raw_in);
       encrypt = true;
   }
   else 
   {
       msg_in = raw_in;
   }

   free (msg_out.data);

   HttpRequest req;
   bool rc = req.parse(msg_in);
   if (!rc )
   {
      printf ("malformed HTTP request\n");
      msg_out = HttpReply::generate400().get_msg();
      return msg_out;
   }

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
   buf tmp;
   printf("\nPlaintext reply: %d bytes\n", reply.get_msg().length);
   printf ("Reply headers: %s", reply.get_hdr());
   if (encrypt)
   {
      for (int i = 0; i < reply.get_body().length; ++i )
      {
          printf ("%c", reply.get_body().data[i]);
      }
      printf ("\n");
      tmp = sessionCrypto.encrypt(reply.get_msg());
      printf("\nEncrypted reply: %d bytes\n\n", tmp.length);
   }
   else 
   {
       tmp = reply.get_msg();     
   }
   

   if (isPairingComplete())
   {
        printf ("pairing complete\n");
   }

   msg_out.data = (uint8_t*) malloc (tmp.length); //memory ownership
   memcpy (msg_out.data, tmp.data, tmp.length);
   msg_out.length = tmp.length;
   
   return msg_out; 
}


HttpReply ProcessReq::processRequest (const HttpRequest& req)
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

HttpReply ProcessReq::processPairRequest(const buf& body)
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

HttpReply ProcessReq::processVerifyRequest(const buf& body)
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
