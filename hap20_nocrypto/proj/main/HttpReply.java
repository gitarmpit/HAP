package main;

public class HttpReply
{
    public int status;
    public byte[] msg;  //complete response
    public byte[] hdr;
    public byte[] body;
    public int bodyLength;
    
    public static HttpReply generateOK(String contentType, byte[] body) 
    {
        HttpReply reply = new HttpReply();
        reply.status = 200;
        reply.bodyLength = body.length;
        String tmp = "HTTP/1.1 200 OK\r\n";
        tmp += "Content-type: " + contentType + "\r\n"; //application/hap+json
        tmp += "Content-Length: " + body.length + "\r\n";
        tmp += "Connection: keep-alive\r\n\r\n";
        reply.hdr = tmp.getBytes();
        reply.body = body;
        
        reply.msg = new byte[reply.hdr.length + body.length];
        System.arraycopy(reply.hdr, 0, reply.msg, 0, reply.hdr.length);
        System.arraycopy(body, 0, reply.msg, reply.hdr.length, body.length);
        return reply;
    }
   
    public static HttpReply generate204(String contentType) 
    {
        HttpReply reply = new HttpReply();
        reply.status = 204;
        reply.bodyLength = 0;
        String tmp = "HTTP/1.1 204 No Content\r\n";
        tmp += "Content-type: " + contentType + "\r\n"; //application/hap+json
        tmp += "Content-Length: 0\r\n";
        tmp += "Connection: keep-alive\r\n\r\n";
        reply.msg = tmp.getBytes();
        return reply;
    }
    
    public static HttpReply generateAuthError (int stage)
    {
        TLV_Encoder encoder = new TLV_Encoder();
        encoder.add(MessageType.STATE.getKey(), (short) stage);
        encoder.add(MessageType.ERROR.getKey(), (short) 2); //2=auth err

        return generateOK ("application/hap+json", encoder.toByteArray());
        
    }
    
    public static HttpReply generate400() 
    {
        HttpReply reply = new HttpReply();
        reply.status = 204;
        reply.bodyLength = 0;
        reply.msg = "HTTP/1.1 400 Bad Request\r\n\r\n".getBytes();
        reply.hdr = reply.msg;
        return reply;
    }
}
