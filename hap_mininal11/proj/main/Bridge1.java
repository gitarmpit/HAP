package main;


public class Bridge1 implements Accessory
{
    // Bridge
    String accessoryList = "{\"accessories\":[{\"aid\":1,\"services\":[{\"iid\":1,\"type\":\"3E\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Test Bridge\"},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Manufacturer\",\"value\":\"Test Bridge Inc\"},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Model\",\"value\":\"model\"},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Serial\",\"value\":\"111\"},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"description\":\"Identify\"}]}]},{\"aid\":2,\"services\":[{\"iid\":1,\"type\":\"3E\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Test LightBulb\"},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Manufacturer\",\"value\":\"none\"},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Model\",\"value\":\"none\"},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Serial\",\"value\":\"none\"},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"description\":\"Identify\"}]},{\"iid\":7,\"type\":\"43\",\"characteristics\":[{\"iid\":8,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Test Lightbulb\"},{\"iid\":9,\"type\":\"25\",\"perms\":[\"pw\",\"pr\",\"ev\"],\"format\":\"bool\",\"description\":\"Turn on and off\",\"value\":false}]}]}]}";

    int category = 2;  
    
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
    public String getValue(String uri)
    {
        return  "{\"characteristics\":[{\"value\":true,\"aid\":2,\"iid\":9}]}";
    }

    @Override
    public int getCategory()
    {
        return category;
    }

    @Override
    public String getName()
    {
        return "Bridge1";
    }

    @Override
    public void setValue(String uri)
    {
        // TODO Auto-generated method stub
        
    }

}
