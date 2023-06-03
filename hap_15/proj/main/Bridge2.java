package main;

public class Bridge2 implements Accessory
{
    //2 bulbs
    String accessoryList = "{\"accessories\":[{\"aid\":1,\"services\":[{\"iid\":1,\"type\":\"3E\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Test Bridge\"},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Manufacturer\",\"value\":\"Test Bridge Inc\"},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Model\",\"value\":\"model\"},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Serial\",\"value\":\"111\"},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"description\":\"Identify\"}]}]},{\"aid\":2,\"services\":[{\"iid\":1,\"type\":\"3E\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Bulb1\"},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Manufacturer\",\"value\":\"none\"},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Model\",\"value\":\"none\"},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Serial\",\"value\":\"none\"},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"description\":\"Identify\"}]},{\"iid\":7,\"type\":\"43\",\"characteristics\":[{\"iid\":8,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Bulb1\"},{\"iid\":9,\"type\":\"25\",\"perms\":[\"pw\",\"pr\",\"ev\"],\"format\":\"bool\",\"description\":\"Turn on and off\",\"value\":false}]}]},{\"aid\":3,\"services\":[{\"iid\":1,\"type\":\"3E\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Bulb2\"},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Manufacturer\",\"value\":\"none\"},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Model\",\"value\":\"none\"},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Serial\",\"value\":\"none\"},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"description\":\"Identify\"}]},{\"iid\":7,\"type\":\"43\",\"characteristics\":[{\"iid\":8,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Bulb2\"},{\"iid\":9,\"type\":\"25\",\"perms\":[\"pw\",\"pr\",\"ev\"],\"format\":\"bool\",\"description\":\"Turn on and off\",\"value\":false}]}]}]}";

    int category = 2;  
    
    @Override
    public String getName()
    {
        return "Bridge2";
    }

    @Override
    public String getAccessoryList()
    {
        return accessoryList;
    }

    @Override
    public String processEvent(String in)
    {
        int aid;
        if (in.contains(":2"))
        {  
            aid = 2;
        }
        else if (in.contains(":3"))
        {  
            aid = 3;
        }
        else 
        {
            aid = 3;
        }
        return "{\"characteristics\":[{\"aid\":" + aid + ",\"iid\":9,\"value\":true}]}";
    }

    @Override
    public String getValue(String uri)
    {
        String body = "";
        if (uri.contains(","))  //two bulbs
        {
            body = "{\"characteristics\":[{\"value\":true,\"aid\":2,\"iid\":9},{\"value\":true,\"aid\":3,\"iid\":9}]}";
        }
        else if (uri.contains("2.9"))
        {
            body = "{\"characteristics\":[{\"value\":true,\"aid\":2,\"iid\":9}]}";
        }
        else if (uri.contains("3.9"))
        {
            body = "{\"characteristics\":[{\"value\":true,\"aid\":3,\"iid\":9}]}";
        }
        return body;
    }

    @Override
    public int getCategory()
    {
        return category;
    }

    @Override
    public void setValue(String uri)
    {
        // TODO Auto-generated method stub
        
    }

}
