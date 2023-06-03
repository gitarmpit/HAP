package cli;

//Thermostat
public class AccessoryParser2
{
    String accList; 
    int lastPos = 0;
    String aid; 
    String iid;
    String type;
    
    String tmp;

    String currentTempId = "";
    String targetTempId = "";
    String currentStateId = "";
    String targetStateId = "";
    String displayUnitsId = "";
    
    String thermostat_aid;
    
    boolean found;
    
    public AccessoryParser2(String accList)
    {
        this.accList = accList;
        aid = null;
        iid = null;
    }
    
    public String getAid() 
    {
    	return thermostat_aid;
    }

    public String getCurrentTempId()
    {
        return currentTempId;
    }
    
    public String getTargetTempId()
    {
        return targetTempId;
    }
    
    public String getCurrentStateId() 
    {
    	return currentStateId;
    }
    public String getTargetStateId() 
    {
    	return targetStateId;
    }
    public String getDisplayUnitsId() 
    {
    	return displayUnitsId;
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
                if (type.equals("4A"))  //Thermostat
                {
                	thermostat_aid = aid;
                    //System.out.println ("Thermostat service: " + iid);
                }
                else if (type.equals("11") && aid != null && iid != null)
                {
                    //System.out.println ("Current temp characterstic: " + iid);
                    currentTempId = iid;
                }
                else if (type.equals("35") && aid != null && iid != null)
                {
                    //System.out.println ("Target temp characterstic: " + iid);
                    targetTempId = iid;
                }
                else if (type.equalsIgnoreCase("0F") && aid != null && iid != null)
                {
                    //System.out.println ("Current state: " + iid);
                    currentStateId = iid;
                }
                else if (type.equals("33") && aid != null && iid != null)
                {
                    //System.out.println ("Target state: " + iid);
                    targetStateId = iid;
                }
                else if (type.equals("36") && aid != null && iid != null)
                {
                    //System.out.println ("Target state: " + iid);
                    displayUnitsId = iid;
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
