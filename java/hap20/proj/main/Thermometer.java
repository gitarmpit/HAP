package main;

public class Thermometer implements Accessory
{
    //9 temp, 10 isActive
    String accessoryList = "{\"accessories\":[{\"aid\":1,\"services\":[{\"iid\":1,\"type\":\"3E\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Thermometer\"},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Manufacturer\",\"value\":\"none\"},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Model\",\"value\":\"none\"},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Serial number\",\"value\":\"none\"},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"description\":\"Identify\"}]},{\"iid\":7,\"type\":\"8A\",\"characteristics\":[{\"iid\":8,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Thermometer\"},{\"iid\":9,\"type\":\"11\",\"perms\":[\"pr\",\"ev\"],\"format\":\"float\",\"description\":\"Temp in C\",\"value\":22.3},{\"iid\":10,\"type\":\"75\",\"perms\":[\"pr\",\"ev\"],\"format\":\"bool\",\"description\":\"Is Active\",\"value\":true}]}]}]}";

    int category = 10; //sensor 
    double  tempC = 22.7;
    
    @Override
    public String getName()
    {
        return "Thermometer";
    }
    
    
    @Override
    public String getAccessoryList()
    {
        return accessoryList;
    }

    @Override
    public String processEvent(String in)
    {
        tempC += 1.;
        return "{\"characteristics\":[{\"value\":" + tempC + ",\"aid\":1,\"iid\":9}]}";
    }

    @Override
    public String getValue(String uri)
    {
        if (uri.contains(","))
        {
            return "{\"characteristics\":[{\"value\":" + tempC + ",\"aid\":1,\"iid\":9},{\"value\":true,\"aid\":1,\"iid\":10}]}";
        }
        else if (uri.contains("9"))
        {
            return "{\"characteristics\":[{\"value\":" + tempC + ",\"aid\":1,\"iid\":9}]}";
        }
        else 
        {
            return "{\"characteristics\":[{\"value\":true,\"aid\":1,\"iid\":10}]}";
        }
    }

    @Override
    public int getCategory()
    {
        return category;
    }


    @Override
    public void setValue(String val)
    {
        // TODO Auto-generated method stub
        
    }

}
