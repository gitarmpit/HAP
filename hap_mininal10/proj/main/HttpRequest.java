package main;

public interface HttpRequest {

    String getUri();
    byte[] getBody();
    String getMethod();
    
}
