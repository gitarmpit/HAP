package cli;


public class AccessoryParser
{
    String accList; 
    int lastPos = 0;
    String aid; 
    String iid;
    String type;
    
    String tmp;

    String tempId;
    boolean found;
    
    public AccessoryParser(String accList)
    {
        this.accList = accList;
        aid = null;
        iid = null;
        tempId = null;
    }

    public String getTempId()
    {
        return tempId;
    }
    
    public void scan()
    {
        do
        {
            found = false;
            if (findProp("aid\":", ","))
            {
                aid = tmp;
            }
            if (findProp("iid\":", ","))
            {
                iid = tmp;
            }
            if (findProp("type\":\"", "\""))
            {
                type = tmp;
                if (type.equals("8A"))
                {
                    System.out.println ("Temp sensor service: " + aid + "." + iid);
                }
                else if (type.equals("11") && aid != null && iid != null)
                {
                    System.out.println ("Current temp characterstic: " + aid + "." + iid);
                    tempId = aid + "." + iid;
                }
                
            }
        } while (found);
        
    }
    
    
    private boolean findProp(String prop, String end)
    {
        int pos = accList.indexOf(prop, lastPos);
        if (pos != -1)
        {
            lastPos = pos + prop.length();
            tmp = accList.substring(lastPos, accList.indexOf(end, lastPos));
            found = true;
            return true;
        }
        return false;
    }

}
