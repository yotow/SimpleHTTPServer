import java.util.concurrent.ThreadFactory;

public class ServerThreadFactory implements ThreadFactory {
    private int counter = 0;
    private String prefix = "";

    public ServerThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    public Thread newThread(Runnable r) {
        return new Thread(r, prefix + "-" + counter++);
    }
}

