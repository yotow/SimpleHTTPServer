import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AccountManager {

    private static final int COUNT_SYMBOLS_IN_DEFAULT_PASS = 10;
    private static final ConcurrentMap<String, Account> accountMap = new ConcurrentHashMap<>();

    public static void init() {
        String generatedString = getRandomStr(COUNT_SYMBOLS_IN_DEFAULT_PASS);
        System.out.println("login: " + "admin, " + "password: " + generatedString);
        accountMap.put("admin", new Account("admin", generatedString));
    }

    private AccountManager() {

    }

    public static Optional<Account> getAccount(String nickName) {
        if (accountMap.containsKey(nickName)) {
            return Optional.of(accountMap.get(nickName));
        }
        return Optional.empty();
    }

    private static String getRandomStr(int count) {
        String uuid = UUID.randomUUID().toString();
        return uuid.replace("-", "").substring(0, count);
    }
}
