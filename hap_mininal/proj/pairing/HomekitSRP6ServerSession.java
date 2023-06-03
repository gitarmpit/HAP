package pairing;


import java.math.BigInteger;

import srp6.*;

public class HomekitSRP6ServerSession extends SRP6Session {
    
    public static enum State {
    
        INIT,
        STEP_1,
        STEP_2
    }
    
    private boolean noSuchUserIdentity = false;
    private BigInteger v = null;
    private BigInteger b = null;
    private State state;
    
    protected ClientEvidenceRoutineImpl _clientEvidenceRoutine = null;
    protected ServerEvidenceRoutineImpl _serverEvidenceRoutine = null;

    private final byte _b[] = 
    { 0, -58, 29, -97, 52, -96, 57, -25, -99, 53, -105, -21, 28, -75, -67, 5, 75, 
        -121, -117, 122, -56, -64, -124, 127, -15, 19, -4, 20, -91, -97, 72, 37, 84, 70, 
        79, -21, 2, 42, -59, -12, 18, 30, 65, -68, 91, -25, -69, 125, -24, -88, 61, -118, 
        -110, 110, -95, 63, -17, 26, 49, -84, -17, 17, 103, -84, -40, -9, -107, -117, 20, 
        -104, 27, 50, -66, 39, 113, -64, 72, -20, -63, -43, -93, -67, 58, 40, -74, 69, 115, 
        -2, -76, 50, 31, -59, -67, -100, 32, -65, -111, -120, 5, 56, 84, -32, -56, -28, 14, 
        -125, 95, 0, -108, 11, -41, -37, 27, -20, 109, 26, -120, 46, -66, -108, 57, -3, -122, 
        -11, -111, -67, -49, 114, 24, -55, -114, 66, 9, 118, 66, 4, 38, 86, -21, -82, 46, 25, 
        -92, -10, 125, 105, -51, 11, -67, 106, 125, -11, 66, 102, 12, 110, 97, -79, 120, -86, 
        -6, -9, 19, -84, 42, -37, -33, 92, -96, 102, 34, 111, 67, -97, 15, 72, -27, -6, -92, 
        0, 60, 99, 3, 66, -40, -52, -31, 51, 13, 66, 53, 33, -117, -10, 12, -70, 90, 28, 30, 
        -71, 20, 122, 75, 85, 24, -76, -14, 89, -43, 45, 75, 66, 46, -25, -95, -26, -63, 95, 
        -103, -74, 10, 3, -21, 53, 66, -71, 65, -81, 68, 48, -51, -45, -7, -26, -52, 97, -52, 
        18, 40, -65, 90, 125, -26, 67, 109, 43, -86, 98, 54, -60, 95, 14, -89, 16, 21, -77, 
        -35, -28, 0, -1, -85, -89, -64, -69, -6, -104, 119, -59, 41, 95, 67, -83, -98, -70, 104,
        -107, -97, 125, 75, 52, 72, -15, -1, 103, -110, 77, -52, -128, -48, 58, 25, -88, 45, -11, 
        -4, 79, -43, -62, -51, -36, -10, -111, 77, -28, 99, 3, 18, -58, -71, 114, 46, -16, 21, -79, 
        -54, 82, 120, 107, 57, 89, 103, -88, 68, 0, 6, 8, 9, -2, -68, -87, -65, -126, -73, -3, -63, 
        -79, -46, 127, -97, -52, -43, -2, -100, 126, -101, -5, 68, 103, -25, -29, 6, -62, 80, -48, 
        74, -26, 17, 9, 51, 7, -66, -10, -5, 88, 47, 108, -99, 31, 4, 83, 34, -38, -64, 88, -62, 
        99, 49, -86, -60, -60, 0, -116, 51, -102, -71 
    };


    public HomekitSRP6ServerSession(final SRP6CryptoParams config, final int timeout) {
    
        super(timeout);
        
        if (config == null)
            throw new IllegalArgumentException("The SRP-6a crypto parameters must not be null");

        this.config = config;
        
        digest = config.getMessageDigestInstance();
        
        if (digest == null)
            throw new IllegalArgumentException("Unsupported hash algorithm 'H': " + config.H);

        state = State.INIT;
        
        updateLastActivityTime();
    }
    
    
    public HomekitSRP6ServerSession(final SRP6CryptoParams config) {
    
        this(config, 0);
            //setClientEvidenceRoutine(new ClientEvidenceRoutineImpl());
            //setServerEvidenceRoutine(new ServerEvidenceRoutineImpl());
        _clientEvidenceRoutine = new ClientEvidenceRoutineImpl();
        _serverEvidenceRoutine = new ServerEvidenceRoutineImpl();
    }
    
