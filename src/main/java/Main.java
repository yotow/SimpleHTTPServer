import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<String> paths = new ArrayList<>(List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/events.html", "/events.js"));
        Server server = new Server(9999, paths, 1);
        AccountManager.init();
        HandlerManager handlerManager = new HandlerManager(server);
        handlerManager.addHandlers();
        server.start();

        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        server.stop();
    }
}
