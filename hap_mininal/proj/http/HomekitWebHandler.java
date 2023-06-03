package http;

import java.util.concurrent.CompletableFuture;


public interface HomekitWebHandler {

    CompletableFuture<Integer> start(HomekitClientConnectionFactory clientConnectionFactory);
    
    void stop();

    void resetConnections();
    
}
