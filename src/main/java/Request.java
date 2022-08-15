import org.apache.commons.fileupload.RequestContext;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Request implements RequestContext {
    private final static int LIMIT_REQUEST_LINE_HEADERS = 4096;
    final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
    final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
    private final List<String> allowedMethods = List.of("GET", "POST");
    private final String GET = "GET";
    private String method;
    private String path;
    private String protocol;
    private ByteArrayInputStream rawBody;
    private Map<String, String> headersParams;
    private List<NameValuePair> requestLineParams;
    private List<NameValuePair> bodyParams;
    private boolean valid;
    private int requestLineEnd;
    private int headersEnd;

    @Override
    public String getCharacterEncoding() {
        return StandardCharsets.UTF_8.name();
    }

    @Override
    public String getContentType() {
        Optional<String> s = getHeadersParam("Content-Type");
        return s.orElse("null");
    }

    @Override
    public int getContentLength() {
        Optional<String> s = getHeadersParam("Content-Length");
        int res = 0;
        if (s.isPresent()) {
            try {
                res = Integer.parseInt(s.get());
            } catch (NumberFormatException e) {
                ConsoleLog.getLogger().log(e.getMessage());
                return 0;
            }
        }
        return res;
    }

    @Override
    public InputStream getInputStream() {
        return new BufferedInputStream(rawBody);
    }

    public String getProtocol() {
        return protocol;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public boolean isValid() {
        return this.valid;
    }

    public Map<String, String> getHeaders() {
        return headersParams;
    }

    public Optional<String> getHeadersParam(String headerName) {
        if (this.headersParams.containsKey(headerName)) {
            return Optional.of(headersParams.get(headerName));
        }
        return Optional.empty();
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

    public Request(final InputStream in) throws IOException {
        in.mark(LIMIT_REQUEST_LINE_HEADERS);
        Optional<String> headers;
        var requestLine = readAndParseRequestLine(in);
        in.reset();

        if (requestLine.isPresent() && isRequestLineValid()) {
            ConsoleLog.getLogger().log("{\"Thread\":\"" + Thread.currentThread().getName() + "\", \"Request line\":\"" + requestLine.orElse("null") + "\"}\n\n");

            headers = readHeaders(in);
            headersParams = parseHeaders(headers.orElse("null:null"));

            in.reset();
            in.skip(headersEnd);

            // для GET тела нет
            if (!method.equals(GET)) {

                // вычитываем Content-Length, чтобы прочитать body
                final var contentLength = getHeadersParam("Content-Length");
                if (contentLength.isPresent()) {
                    final var length = Integer.parseInt(contentLength.get());
                    final byte[] bodyBytes;
                    var contentType = getHeadersParam("Content-Type");
                    if (contentType.isPresent() && contentType.get().contains("multipart/form-data")) {
                        bodyBytes = in.readNBytes(length);
                        rawBody = new ByteArrayInputStream(bodyBytes);
                    } else {
                        bodyBytes = in.readNBytes(length);
                        parseBody(new String(bodyBytes));
                    }
                    System.out.println("++++++++body++++++++++++");
                    System.out.println(new String(bodyBytes));
                    System.out.println("++++++++body++++++++++++");
                }
            }
        } else {
            setRequestInvalid();
        }
    }

    private Optional<String> readAndParseRequestLine(InputStream in) throws IOException {
        final var buffer = new byte[LIMIT_REQUEST_LINE_HEADERS];
        final var read = in.read(buffer);

        // ищем request line
        requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            return Optional.empty();
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd));
        final var partsOfRequest = requestLine.split(" ");
        if (partsOfRequest.length != 3) {
            return Optional.empty();
        }

        this.method = partsOfRequest[0];
        if (!allowedMethods.contains(method)) {
            return Optional.empty();
        }

        if (partsOfRequest[1].contains("?")) {
            String s = partsOfRequest[1];
            this.path = s.substring(0, s.indexOf('?'));
            requestLineParams = URLEncodedUtils.parse(s.substring(s.indexOf('?') + 1), StandardCharsets.UTF_8);
        } else this.path = partsOfRequest[1];
        if (!path.startsWith("/")) {
            return Optional.empty();
        }

        this.protocol = partsOfRequest[2];

        requestLineEnd = requestLineEnd + requestLineDelimiter.length;

        return Optional.of(requestLine);
    }

    private Optional<String> readHeaders(InputStream in) throws IOException {
        final var buffer = new byte[LIMIT_REQUEST_LINE_HEADERS];
        final var read = in.read(buffer);
        // ищем заголовки

        final var headersStart = requestLineEnd;
        headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return Optional.empty();
        }

        in.reset();
        in.skip(requestLineEnd);
        final var headersBytes = in.readNBytes(headersEnd - headersStart);

        String headers = new String(headersBytes);
        headersEnd += headersDelimiter.length;
        return Optional.of(headers);
    }

    private void setRequestInvalid() {
        valid = false;
    }

    private boolean isRequestLineValid() {
        this.valid = getMethod() != null && getMethod().length() > 2 && !getMethod().isEmpty()
                && getProtocol() != null && !getProtocol().isEmpty() && getProtocol().contains("HTTP")
                && getPath() != null && !getPath().isEmpty() && getPath().startsWith("/");
        return this.valid;
    }

    private void parseBody(String body) {
        bodyParams = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
    }

    private Map<String, String> parseHeaders(String headers) {
        Map<String, String> res = new HashMap<>();
        String[] strings = headers.split("\n");
        for (String s : strings) {
            int pos = s.indexOf(":");
            if (pos < 0) {
                continue;
            }
            res.put(s.substring(0, pos), s.substring(pos + 1).trim());
        }
        return res;
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
