
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiServer {

    private static final Bank bank = Bank.loadFromFile();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress(
    Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"))
)

        server.createContext("/api/", exchange -> {
            addCors(exchange);

            try {
                if ("OPTIONS".equalsIgnoreCase(
                        exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod();

                if (path.equals("/api/create-account")
                        && method.equalsIgnoreCase("POST")) {
                    createAccount(exchange);
                } else if (path.equals("/api/accounts")
                        && method.equalsIgnoreCase("GET")) {
                    listAccounts(exchange);
                } else if (path.equals("/api/deposit")
                        && method.equalsIgnoreCase("POST")) {
                    moneyAction(exchange, true);
                } else if (path.equals("/api/withdraw")
                        && method.equalsIgnoreCase("POST")) {
                    moneyAction(exchange, false);
                } else if (path.equals("/api/balance")
                        && method.equalsIgnoreCase("GET")) {
                    accountDetails(exchange, false);
                } else if (path.equals("/api/account-info")
                        && method.equalsIgnoreCase("GET")) {
                    accountDetails(exchange, true);
                } else if (path.equals("/api/passbook")
                        && method.equalsIgnoreCase("GET")) {
                    passbook(exchange);
                } else {
                    send(exchange, 404,
                            "{\"error\":\"API endpoint not found\"}");
                }
            } catch (Exception e) {
                send(exchange, 400, jsonError(
                        e.getMessage() == null
                                ? "Request failed" : e.getMessage()));
            }
        });

        server.start();
        System.out.println(
                "SmartBank API started at http://localhost:8080");
        System.out.println(
                "Keep this terminal open while using the website.");
    }

    private static void createAccount(HttpExchange ex)
            throws Exception {
        String body = readBody(ex);

        String name = getJson(body, "name");
        String mobile = getJson(body, "mobile");
        String type = getJson(body, "type");
        double deposit = Double.parseDouble(
                getJson(body, "initialDeposit"));

        Account account = bank.createAccountForApi(
                name, mobile, type, deposit);
                bank.saveToFile();

        send(ex, 200, "{"
                + "\"success\":true,"
                + "\"accountNumber\":\""
                + escape(account.getAccountNumber()) + "\","
                + "\"name\":\"" + escape(account.getName()) + "\","
                + "\"balance\":" + account.getBalance()
                + "}");
    }

    private static void listAccounts(HttpExchange ex)
            throws IOException {
        ArrayList<Account> accounts = bank.getAccountsForApi();
        StringBuilder out = new StringBuilder("[");

        for (int i = 0; i < accounts.size(); i++) {
            if (i > 0) out.append(",");
            out.append(accountJson(accounts.get(i)));
        }

        out.append("]");
        send(ex, 200, out.toString());
    }

    private static void moneyAction(
            HttpExchange ex, boolean isDeposit) throws Exception {
        String body = readBody(ex);
        String number = getJson(body, "accountNumber");
        double amount = Double.parseDouble(getJson(body, "amount"));

        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }

        Account account = bank.findAccount(number);

        if (account == null) {
            throw new IllegalArgumentException(
                    "Account not found");
        }

        boolean success = isDeposit
                ? account.deposit(amount)
                : account.withdraw(amount);

        if (!success) {
            throw new IllegalArgumentException(
                    isDeposit ? "Invalid deposit amount"
                            : "Withdrawal failed. Check balance and daily limit");
        }
        bank.saveToFile();

        send(ex, 200, "{"
                + "\"success\":true,"
                + "\"message\":\""
                + (isDeposit ? "Deposit successful"
                             : "Withdrawal successful") + "\","
                + "\"accountNumber\":\""
                + escape(account.getAccountNumber()) + "\","
                + "\"balance\":" + account.getBalance()
                + "}");
    }

    private static void accountDetails(
            HttpExchange ex, boolean includeInfo) throws Exception {
        String number = queryValue(ex, "accountNumber");

        Account account = bank.findAccount(number);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        String json = "{"
                + "\"accountNumber\":\""
                + escape(account.getAccountNumber()) + "\","
                + "\"name\":\"" + escape(account.getName()) + "\","
                + "\"mobile\":\"" + escape(account.getMobile()) + "\","
                + "\"accountType\":\""
                + escape(account.getAccountType()) + "\","
                + "\"balance\":" + account.getBalance() + ","
                + "\"dailyWithdrawalLimit\":"
                + account.getDailyWithdrawalLimit()
                + "}";

        send(ex, 200, json);
    }

    private static void passbook(HttpExchange ex) throws Exception {
        String number = queryValue(ex, "accountNumber");
        String from = queryValue(ex, "fromDate");
        String to = queryValue(ex, "toDate");

        Account account = bank.findAccount(number);
        if (account == null) {
            throw new IllegalArgumentException("Account not found");
        }

        LocalDate fromDate = LocalDate.parse(
                from, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate toDate = LocalDate.parse(
                to, DateTimeFormatter.ISO_LOCAL_DATE);

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException(
                    "From date cannot be after To date");
        }

        StringBuilder out = new StringBuilder();
        out.append("{\"account\":")
           .append(accountJson(account))
           .append(",\"transactions\":[");

        boolean first = true;

        for (Transaction t : account.getTransactions()) {
            LocalDate date = t.getDateTime().toLocalDate();

            if (!date.isBefore(fromDate) && !date.isAfter(toDate)) {
                if (!first) out.append(",");
                first = false;

                out.append("{")
                   .append("\"dateTime\":\"")
                   .append(t.getDateTime().toString()).append("\",")
                   .append("\"type\":\"").append(escape(t.getType()))
                   .append("\",")
                   .append("\"amount\":").append(t.getAmount()).append(",")
                   .append("\"balanceAfter\":")
                   .append(t.getBalanceAfter())
                   .append("}");
            }
        }

        out.append("]}");
        send(ex, 200, out.toString());
    }

    private static String accountJson(Account a) {
        return "{"
                + "\"accountNumber\":\""
                + escape(a.getAccountNumber()) + "\","
                + "\"name\":\"" + escape(a.getName()) + "\","
                + "\"mobile\":\"" + escape(a.getMobile()) + "\","
                + "\"accountType\":\""
                + escape(a.getAccountType()) + "\","
                + "\"balance\":" + a.getBalance()
                + "}";
    }

    private static String readBody(HttpExchange ex)
            throws IOException {
        return new String(
                ex.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private static String queryValue(
            HttpExchange ex, String key) {
        String query = ex.getRequestURI().getRawQuery();

        if (query != null) {
            for (String part : query.split("&")) {
                String[] pair = part.split("=", 2);

                if (pair.length == 2
                        && pair[0].equals(key)) {
                    return URLDecoder.decode(
                            pair[1], StandardCharsets.UTF_8);
                }
            }
        }

        throw new IllegalArgumentException(
                "Missing parameter: " + key);
    }

    private static String getJson(String json, String key) {
        Pattern pattern = Pattern.compile(
                "\"" + Pattern.quote(key)
                + "\"\\s*:\\s*(?:\"((?:\\\\.|[^\"\\\\])*)\"|([^,}\\s]+))");

        Matcher matcher = pattern.matcher(json);

        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Missing field: " + key);
        }

        String value = matcher.group(1) != null
                ? matcher.group(1) : matcher.group(2);

        return value.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n");
    }

    private static String escape(String value) {
        if (value == null) return "";

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String jsonError(String message) {
        return "{\"error\":\"" + escape(message) + "\"}";
    }

    private static void addCors(HttpExchange ex) {
        ex.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set(
                "Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set(
                "Access-Control-Allow-Headers", "Content-Type");
    }

    private static void send(
            HttpExchange ex, int status, String response)
            throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
