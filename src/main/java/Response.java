public class Response {
    private final String statusLine;
    private final String headers;
    private final String body;

    public Response(String statusLine, String headers, String body) {
        this.statusLine = statusLine;
        this.headers = headers;
        this.body = body;
    }

    public String getStatusLine() {
        return statusLine;
    }

    public String getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
