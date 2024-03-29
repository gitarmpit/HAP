package main;

public class HttpRequest
{
    String uri = "";
    byte[] body;
    String method = "";
    int lastRead = 0;

    public HttpRequest(byte[] b)
    {
        body = new byte[0];
        parseMethod(b);
        parseUri(b);
        int contentLength = getContentLength(b);
        if (contentLength != -1)
        {
            int eoh = find("\r\n\r\n", b, lastRead);
            body = new byte[contentLength];
            if (eoh != -1)
            {
                for (int i = 0; i < contentLength; ++i)
                {
                    body[i] = b[eoh + i];
                }
            }
        }
    }

    private void parseUri(byte[] buf)
    {
        int rc = find(" ", buf, 0);
        if (rc == -1)
        {
            return;
        }
        for (int i = rc; i < buf.length; ++i)
        {
            if (buf[i] == ' ')
            {
                break;
            }
            uri += (char) buf[i];
        }

        return;
    }

    private void parseMethod(byte[] buf)
    {
        for (int i = 0; i < buf.length; ++i)
        {
            if (buf[i] == ' ')
            {
                break;
            }

            method += (char) buf[i];
        }

        return;
    }

    private int getContentLength(byte[] buf)
    {
        int rc = find("Content-Length:", buf, 0);
        if (rc == -1)
        {
            return -1;
        }
        if (buf[rc] == ' ')
        {
            ++rc;
        }
        String n = "";
        for (int i = rc; i < buf.length; ++i)
        {
            if (buf[i] < '0' || (buf[i] > '9'))
            {
                break;
            }
            n += (char) buf[i];
        }
        lastRead = rc;
        return Integer.parseInt(n);

    }

    public static int find(String ssubstr, byte[] str, int start)
    {
        int rc = -1;
        byte[] substr = ssubstr.getBytes();

        int strpos = 0;
        for (int i = start; i < str.length; ++i)
        {
            if (str[i] != substr[0])
            {
                continue;
            }
            strpos = i;
            int subpos = 0;
            while (true)
            {
                if (subpos == substr.length)
                {
                    return strpos;
                }
                if (str[strpos++] != substr[subpos++])
                {
                    break;
                }

            }
        }

        return rc;
    }

    public String getUri()
    {
        return uri;
    }

    public byte[] getBody()
    {
        return body;
    }

    public String getMethod()
    {
        return method;
    }

}
