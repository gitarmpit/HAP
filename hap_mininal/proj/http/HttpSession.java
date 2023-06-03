package http;


import pairing.*;
import impl.HomekitAuthInfo;

import java.io.IOException;
import java.net.InetAddress;
import adv.Adv;

class HttpSession {
    
    private volatile PairingManager pairingManager;
    private volatile PairVerificationManager pairVerificationManager;
    
    //private volatile AccessoryController accessoryController;
    //private volatile CharacteristicsController characteristicsController;

    private final HomekitAuthInfo authInfo;
    private final HomekitClientConnection connection;  //only need for PUT characteristic
    
    
    public HttpSession(HomekitAuthInfo authInfo,
            HomekitClientConnection connection) 
    {
        this.authInfo = authInfo;
        this.connection = connection;
    }

    public HttpResponse handleRequest(HttpRequest request) throws IOException {
        switch(request.getUri()) {
        case "/pair-setup":
            return handlePairSetup(request);
            
        case "/pair-verify":
            return handlePairVerify(request);
            
        default:
//          if (registry.isAllowUnauthenticatedRequests()) {
//              return handleAuthenticatedRequest(request);
//          } else {
            {
                System.out.println("Unrecognized request for "+request.getUri());
                return new NotFoundResponse();
            }
        }
    }
    
    public HttpResponse handleAuthenticatedRequest(HttpRequest request) throws IOException {
        try {
            switch(request.getUri()) {
            case "/accessories":
            {
                //return getAccessoryController().listing();
                System.out.println ("/accessories");
                return new NotFoundResponse();
            }   
            case "/characteristics":
                switch(request.getMethod()) {
                case PUT:
                {
                        System.out.println ("/characteristics PUT");
                        return new NotFoundResponse();
                    //return getCharacteristicsController().put(request, connection);
                }   
                default:
                    System.out.println("Unrecognized method for "+request.getUri());
                    return new NotFoundResponse();
                }
                
            case "/pairings":
            {
                System.out.println ("/pairings");
                return new NotFoundResponse();
            //  return new PairingUpdateController(authInfo, advertiser).handle(request);
            }   
            default:
                if (request.getUri().startsWith("/characteristics?")) 
                {
                    System.out.println ("/characteristics?");
                    return new NotFoundResponse();
                    //return getCharacteristicsController().get(request);
                }
                System.out.println("Unrecognized request for "+request.getUri());
                return new NotFoundResponse();
            }
        } catch (Exception e) {
            System.out.println("Could not handle request: " + e.getMessage());
            return new InternalServerErrorResponse(e);
        }
    }
        
    private HttpResponse handlePairSetup(HttpRequest request) {
        if (pairingManager == null) {
            synchronized(HttpSession.class) {
                if (pairingManager == null) {
                    pairingManager = new PairingManager(authInfo);
                }
            }
        }
        try {
            return pairingManager.handle(request);
        } catch (Exception e) {
            System.out.println("Exception encountered during pairing: " + e.getMessage());
            return new InternalServerErrorResponse(e);
        }
    }
    
    private HttpResponse handlePairVerify(HttpRequest request) {
        if (pairVerificationManager == null) {
            synchronized(HttpSession.class) {
                if (pairVerificationManager == null) {
                    pairVerificationManager = new PairVerificationManager(authInfo);
                }
            }
        }
        try {
            return pairVerificationManager.handle(request);
        } catch (Exception e) {
            System.out.println("Excepton encountered while verifying pairing: " + e.getMessage());
            return new InternalServerErrorResponse(e);
        }
    }
    
//  private synchronized AccessoryController getAccessoryController() {     
//      if (accessoryController == null) {
//          accessoryController = new AccessoryController(registry);
//      }
//      return accessoryController;
//  }
    
//  private synchronized CharacteristicsController getCharacteristicsController() {
//      if (characteristicsController == null) {
//          characteristicsController = new CharacteristicsController(registry, subscriptions);
//      }
//      return characteristicsController;
//  }
    
//  public static class SessionKey {
//      private final InetAddress address;
//      private final HomekitAccessory accessory;
//      
//      public SessionKey(InetAddress address, HomekitAccessory accessory) {
//          this.address = address;
//          this.accessory = accessory;
//      }
//      
//      @Override
//      public boolean equals(Object obj) {
//          if (obj instanceof SessionKey) {
//              return address.equals(((SessionKey) obj).address) && 
//                      accessory.equals(((SessionKey) obj).accessory);
//          } else {
//              return false;
//          }
//      }
//      
//      @Override
//      public int hashCode() {
//          int hash = 1;
//          hash = hash * 31 + address.hashCode();
//          hash = hash * 31 + accessory.hashCode();
//          return hash;
//      }
//  }

}
