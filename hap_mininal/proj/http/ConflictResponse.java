package http;


public class ConflictResponse implements HttpResponse {

    @Override
    public int getStatusCode() {
        return 409;
    }

}
