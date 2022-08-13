import org.apache.http.NameValuePair;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class HandlerManager {
    private final Server server;
    private static final String PATH_FOLDER = "public";


    public HandlerManager(Server server) {
        this.server = server;
    }

    public void addHandlers() {
        server.addHandler("GET", Server.DEFAULT_HANDLER_KEY, (request, responseStream) -> {
            final var filePath = Path.of(".", PATH_FOLDER, request.getURI());
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
        });

        server.addHandler("GET", "/classic.html", (request, responseStream) -> sendResponse(request.getURI(), "{time}", LocalDateTime.now().toString(), responseStream));

        server.addHandler("GET", "/message.html", (request, responseStream) -> {
            if (request.getRequestLineParams().isPresent()) {
                int count = 0;
                for (NameValuePair nm : request.getRequestLineParams().get()) {
                    if (nm.getName().equalsIgnoreCase("count")) {
                        count = Integer.parseInt(nm.getValue());
                    }
                }
                String mes = "<br>" + MessageRepository.getMessages(count);
                mes = mes.replaceAll("\n", "<br>");

                sendResponse(request.getURI(), "{messages}", mes, responseStream);
            } else {
                sendResponse(request.getURI(), null, null, responseStream);
            }
        });

        server.addHandler("POST", "/auth.html", (request, responseStream) -> {
            String login = null;
            String password = null;
            if (request.getBodyParams().isPresent()) {
                for (NameValuePair nm : request.getBodyParams().get()) {
                    switch (nm.getName()) {
                        case "login":
                            login = nm.getValue();
                            break;
                        case "password":
                            password = nm.getValue();
                    }
                }

                if (AccountManager.getAccount(login).isPresent() && AccountManager.getAccount(login).get().getPass().equals(password)) {
                    sendResponse("/links.html", null, null, responseStream);
                }
                sendResponse("/auth.html", "систему", "систему. <p style=\"color:red;\">Неправильный логин или пароль</p>", responseStream);
            }

        });

        server.addHandler("GET", "/", ((request, responseStream) -> sendResponse("/auth.html", null, null, responseStream)));
    }

    private synchronized static void sendResponse(String path, String forReplace, String replacement, BufferedOutputStream responseStream) {
        final var filePath = Path.of(".", PATH_FOLDER, path);
        final String mimeType;
        final String template;
        try {
            mimeType = Files.probeContentType(filePath);
            template = Files.readString(filePath);
            final byte[] content;
            if (forReplace != null && replacement != null && !forReplace.isEmpty()) {
                content = template.replace(forReplace, replacement).getBytes();
            } else {
                content = template.getBytes();
            }
            responseStream.write(("HTTP/1.1 200 OK\r\n" + "Content-Type: " + mimeType + "\r\n" + "Content-Length: " + content.length + "\r\n" + "Connection: close\r\n" + "\r\n").getBytes());
            responseStream.write(content);
            responseStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
