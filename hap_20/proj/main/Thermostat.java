package main;


public class Thermostat implements Accessory
{
    String accessoryList = "{\"accessories\":[{\"aid\":1,\"services\":[{\"iid\":1,\"type\":\"3E\",\"characteristics\":[{\"iid\":2,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Thermostat\"},{\"iid\":3,\"type\":\"20\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Manufacturer\",\"value\":\"none\"},{\"iid\":4,\"type\":\"21\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Model\",\"value\":\"none\"},{\"iid\":5,\"type\":\"30\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Serial number\",\"value\":\"none\"},{\"iid\":6,\"type\":\"14\",\"perms\":[\"pw\"],\"format\":\"bool\",\"description\":\"Identify\"}]},{\"iid\":7,\"type\":\"4A\",\"characteristics\":[{\"iid\":8,\"type\":\"23\",\"perms\":[\"pr\"],\"format\":\"string\",\"description\":\"Name\",\"value\":\"Thermostat\"},{\"iid\":9,\"type\":\"0F\",\"perms\":[\"pr\",\"ev\"],\"format\":\"uint8\",\"description\":\"Current Heating Cooling State\",\"value\":2},{\"iid\":10,\"type\":\"33\",\"perms\":[\"pr\",\"ev\"],\"format\":\"uint8\",\"description\":\"Target Heating Cooling State\",\"value\":2},{\"iid\":11,\"type\":\"11\",\"perms\":[\"pr\",\"ev\"],\"format\":\"float\",\"description\":\"Current Temp\",\"value\":22.3},{\"iid\":12,\"type\":\"35\",\"perms\":[\"pr\",\"ev\"],\"format\":\"float\",\"description\":\"Target Temp\",\"value\":22.3},{\"iid\":13,\"type\":\"36\",\"perms\":[\"pr\",\"ev\"],\"format\":\"uint8\",\"description\":\"Temp display units\",\"value\":0}]}]}]}";

    int category = 9;  
    
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
        return "Thermostat1";
    }

    @Override
    public void setValue(String val)
    {
    	System.out.println ("set value: " + val);
    }

}
