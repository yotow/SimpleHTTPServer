public class ConsoleLog implements ILog {

    @Override
    public void log(String msg) {
        System.out.println(msg);
    }
}
