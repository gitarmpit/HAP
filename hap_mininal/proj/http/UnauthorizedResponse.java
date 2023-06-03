package http;


public class UnauthorizedResponse implements HttpResponse {

    @Override
    public int getStatusCode() {
        return 401;
    }

}
