import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class Request {

    private String requestLine;
    private String method;
    private String URI;
    private String versionOfProtocol;
    private String headers;
    private String body;

    private List<NameValuePair> requestLineParams;

    private List<NameValuePair> bodyParams;

    private boolean valid;


    public Request(InputStream inputStream) throws IOException {

        try (final var in = new BufferedReader(new InputStreamReader(inputStream))) {
            requestLine = in.readLine();
            if (!requestLine.isEmpty()) {
                var partsOfRequest = requestLine.split(" ");
                if (partsOfRequest.length == 3) {
                    this.method = partsOfRequest[0];

                    if (partsOfRequest[1].contains("?")) {
                        String s = partsOfRequest[1];
                        this.URI = s.substring(0, s.indexOf('?'));
                        requestLineParams = URLEncodedUtils.parse(s.substring(s.indexOf('?') + 1), StandardCharsets.UTF_8);
                    } else this.URI = partsOfRequest[1];

                    this.versionOfProtocol = partsOfRequest[2];
                } else {
                    valid = false;
                }
                this.headers = readLinesFromBytes(in);
                if (in.ready()) {
                    this.body = readLinesFromBytes(in);
                    //TODO else json
                    if (!body.substring(0, 5).contains("{"))
                        bodyParams = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
                }
                valid = true;
            }
        } catch (IOException e) {
            ILog logger = ConsoleLog.getLogger();
            logger.log(e.getMessage());
            valid = false;
        }
    }

    public String getVersionOfProtocol() {
        return versionOfProtocol;
    }

    public String getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public String getMethod() {
        return method;
    }

    public String getURI() {
        return URI;
    }

    public boolean isValid() {
        return this.valid;
    }

    public String getRequestLine() {
        return this.requestLine;
    }

    public Optional<List<NameValuePair>> getRequestLineParams() {
        if (requestLineParams != null) {
            return Optional.of(requestLineParams);
        } else return Optional.empty();
    }

    public Optional<List<NameValuePair>> getBodyParams() {
        if (bodyParams != null) {
            return Optional.of(bodyParams);
        } else return Optional.empty();
    }

    private String readLinesFromBytes(BufferedReader in) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while (!(line = in.readLine()).isEmpty()) {
            stringBuilder.append(line).append("\n");
            if (!in.ready()) {
                break;
            }
        }
        return stringBuilder.toString();
    }

    // from Google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}