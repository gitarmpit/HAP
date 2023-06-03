package pairing;

import impl.*;
import http.*;

public class PairingManager {

    
    private final HomekitAuthInfo authInfo;
    
    private SrpHandler srpHandler;

    private String label;
    
    public PairingManager(HomekitAuthInfo authInfo) {
        this.authInfo = authInfo;
        label = "Home Bridge";
    }

    public HttpResponse handle(HttpRequest httpRequest) throws Exception {
        PairSetupRequest req = PairSetupRequest.of(httpRequest.getBody());
        
        if (req.getStage() == Stage.ONE)  {
            System.out.println("Starting pair for " + label);
            srpHandler = new SrpHandler(authInfo.getPin(), authInfo.getSalt());
            return srpHandler.handle(req);
        } else if (req.getStage() == Stage.TWO) {
            System.out.println("Entering second stage of pair for " + label);
            if (srpHandler == null) {
                System.out.println("Received unexpected stage 2 request for " + label);
                return new UnauthorizedResponse();
            } else {
                try {
                    return srpHandler.handle(req);
                } catch (Exception e) {
                    srpHandler = null; //You don't get to try again - need a new key
                    System.out.println("Exception encountered while processing pairing request: " + e.getMessage());
                    return new UnauthorizedResponse();
                }
            }
        } else if (req.getStage() == Stage.THREE) {
            System.out.println("Entering third stage of pair for " + label);
            if (srpHandler == null) {
                System.out.println("Received unexpected stage 3 request for " + label);
                return new UnauthorizedResponse();
            } else {
                FinalPairHandler handler = new FinalPairHandler(srpHandler.getK(), authInfo);
                try {
                    return handler.handle(req);
                } catch (Exception e) {
                    System.out.println("Exception while finalizing pairing: " + e.getMessage());
                    return new UnauthorizedResponse();
                }
            }
        } 
        
        return new NotFoundResponse();
    }
}
