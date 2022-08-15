import java.io.*;
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
    ILog logger = ConsoleLog.getLogger();
    private final int threadCount;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private final ConcurrentMap<String, Handler> getHandlers;
    private final ConcurrentMap<String, Handler> postHandlers;

    public Server(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths;
        this.threadCount = DEFAULT_THREAD_COUNT;

        getHandlers = new ConcurrentHashMap<>();
        postHandlers = new ConcurrentHashMap<>();
    }

    public Server(int port, List<String> validPaths, int threadCount) {
        this.validPaths = validPaths;
        this.port = port;
        this.threadCount = threadCount;

        getHandlers = new ConcurrentHashMap<>();
        postHandlers = new ConcurrentHashMap<>();

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
                 final var out = new BufferedOutputStream(socket.getOutputStream());
                 final var in = new BufferedInputStream(socket.getInputStream())) {

                final var request = new Request(in);
                if (request.isValid() && validPaths.contains(request.getPath())) {
                    sendResponse(out, request);
                } else {
                    sendResponse404(out);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendResponse(BufferedOutputStream out, Request request) {
        var handlers = postHandlers;
        if (request.getMethod().equals("GET")) {
            handlers = getHandlers;
        }

        Handler handler;
        if (handlers.containsKey(request.getPath())) {
            handler = handlers.get(request.getPath());
        } else {
            handler = handlers.get(DEFAULT_HANDLER_KEY);
        }
        handler.handle(request, out);
    }

    private void sendResponse404(BufferedOutputStream out) throws IOException {
        out.write(("HTTP/1.1 404 Not Found\r\n" + "Content-Length: 0\r\n" + "Connection: close\r\n" + "\r\n").getBytes());
        out.flush();
    }

    public void addHandler(String method, String path, Handler handler) {
        var handlers = postHandlers;
        if (method.equals("GET")) {
            handlers = getHandlers;
        }
        validPaths.add(path);
        handlers.put(path, handler);
    }
}
