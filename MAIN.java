import java.util.ArrayList;
import java.io.Serializable;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ================= TRANSACTION CLASS =================
class Transaction implements Serializable {
    private LocalDateTime dateTime;
    private String type;
    private double amount;
    private double balanceAfter;

    public Transaction(String type, double amount, double balanceAfter) {
        this.dateTime = LocalDateTime.now();
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public void display() {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

        System.out.printf("%-18s %-15s %-12.2f %-12.2f%n",
                dateTime.format(formatter),
                type,
                amount,
                balanceAfter);
    }
}


// ================= ABSTRACT ACCOUNT CLASS =================
abstract class Account implements Serializable {

    private String accountNumber;
    private String name;
    private String mobile;
    protected double balance;

    private ArrayList<Transaction> transactions;

    // Daily withdrawal details
    private double dailyWithdrawn;
    private LocalDate withdrawalDate;

    public Account(String accountNumber, String name,
                   String mobile, double initialBalance) {

        this.accountNumber = accountNumber;
        this.name = name;
        this.mobile = mobile;
        this.balance = initialBalance;

        transactions = new ArrayList<>();

        dailyWithdrawn = 0;
        withdrawalDate = LocalDate.now();

        if (initialBalance > 0) {
            transactions.add(
                    new Transaction(
                            "Initial Deposit",
                            initialBalance,
                            balance
                    )
            );
        }
    }

    // Encapsulation - Getters

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getName() {
        return name;
    }

    public String getMobile() {
        return mobile;
    }

    public double getBalance() {
        return balance;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    // ================= DEPOSIT =================

    public boolean deposit(double amount) {

        if (amount <= 0) {
            return false;
        }

        balance = balance + amount;

        transactions.add(
                new Transaction(
                        "Deposit",
                        amount,
                        balance
                )
        );

        return true;
    }

    // ================= WITHDRAW =================

    public boolean withdraw(double amount) {

        if (amount <= 0) {
            return false;
        }

        // Reset daily withdrawal if new day
        if (!LocalDate.now().equals(withdrawalDate)) {
            dailyWithdrawn = 0;
            withdrawalDate = LocalDate.now();
        }

        // Check balance
        if (amount > balance) {
            System.out.println("\nInsufficient balance!");
            return false;
        }

        // Check daily limit
        if (dailyWithdrawn + amount > getDailyWithdrawalLimit()) {
            System.out.println("\nDaily withdrawal limit exceeded!");
            System.out.println("Your daily limit is: ₹"
                    + getDailyWithdrawalLimit());
            System.out.println("Already withdrawn today: ₹"
                    + dailyWithdrawn);
            return false;
        }

        balance = balance - amount;
        dailyWithdrawn = dailyWithdrawn + amount;

        transactions.add(
                new Transaction(
                        "Withdrawal",
                        amount,
                        balance
                )
        );

        return true;
    }

    // ================= ABSTRACT METHOD =================

    public abstract double getDailyWithdrawalLimit();

    public abstract String getAccountType();

    // ================= ACCOUNT INFORMATION =================

    public void displayAccountInformation() {

        System.out.println("\n==========================================");
        System.out.println("          ACCOUNT INFORMATION");
        System.out.println("==========================================");

        System.out.println("Account Number : " + accountNumber);
        System.out.println("Account Holder : " + name);
        System.out.println("Mobile Number  : " + mobile);
        System.out.println("Account Type   : " + getAccountType());
        System.out.printf("Balance        : ₹%.2f%n", balance);
        System.out.printf("Daily Limit    : ₹%.2f%n",
                getDailyWithdrawalLimit());

        System.out.println("==========================================");
    }

    // ================= PASSBOOK =================

    public void printPassbook(LocalDate fromDate,
                              LocalDate toDate) {
            final DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        

        System.out.println("\n==============================================================");
        System.out.println("                       PASSBOOK");
        System.out.println("==============================================================");

        System.out.println("Account Number : " + accountNumber);
        System.out.println("Account Holder : " + name);
        System.out.println("From Date      : " + fromDate);
        System.out.println("To Date        : " + toDate);

        System.out.println("--------------------------------------------------------------");

        System.out.printf("%-18s %-15s %-12s %-12s%n",
                "Date & Time",
                "Transaction",
                "Amount",
                "Balance");

        System.out.println("--------------------------------------------------------------");

        boolean found = false;

        for (Transaction transaction : transactions) {

            LocalDate transactionDate =
                    transaction.getDateTime().toLocalDate();

            if (!transactionDate.isBefore(fromDate)
                    && !transactionDate.isAfter(toDate)) {

                transaction.display();
                found = true;
            }
        }

        if (!found) {
            System.out.println("No transactions found for this period.");
        }

        System.out.println("==============================================================");
    }
}


// ================= SAVINGS ACCOUNT =================

class SavingsAccount extends Account {

    private static final double DAILY_LIMIT = 25000;


    public SavingsAccount(String accountNumber,
                           String name,
                           String mobile,
                           double initialBalance) {

        super(accountNumber, name, mobile, initialBalance);
    }

