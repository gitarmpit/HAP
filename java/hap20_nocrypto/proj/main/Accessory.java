package main;

public interface Accessory
{
    String getName();
    int getCategory();
    String getAccessoryList();
    String processEvent(String in);
    String getValue (String uri);
    void setValue(String uri);
}
