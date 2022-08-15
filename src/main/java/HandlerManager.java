import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.http.NameValuePair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

public class HandlerManager {
    private final Server server;
    private static final String PATH_FOLDER = "public";
    private static final String FILES_FOLDER = "C:/files/";


    public HandlerManager(Server server) {
        this.server = server;
    }

    public void addHandlers() {
        server.addHandler("GET", Server.DEFAULT_HANDLER_KEY, (request, responseStream) -> {
            final var filePath = Path.of(".", PATH_FOLDER, request.getPath());
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

        server.addHandler("GET", "/classic.html", (request, responseStream) -> sendResponse(request.getPath(), "{time}", LocalDateTime.now().toString(), responseStream));

        server.addHandler("GET", "/message.html", (request, responseStream) -> {
            if (request.getRequestLineParams().isPresent()) {
                int count = 0;
                for (NameValuePair nm : request.getRequestLineParams().get()) {
                    if (nm.getName().equalsIgnoreCase("count")) {
                        try {
                            count = Integer.parseInt(nm.getValue().trim());
                        } catch (NumberFormatException n) {
                            //ignore
                        }
                    }
                }
                String mes = message2HTML(count);

                sendResponse(request.getPath(), "{messages}", mes, responseStream);
            } else {
                sendResponse(request.getPath(), null, null, responseStream);
            }
        });

        server.addHandler("POST", "/message.html", (request, responseStream) -> {
            if (request.getBodyParams().isPresent()) {
                String name = null;
                String text = null;
                for (NameValuePair nm : request.getBodyParams().get()) {
                    if (nm.getName().equalsIgnoreCase("name")) {
                        name = nm.getValue();
                    } else if (nm.getName().equalsIgnoreCase("text")) {
                        text = nm.getValue();
                    }
                }
                if (name == null | text == null) {
                    sendResponse(request.getPath(), null, null, responseStream);
                }

                MessageRepository.setMessage(name, text);

                String mes = message2HTML(10);

                sendResponse(request.getPath(), "{messages}", mes, responseStream);
            } else {
                sendResponse(request.getPath(), null, null, responseStream);
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

        server.addHandler("GET", "/files.html", ((request, responseStream) -> sendResponse("/files.html", null, null, responseStream)));

        server.addHandler("POST", "/files.html", (request, responseStream) -> {

            try {
                // Create a new file upload handler
                ServletFileUpload upload = new ServletFileUpload();

                // Parse the request
                FileItemIterator iter = upload.getItemIterator(request);
                while (iter.hasNext()) {
                    FileItemStream item = iter.next();
                    String name = item.getFieldName();

                    try (BufferedInputStream stream = new BufferedInputStream(item.openStream())) {
                        if (item.isFormField()) {
                            System.out.println("Form field " + name + " with value " + Streams.asString(stream) + " detected.");
                        } else {
                            Path path = Path.of(FILES_FOLDER + item.getName());
                            ConsoleLog.getLogger().log("File field " + name + " with file name " + item.getName() + " upload.");
                            // Process the input stream
                            java.nio.file.Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            } catch (IOException | FileUploadException e) {
                throw new RuntimeException(e);
            }
            sendResponse("/files.html", null, null, responseStream);
        });
    }

    private static String message2HTML(int count) {
        List<Message> messages = MessageRepository.getMessages(count);

        StringBuilder stringBuilder = new StringBuilder();
        for (Message message : messages) {
            stringBuilder.append(message.getSender()).append(": ").append(message.getMessage()).append(". <pre>    дата: ").append(message.getDate()).append("</pre>\n");
        }

        String mes = "<br>" + stringBuilder;
        mes = mes.replaceAll("\n", "<br>");
        return mes;
    }

    private synchronized static void sendResponse(final String path, final String forReplace, final String replacement, final BufferedOutputStream responseStream) {
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
