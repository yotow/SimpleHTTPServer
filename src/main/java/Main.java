import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String pathFolder = "public";
        List<String> paths = new ArrayList<>(List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js"));
        Server server = new Server(9999, paths, 2);

        // добавление handler' ов (обработчиков)
        server.addHandler("GET", "/classic.html", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                final var filePath = Path.of(".", pathFolder, request.getPath());
                final String mimeType;
                final String template;
                try {
                    mimeType = Files.probeContentType(filePath);
                    template = Files.readString(filePath);
                    final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();

                    responseStream.write(("HTTP/1.1 200 OK\r\n" + "Content-Type: " + mimeType + "\r\n" + "Content-Length: " + content.length + "\r\n" + "Connection: close\r\n" + "\r\n").getBytes());
                    responseStream.write(content);
                    responseStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        server.addHandler("GET", Server.DEFAULT_HANDLER_KEY, new Handler() {
            @Override
            public void handle(Request request, BufferedOutputStream responseStream) {
                final var filePath = Path.of(".", pathFolder, request.getPath());
                final String mimeType;
                try {
                    mimeType = Files.probeContentType(filePath);
                    final var length = Files.size(filePath);
                    responseStream.write(("HTTP/1.1 200 OK\r\n" + "Content-Type: " + mimeType + "\r\n" + "Content-Length: " + length + "\r\n" + "Connection: close\r\n" + "\r\n").getBytes());
                    Files.copy(filePath, responseStream);
                    responseStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

/*
        server.addHandler("POST", "/messages", new Handler() {
            public void handle(Request r, BufferedOutputStream responseStream) {

            }
        });
*/

        server.start();

        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        server.stop();
    }
}
