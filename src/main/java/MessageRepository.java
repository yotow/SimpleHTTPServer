import java.util.LinkedList;

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

    public static String getMessages(int count) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Message message : messages) {
            stringBuilder.append(message.getSender()).append(":").append(message.getMessage()).append("\n");
            count--;
            if (count == 0) {
                break;
            }
        }
        return stringBuilder.toString();
    }
}
