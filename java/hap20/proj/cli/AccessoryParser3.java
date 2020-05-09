package cli;

//Lock
public class AccessoryParser3
{
    String accList; 
    int lastPos = 0;
    String aid; 
    String iid;
    String type;
    
    String tmp;
    
    String currentStateId;
    String targetStateId;

    boolean found;
    
    String lock_aid;
    
    public AccessoryParser3(String accList)
    {
        this.accList = accList;
        aid = null;
        iid = null;
    }

    public String getCurrentStateId() 
    {
    	return currentStateId;
    }
    public String getTargetStateId() 
    {
    	return targetStateId;
    }
    
    public String getAid() 
    {
    	return lock_aid;
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
                if (type.equals("45"))
                {
                    System.out.println ("Lock service: " + aid + "." + iid);
                    lock_aid = aid;
                }
                else if (type.equalsIgnoreCase("1D") && aid != null && iid != null)
                {
                    System.out.println ("LockCurrentState: " + aid + "." + iid);
                    currentStateId = iid;
                }
                else if (type.equalsIgnoreCase("1E") && aid != null && iid != null)
                {
                    System.out.println ("LockTargetState: " + aid + "." + iid);
                    targetStateId = iid;
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
