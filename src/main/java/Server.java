import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int DEFAULT_THREAD_COUNT = 64;
    public static final String DEFAULT_HANDLER_KEY = "default";
    private final List<String> validPaths;
    private boolean stateOff = true;
    private final int port;
    ILog logger = new ConsoleLog();
    private final int threadCount;
    private ServerSocket serverSocket;
    private ExecutorService executorService;

    private final ConcurrentMap<String, ConcurrentMap<String, Handler>> handlers;

    public Server(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths;
        this.threadCount = DEFAULT_THREAD_COUNT;

        handlers = new ConcurrentHashMap<>();

    }

    public Server(int port, List<String> validPaths, int threadCount) {
        this.validPaths = validPaths;
        this.port = port;
        this.threadCount = threadCount;

        handlers = new ConcurrentHashMap<>();
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
            try (final var socket = serverSocket.accept(); final var in = new BufferedReader(new InputStreamReader(socket.getInputStream())); final var out = new BufferedOutputStream(socket.getOutputStream())) {
                // read only request line for simplicity
                // must be in form GET /path HTTP/1.1
                final var request = new Request(in.readLine());
                logger.log("{\"Thread\":\"" + Thread.currentThread().getName() + "\", \"Request line\":\""
                        + request.getRequestLine() + "\"}");

                if (request.isCorrect() && validPaths.contains(request.getPath())) {
                    if (handlers.containsKey(request.getMethod())) {
                        ConcurrentMap<String, Handler> map = handlers.get(request.getMethod());
                        Handler handler;
                        if (map.containsKey(request.getPath())) {
                            handler = map.get(request.getPath());
                        } else {
                            handler = map.get(DEFAULT_HANDLER_KEY);
                        }
                        handler.handle(request, out);
                    }
                } else {
                    sendResponse404(out);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendResponse404(BufferedOutputStream out) throws IOException {
        out.write(("HTTP/1.1 404 Not Found\r\n" + "Content-Length: 0\r\n" + "Connection: close\r\n" + "\r\n").getBytes());
        out.flush();
    }

    public void addHandler(String method, String path, Handler handler) {
        validPaths.add(path);
        ConcurrentMap<String, Handler> entity = new ConcurrentHashMap<>();
        entity.put(path, handler);
        handlers.put(method, entity);
    }
}
