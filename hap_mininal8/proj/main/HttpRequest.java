package main;

public interface HttpRequest {

    String getUri();
    byte[] getBody();
    //HttpMethod getMethod();
    
}
