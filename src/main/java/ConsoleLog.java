public class ConsoleLog implements ILog {

    private static volatile ConsoleLog entity;
    private ConsoleLog(){
    }

    public static ConsoleLog getLogger(){
        if(entity == null){
            entity = new ConsoleLog();
        }
        return entity;
    }
    @Override
    public void log(String msg) {
        System.out.println(msg);
    }
}
