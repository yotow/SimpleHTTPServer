import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    final String sender;
    final String message;
    final String date;

    public Message(String sender, String message) {
        this.message = message;
        this.sender = sender;
        LocalDateTime now = LocalDateTime.now();
        // format datetime to string
        this.date = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy hh:mm a"));

    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }
}
