import java.util.LinkedList;
import java.util.List;

public class MessageRepository {
    private static final LinkedList<Message> messages = new LinkedList<>();

    static {
        messages.addFirst(new Message("Света", "Привет"));
        messages.addFirst(new Message("Света", "как дела?"));
        messages.addFirst(new Message("Паша", "Бро го"));
        messages.addFirst(new Message("Паша", "Бро не гони"));
        messages.addFirst(new Message("Маша", "пойдем гулять"));
        messages.addFirst(new Message("Маша", "или в кино"));
        messages.addFirst(new Message("Погода", "в питере время пить"));
    }
    private MessageRepository(){

    }

    public static List<Message> getMessages(int count) {
        LinkedList<Message> res = new LinkedList<>();
        messages.stream().limit(count).forEach(res::addFirst);
        return res;
    }

    public static void setMessage(String name, String text){
        messages.addFirst(new Message(name, text));
    }
}
