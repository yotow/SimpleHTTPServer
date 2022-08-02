import com.sun.jdi.request.StepRequest;

public class Request {
    private final String method;
    private final String path;
    private final String[] partsOfRequest;
    private final String requestLine;

    private  String requestBody;

    public Request(String requestLine) {
        this.requestLine = requestLine;
        this.partsOfRequest = requestLine.split(" ");
        this.path = partsOfRequest[1];
        this.method = partsOfRequest[0];
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public boolean isCorrect() {
        return this.partsOfRequest.length == 3;
    }

    public String getRequestLine() {
        return this.requestLine;
    }
}
