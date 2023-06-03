package pairing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import pairing.TypeLengthValueUtils.DecodeResult;
import impl.HomekitAuthInfo;
import http.*;

public class PairingUpdateController {

    private final HomekitAuthInfo authInfo;
    
    public PairingUpdateController(HomekitAuthInfo authInfo) {
        this.authInfo = authInfo;
    }

    public HttpResponse handle(HttpRequest request) throws IOException {
        DecodeResult d = TypeLengthValueUtils.decode(request.getBody());
        
        int method = d.getByte(MessageType.METHOD);
        if (method == 3) { //Add pairing
            byte[] username = d.getBytes(MessageType.USERNAME);
            byte[] ltpk = d.getBytes(MessageType.PUBLIC_KEY);
            authInfo.createUser(authInfo.getMac()+new String(username, StandardCharsets.UTF_8), ltpk);
        } else if (method == 4) { //Remove pairing
            byte[] username = d.getBytes(MessageType.USERNAME);
            authInfo.removeUser(authInfo.getMac()+new String(username, StandardCharsets.UTF_8));
            if (!authInfo.hasUser()) {
                System.out.println ("user removed: Set discoverable true");
                //advertiser.setDiscoverable(true);
            }
        } else {
            throw new RuntimeException("Unrecognized method: "+method);
        }
        return new PairingResponse(new byte[] {0x06, 0x01, 0x02});
    }

}
