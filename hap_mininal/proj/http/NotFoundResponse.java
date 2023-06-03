package http;



public class NotFoundResponse implements HttpResponse {

    @Override
    public int getStatusCode() {
        return 404;
    }

}
