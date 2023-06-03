package main;


public class Lock implements Accessory
{
    String accessoryList = "{\"accessories\":[{\"aid\":1,\"services\":[{\"iid\":1,\"type\":\"3E\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Door Lock\"},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Manufacturer\",\"value\":\"none\"},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Model\",\"value\":\"none\"},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Serial number\",\"value\":\"none\"},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"description\":\"Identify\"}]},{\"iid\":7,\"type\":\"45\",\"characteristics\":[{\"iid\":8,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Doork Lock\"},{\"iid\":9,\"type\":\"1D\",\"perms\":[\"pw\",\"pr\",\"ev\"],\"format\":\"uint8\",\"description\":\"Lock Current State\",\"value\":0},{\"iid\":10,\"type\":\"1E\",\"perms\":[\"pw\",\"pr\",\"ev\"],\"format\":\"uint8\",\"description\":\"Lock Target State\",\"value\":0}]}]}]}";

    int category = 6;  
    
    @Override
    public String getAccessoryList()
    {
        return accessoryList;
    }

    @Override
    public String processEvent(String in)
    {
        int aid = 2;
        // event: push the value of 2.9
        return "{\"characteristics\":[{\"aid\":" + aid + ",\"iid\":9,\"value\":true}]}";
    }

    @Override
    public String getValue(String val)
    {
        if (val.contains("9"))
            return  "{\"characteristics\":[{\"value\":0,\"aid\":2,\"iid\":9}]}";
        else
            return  "{\"characteristics\":[{\"value\":0,\"aid\":2,\"iid\":10}]}";
            
    }

    @Override
    public int getCategory()
    {
        return category;
    }

    @Override
    public String getName()
    {
        return "Door Lock";
    }

    @Override
    public void setValue(String val)
    {
    	System.out.println ("set value: " + val);
    }

}
