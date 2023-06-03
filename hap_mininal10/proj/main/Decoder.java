package main;

public class Decoder
{
    private int ptr = 0;
    private byte[] t1 = null; //name
    private byte[] t3 = null; // public key
    private byte[] t4 = null; // proof
    private byte[] t5 = null; // encrypted sub-TLV
    private byte[] t10 = null; // signature
    private int stage = 0;

    public Decoder()
    {

    }

    private byte[] add(byte[] to, byte[] from)
    {
	if (to == null)
	{
	    return from;
	} else
	{
	    byte[] merge = new byte[to.length + from.length];
	    for (int i = 0; i < to.length; ++i)
	    {
		merge[i] = to[i];
	    }
	    for (int i = 0; i < from.length; ++i)
	    {
		merge[i + to.length] = from[i];
	    }

	    return merge;
	}
    }
    
    public void decode(byte[] b)
    {
	while (ptr < b.length)
	{
	    int type = b[ptr++];
	    int len = ((char) b[ptr++]) & 0x00ff;
	    byte[] tmp = new byte[len];
	    for (int i = 0; i < len; ++i)
	    {
		tmp[i] = b[ptr + i];
	    }

	    if (type == 6) // stage
	    {
		stage = tmp[0];
	    } 
	    else if (type == 1)
	    {
		t1 = add(t1, tmp);
	    } 
	    else if (type == 3)
	    {
		t3 = add(t3, tmp);
	    } 
	    else if (type == 4)
	    {
		t4 = add(t4, tmp);
	    } 
	    else if (type == 5)
	    {
		t5 = add(t5, tmp);
	    } 
	    else if (type == 10)
	    {
		t10 = add(t10, tmp);
	    }

	    System.out.println("parse type: " + type + ", len=" + len + ", stage: " + stage);
	    ptr += len;
	}

    }
    
    public int getStage() 
    {
	return stage;
    }
    
    public byte[] getData(int type) 
    {
	if (type == 1)
	{
	    return t1;
	} 
	else if (type == 3)
	{
	    return t3;
	} 
	else if (type == 4)
	{
	    return t4;
	} 
	else if (type == 5)
	{
	    return t5;
	} 
	else if (type == 10)
	{
	    return t10;
	}
	else 
	{
	    return null;
	}
	
    }

}