    @Override
    public double getDailyWithdrawalLimit() {
        return DAILY_LIMIT;
    }

    @Override
    public String getAccountType() {
        return "Savings Account";
    }
}


// ================= CURRENT ACCOUNT =================

class CurrentAccount extends Account {

    private static final double DAILY_LIMIT = 50000;

    public CurrentAccount(String accountNumber,
                          String name,
                          String mobile,
                          double initialBalance) {

        super(accountNumber, name, mobile, initialBalance);
    }

    @Override
    public double getDailyWithdrawalLimit() {
        return DAILY_LIMIT;
    }

    @Override
    public String getAccountType() {
        return "Current Account";
    }
}


// ================= BANK CLASS =================

class Bank implements Serializable {

    private ArrayList<Account> accounts;

    private int nextAccountNumber = 1001;

    public Bank() {
        accounts = new ArrayList<>();
    }

    // Save all accounts, balances, transactions and account-number counter.
    public void saveToFile() {
        try (java.io.ObjectOutputStream out =
                new java.io.ObjectOutputStream(
                        new java.io.FileOutputStream("smartbank.dat"))) {
            out.writeObject(this);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Could not save bank data", e);
        }
    }

    // Load previously saved data; create a new bank if no file exists yet.
    public static Bank loadFromFile() {
        java.io.File file = new java.io.File("smartbank.dat");
        if (!file.exists()) {
            return new Bank();
        }

        try (java.io.ObjectInputStream in =
                new java.io.ObjectInputStream(
                        new java.io.FileInputStream(file))) {
            return (Bank) in.readObject();
        } catch (java.io.IOException | ClassNotFoundException e) {
            throw new RuntimeException("Could not load bank data", e);
        }
    }
        public Account createAccountForApi(
        String name,
        String mobile,
        String type,
        double initialBalance) {

    if (name == null || name.trim().isEmpty()
            || mobile == null || !mobile.matches("[0-9]{10}")
            || !Double.isFinite(initialBalance)
            || initialBalance < 0) {
        throw new IllegalArgumentException("Invalid account details");
    }

    String accountNumber = "SB" + nextAccountNumber++;

    Account account;

    if ("SAVINGS".equalsIgnoreCase(type)) {
        account = new SavingsAccount(
                accountNumber, name.trim(), mobile, initialBalance);
    } else if ("CURRENT".equalsIgnoreCase(type)) {
        account = new CurrentAccount(
                accountNumber, name.trim(), mobile, initialBalance);
    } else {
        throw new IllegalArgumentException("Invalid account type");
    }

    accounts.add(account);
    return account;
}

public ArrayList<Account> getAccountsForApi() {
    return new ArrayList<>(accounts);

    }

    // ================= CREATE ACCOUNT =================

    public void createAccount(Scanner sc) {

        System.out.println("\n==========================================");
        System.out.println("             CREATE ACCOUNT");
        System.out.println("==========================================");

        sc.nextLine();

        System.out.print("Enter account holder name: ");
        String name = sc.nextLine();

        System.out.print("Enter mobile number: ");
        String mobile = sc.nextLine();

        System.out.println("\nSelect Account Type:");
        System.out.println("1. Savings Account");
        System.out.println("2. Current Account");

        System.out.print("Enter choice: ");
        int choice = sc.nextInt();

        System.out.print("Enter initial deposit: ₹");
        double initialDeposit = sc.nextDouble();

        if (initialDeposit < 0) {
            System.out.println("Invalid initial deposit!");
            return;
        }

        String accountNumber =
                "SB" + nextAccountNumber;

        nextAccountNumber++;

        Account account;

        if (choice == 1) {

            account = new SavingsAccount(
                    accountNumber,
                    name,
                    mobile,
                    initialDeposit
            );

        } else if (choice == 2) {

            account = new CurrentAccount(
                    accountNumber,
                    name,
                    mobile,
                    initialDeposit
            );

        } else {

            System.out.println("Invalid account type!");
            return;
        }

        accounts.add(account);

        System.out.println("\n==========================================");
        System.out.println("       ACCOUNT CREATED SUCCESSFULLY");
        System.out.println("==========================================");

        System.out.println("Account Number : "
                + account.getAccountNumber());

        System.out.println("Account Holder : "
                + account.getName());

        System.out.println("Account Type   : "
                + account.getAccountType());

        System.out.printf("Initial Balance: ₹%.2f%n",
                account.getBalance());

        System.out.println("==========================================");
    }

    // ================= FIND ACCOUNT =================

    public Account findAccount(String accountNumber) {

        for (Account account : accounts) {

            if (account.getAccountNumber()
                    .equalsIgnoreCase(accountNumber)) {

                return account;
            }
        }

        return null;
    }

    // ================= GET ACCOUNT =================

    public Account getAccount(Scanner sc) {

        sc.nextLine();

        System.out.print("Enter account number: ");
        String accountNumber = sc.nextLine();

        Account account = findAccount(accountNumber);

        if (account == null) {
            System.out.println("\nAccount not found!");
        }

        return account;
    }