    public BigInteger step1(final String userID, final BigInteger s, final BigInteger v) {
    
        // Check arguments
        
        if (userID == null || userID.trim().isEmpty())
            throw new IllegalArgumentException("The user identity 'I' must not be null or empty");
            
        this.userID = userID;
        
        
        if (s == null)
            throw new IllegalArgumentException("The salt 's' must not be null");
            
        this.s = s;
        
        
        if (v == null)
            throw new IllegalArgumentException("The verifier 'v' must not be null");
            
        this.v = v;
        
        
        // Check current state
        if (state != State.INIT)
            throw new IllegalStateException("State violation: Session must be in INIT state");
        
        // Generate server private and public values
        k = SRP6Routines.computeK(digest, config.N, config.g);
        digest.reset();
        
        //b = HomekitSRP6Routines.generatePrivateValue(config.N, random);
        b = new BigInteger(_b);
        digest.reset();
        
        B = SRP6Routines.computePublicServerValue(config.N, config.g, k, v, b);

        state = State.STEP_1;
        
        updateLastActivityTime();
        
        return B;
    }
    
    
//  public BigInteger mockStep1(final String userID, final BigInteger s, final BigInteger v) {
//  
//      noSuchUserIdentity = true;
//      
//      return step1(userID, s, v);
//  }
    
    public BigInteger step2(final BigInteger A, final BigInteger M1)
        throws SRP6Exception {
    
        // Check arguments
        
        if (A == null)
            throw new IllegalArgumentException("The client public value 'A' must not be null");
            
        this.A = A;
        
        if (M1 == null)
            throw new IllegalArgumentException("The client evidence message 'M1' must not be null");
        
        this.M1 = M1;
    
        // Check current state
        if (state != State.STEP_1)
            throw new IllegalStateException("State violation: Session must be in STEP_1 state");
        
        // Check timeout
        if (hasTimedOut())
            throw new SRP6Exception("Session timeout", SRP6Exception.CauseType.TIMEOUT);
    
        // Check A validity
        if (! SRP6Routines.isValidPublicValue(config.N, A))
            throw new SRP6Exception("Bad client public value 'A'", SRP6Exception.CauseType.BAD_PUBLIC_VALUE);
        
        // Check for previous mock step 1
        if (noSuchUserIdentity)
            throw new SRP6Exception("Bad client credentials", SRP6Exception.CauseType.BAD_CREDENTIALS);
        
        if (hashedKeysRoutine != null) {
            URoutineContext hashedKeysContext = new URoutineContext(A, B);
            u = hashedKeysRoutine.computeU(config, hashedKeysContext);
        } else {
            u = SRP6Routines.computeU(digest, config.N, A, B);
            digest.reset();
        }
        
        S = SRP6Routines.computeSessionKey(config.N, v, u, A, b);
        
        // Compute the own client evidence message 'M1'
        BigInteger computedM1;
        
        if (_clientEvidenceRoutine != null) {
        
            // With custom routine
            SRP6ClientEvidenceContext ctx = new SRP6ClientEvidenceContext(userID, s, A, B, S);
            computedM1 = _clientEvidenceRoutine.computeClientEvidence(config, ctx);
        }
        else {
            // With default routine
            computedM1 = SRP6Routines.computeClientEvidence(digest, A, B, S);
            digest.reset();
        }
        
        if (! computedM1.equals(M1))
            throw new SRP6Exception("Bad client credentials", SRP6Exception.CauseType.BAD_CREDENTIALS);
    
        state = State.STEP_2;
        
        
        if (_serverEvidenceRoutine != null) {
        
            // With custom routine
            SRP6ServerEvidenceContext ctx = new SRP6ServerEvidenceContext(A, M1, S);
            
            M2 = _serverEvidenceRoutine.computeServerEvidence(config, ctx);
        }
        else 
        {
            M2 = SRP6Routines.computeServerEvidence(digest, A, M1, computedM1);
        }
        
        updateLastActivityTime();
        
        return M2;
    }
    
    
    public State getState() {
    
        return state;
    }
}
