package pairing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import http.*;

class PairingResponse extends OkResponse {
    
    private static final Map<String, String> headers = Collections.unmodifiableMap(
            new HashMap<String, String>() {
                private static final long serialVersionUID = 1L;
                {
                    put("Content-type", "application/pairing+tlv8");
                }
            });
            

    public PairingResponse(byte[] body) {
        super(body);
    }
    
    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

}