    // ================= DEPOSIT =================

    public void depositMoney(Scanner sc) {

        Account account = getAccount(sc);

        if (account == null) {
            return;
        }

        System.out.print("Enter amount to deposit: ₹");
        double amount = sc.nextDouble();

        if (account.deposit(amount)) {

            System.out.println("\nDeposit successful!");
            System.out.printf("Deposited Amount: ₹%.2f%n", amount);
            System.out.printf("New Balance: ₹%.2f%n",
                    account.getBalance());

        } else {

            System.out.println("\nInvalid deposit amount!");
        }
    }

    // ================= WITHDRAW =================

    public void withdrawMoney(Scanner sc) {

        Account account = getAccount(sc);

        if (account == null) {
            return;
        }

        System.out.print("Enter amount to withdraw: ₹");
        double amount = sc.nextDouble();

        if (account.withdraw(amount)) {

            System.out.println("\nWithdrawal successful!");
            System.out.printf("Withdrawn Amount: ₹%.2f%n",
                    amount);

            System.out.printf("Remaining Balance: ₹%.2f%n",
                    account.getBalance());

        }
    }

    // ================= CHECK BALANCE =================

    public void checkBalance(Scanner sc) {

        Account account = getAccount(sc);

        if (account == null) {
            return;
        }

        System.out.println("\n==========================================");
        System.out.println("              BALANCE");
        System.out.println("==========================================");

        System.out.println("Account Number: "
                + account.getAccountNumber());

        System.out.printf("Available Balance: ₹%.2f%n",
                account.getBalance());

        System.out.println("==========================================");
    }

    // ================= DAILY LIMIT =================

    public void checkDailyLimit(Scanner sc) {

        Account account = getAccount(sc);

        if (account == null) {
            return;
        }

        System.out.println("\n==========================================");
        System.out.println("        DAILY WITHDRAWAL LIMIT");
        System.out.println("==========================================");

        System.out.println("Account Type : "
                + account.getAccountType());

        System.out.printf("Daily Limit  : ₹%.2f%n",
                account.getDailyWithdrawalLimit());

        System.out.println("==========================================");
    }

    // ================= ACCOUNT INFORMATION =================

    public void displayAccountInformation(Scanner sc) {

        Account account = getAccount(sc);

        if (account == null) {
            return;
        }

        account.displayAccountInformation();
    }

    // ================= PASSBOOK =================

    public void passbook(Scanner sc) {

        Account account = getAccount(sc);

        if (account == null) {
            return;
        }

        sc.nextLine();

        try {

            System.out.print("Enter from date (dd-MM-yyyy): ");
            String from = sc.nextLine();

            System.out.print("Enter to date (dd-MM-yyyy): ");
            String to = sc.nextLine();

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("dd-MM-yyyy");

            LocalDate fromDate =
                    LocalDate.parse(from, formatter);

            LocalDate toDate =
                    LocalDate.parse(to, formatter);

            if (fromDate.isAfter(toDate)) {

                System.out.println(
                        "From date cannot be after To date."
                );

                return;
            }

            account.printPassbook(fromDate, toDate);

        } catch (Exception e) {

            System.out.println(
                    "Invalid date format!"
            );

            System.out.println(
                    "Please use dd-MM-yyyy."
            );
        }
    }
}


// ================= MAIN CLASS =================

public class MAIN {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        Bank bank = new Bank();

        int choice;

        System.out.println("\n==============================================");
        System.out.println("          WELCOME TO SMARTBANK");
        System.out.println("       BANKING MANAGEMENT SYSTEM");
        System.out.println("==============================================");

        do {

            System.out.println("\n");
            System.out.println("============== MAIN MENU ==============");
            System.out.println("1. Create an Account");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. Check Daily Withdrawal Limit");
            System.out.println("5. Check Balance");
            System.out.println("6. Display Account Information");
            System.out.println("7. Passbook Print");
            System.out.println("8. Exit");
            System.out.println("========================================");

            System.out.print("Enter your choice: ");

            while (!sc.hasNextInt()) {

                System.out.println("Please enter a valid number.");
                sc.next();

                System.out.print("Enter your choice: ");
            }

            choice = sc.nextInt();

            switch (choice) {

                case 1:
                    bank.createAccount(sc);
                    break;

                case 2:
                    bank.depositMoney(sc);
                    break;

                case 3:
                    bank.withdrawMoney(sc);
                    break;

                case 4:
                    bank.checkDailyLimit(sc);
                    break;

                case 5:
                    bank.checkBalance(sc);
                    break;

                case 6:
                    bank.displayAccountInformation(sc);
                    break;

                case 7:
                    bank.passbook(sc);
                    break;

                case 8:
                    System.out.println(
                            "\nThank you for using SmartBank!"
                    );
                    break;

                default:
                    System.out.println(
                            "\nInvalid choice! Please try again."
                    );
            }

        } while (choice != 8);

        sc.close();
    }
}