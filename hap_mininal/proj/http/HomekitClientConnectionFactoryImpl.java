package http;

import impl.HomekitAuthInfo;

import java.util.function.Consumer;

public class HomekitClientConnectionFactoryImpl implements HomekitClientConnectionFactory{

//    private final HomekitAuthInfo authInfo;
//    private final HomekitRegistry registry;
//    private final SubscriptionManager subscriptions;
//    private final JmdnsHomekitAdvertiser advertiser;
    
    private HomekitAuthInfo authInfo;
    
//    public HomekitClientConnectionFactoryImpl(HomekitAuthInfo authInfo,
//            HomekitRegistry registry, SubscriptionManager subscriptions, JmdnsHomekitAdvertiser advertiser) {
//        this.registry = registry;
//        this.authInfo = authInfo;
//        this.subscriptions = subscriptions;
//        this.advertiser = advertiser;
//    }
    
    public HomekitClientConnectionFactoryImpl(HomekitAuthInfo authInfo) 
    {
	this.authInfo = authInfo;
    }
    
    @Override
    public HomekitClientConnection createConnection(Consumer<HttpResponse> outOfBandMessageCallback) {
        return new ConnectionImpl(authInfo);
        //return new ConnectionImpl(authInfo, registry, outOfBandMessageCallback, subscriptions, advertiser);
    }

    
    
}
