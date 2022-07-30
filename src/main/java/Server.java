import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final String PATH_FOLDER = "public";
    private static final String SPECIAL_PAGE_1 = "/classic.html";
    private static final int DEFAULT_THREAD_COUNT = 64;
    private final List<String> validPaths;
    private boolean stateOff = true;
    private final int port;
    ILog logger = new ConsoleLog();
    private final int threadCount;
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    public Server(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths;
        this.threadCount = DEFAULT_THREAD_COUNT;
    }

    public Server(int port, List<String> validPaths, int threadCount) {
        this.validPaths = validPaths;
        this.port = port;
        this.threadCount = threadCount;
    }

    public void start() {
        executorService = Executors.newFixedThreadPool(threadCount);
        stateOff = false;
        try {
            serverSocket = new ServerSocket(port);
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(() -> run(serverSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            stateOff = true;
            serverSocket.close();
            executorService.shutdown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void run(ServerSocket serverSocket) {
        logger.log("Server started in thread " + Thread.currentThread().getName());
        while (!stateOff) {
            logger.log(Thread.currentThread().getName() + " waiting for connection ");
            try (final var socket = serverSocket.accept();
                 final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 final var out = new BufferedOutputStream(socket.getOutputStream())) {
                // read only request line for simplicity
                // must be in form GET /path HTTP/1.1
                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");

                logger.log("{\"Thread\":\"" + Thread.currentThread().getName() + "\", \"Request line\":\"" + requestLine + "\"}");

                if (parts.length == 3) {
                    final var path = parts[1];


                    if (!validPaths.contains(path)) {
                        sendResponse404(out);
                        continue;
                    }

                    // special case for classic
                    if (path.equals(SPECIAL_PAGE_1)) {
                        sendSpecialResponse1(out);
                        continue;
                    }
                    sendResponse(path, out);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void sendResponse404(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void sendSpecialResponse1(BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", PATH_FOLDER, SPECIAL_PAGE_1);
        final var mimeType = Files.probeContentType(filePath);
        final var template = Files.readString(filePath);

        final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();

        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }

    private void sendResponse(String path, BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", PATH_FOLDER, path);
        final var mimeType = Files.probeContentType(filePath);

        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }
}
