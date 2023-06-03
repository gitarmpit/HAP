package http;

import java.util.function.Consumer;

public interface HomekitClientConnectionFactory {

    HomekitClientConnection createConnection(Consumer<HttpResponse> outOfBandMessageCallback);
    
}
