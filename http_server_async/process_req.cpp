#include <stdio.h>
#include "process_req.h"
#include "common.h"

ProcessReq::ProcessReq() 
{
}

bool ProcessReq::process(const buf& msg_in, buf& msg_out)
{
   int ret = processPlainText (msg_in, msg_out);

   //incomplete request
   if (ret == 1)
   {
       return false;
   } 

   return true;

}

int ProcessReq::processPlainText(const buf& msg_in, buf& msg_out) 
{
   req.addChunk (msg_in);
   bool expect_content_length = true; 
   int ret = req.parse(expect_content_length);
   if (ret == -1) 
   {
      printf ("malformed HTTP request\n");
      return -1;
   }
   else if (ret == 1) 
   {
      printf ("incomplete HTTP request\n");
      return 1;
   }

   printf ("Plaintext request content-length: %d\n", req.getContentLength());   

   printf ("Request headers:\n%s\n", req.get_headers());

   reply = processRequest(req);
   printf("\nPlaintext reply: %d bytes\n", reply.get_msg().length);
   printf ("Reply headers: %s", reply.get_hdr());

   msg_out = reply.get_msg();
   return 0;
}


HttpReply ProcessReq::processRequest (const HttpRequest& req)
{
    const char* uri = req.get_uri();
    const char* httpMethod = req.get_method();

    printf ("uri: %s, method: %s\n", uri, httpMethod);

    char body[] = "<html><body>Hello!</body></html>";
    return HttpReply::generateOK("text/html", body);
}

