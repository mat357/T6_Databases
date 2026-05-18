import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Databases Started. Type 'help' for commands.");

        Database database = new Database();
        boolean isRunning = true;
        boolean isFileOpened = false;

        while (isRunning) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] words = input.split(" ");
            String command = words[0].toLowerCase();

            switch (command) {
                case "help":
                    System.out.println("The following commands are supported:\n" +
                            "open <file>    opens <file>\n" +
                            "close          closes currently opened file\n" +
                            "save           saves the currently open file\n" +
                            "save as <file> saves the currently open file in <file>\n" +
                            "import <file>  imports a table\n" +
                            "showtables     shows all tables\n" +
                            "describe <table> describes table structure\n" +
                            "print <table>  prints table contents\n" +
                            "export <table> <file> exports table to file\n" +
                            "select <column-n> <value> <table>\n" +
                            "addcolumn <table> <column> <type>\n" +
                            "update <table> <search column-n> <search value> <target column-n> <target value>\n" +
                            "delete <table> <search column-n> <search value>\n" +
                            "insert <table> <value1> <value2> ...\n" +
                            "innerjoin <table1> <column-n1> <table2> <column-n2>\n" +
                            "rename <old table> <new table>\n" +
                            "count <table> <search column-n> <search value>\n" +
                            "aggregate <table> <search column-n> <search value> <target column-n> <operation>\n" +
                            "help           prints this information\n" +
                            "exit           exits the program");
                    break;
                case "open":
                    if (words.length < 2) {
                        System.out.println("Error: No file specified.");
                        break;
                    }
                    String fileName = words[1];
                    File file = new File(fileName);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    System.out.println("Successfully opened " + fileName);
                    isFileOpened = true;
                    break;
                case "close":
                    System.out.println("Successfully closed file.");
                    isFileOpened = false;
                    break;
                case "exit":
                    System.out.println("Exiting the program...");
                    isRunning = false;
                    break;
                case "save":
                    if (words.length >= 3 && words[1].equalsIgnoreCase("as")) {
                        requireOpen(isFileOpened);
                        database.saveAs(words[2]);
                        System.out.println("Successfully saved as " + words[2]);
                    } else {
                        requireOpen(isFileOpened);
                        database.save();
                        System.out.println("Successfully saved.");
                    }
                    break;

                case "import":
                    requireOpen(isFileOpened);
                    database.importTable(words[1]);
                    System.out.println("Table imported.");
                    break;

                case "showtables":
                    requireOpen(isFileOpened);
                    database.showTables();
                    break;

                case "describe":
                    requireOpen(isFileOpened);
                    database.getTable(words[1]).describe();
                    break;

                case "print":
                    requireOpen(isFileOpened);
                    database.getTable(words[1]).printPaged(scanner);
                    break;

                case "export":
                    requireOpen(isFileOpened);
                    database.getTable(words[1]).saveToFile(words[2]);
                    System.out.println("Table exported.");
                    break;

                case "select":
                    requireOpen(isFileOpened);
                    Table selectTable = database.getTable(words[3]);
                    selectTable.printRows(selectTable.select(toIndex(words[1]), words[2]));
                    break;

                case "addcolumn":
                    requireOpen(isFileOpened);
                    database.getTable(words[1]).addColumn(
                            new Column(words[2], DataType.fromString(words[3]))
                    );
                    System.out.println("Column added.");
                    break;

                case "update":
                    requireOpen(isFileOpened);
                    int updated = database.getTable(words[1]).update(
                            toIndex(words[2]),
                            words[3],
                            toIndex(words[4]),
                            words[5]
                    );
                    System.out.println("Updated rows: " + updated);
                    break;

                case "delete":
                    requireOpen(isFileOpened);
                    int deleted = database.getTable(words[1]).delete(
                            toIndex(words[2]),
                            words[3]
                    );
                    System.out.println("Deleted rows: " + deleted);
                    break;

                case "insert":
                    requireOpen(isFileOpened);
                    Table insertTable = database.getTable(words[1]);
                    List<String> insertArgs = new ArrayList<>();
                    for (int i = 2; i < words.length; i++) {
                        insertArgs.add(words[i]);
                    }
                    insertTable.insert(insertArgs);
                    System.out.println("Row inserted.");
                    break;

                case "innerjoin":
                    requireOpen(isFileOpened);
                    Table first = database.getTable(words[1]);
                    Table second = database.getTable(words[3]);
                    Table joined = first.innerJoin(second, toIndex(words[2]), toIndex(words[4]));
                    joined.print();
                    break;

                case "rename":
                    requireOpen(isFileOpened);
                    database.rename(words[1], words[2]);
                    System.out.println("Table renamed.");
                    break;

                case "count":
                    requireOpen(isFileOpened);
                    int count = database.getTable(words[1]).count(
                            toIndex(words[2]),
                            words[3]
                    );
                    System.out.println(count);
                    break;

                case "aggregate":
                    requireOpen(isFileOpened);
                    String result = database.getTable(words[1]).aggregate(
                            toIndex(words[2]),
                            words[3],
                            toIndex(words[4]),
                            words[5]
                    );
                    System.out.println(result);
                    break;

                default:
                    if (!isFileOpened) {
                        System.out.println("Error: No file is currently open. Use 'open <file>'.");
                    } else {
                        System.out.println("Unknown command.");
                    }
            }
        }
        scanner.close();
    }

    private static void requireOpen(boolean isOpen) {
        if (!isOpen) {
            throw new IllegalStateException("No database is currently open.");
        }
    }

    private static int toIndex(String text) {
        return Integer.parseInt(text) - 1;
    }

    private static List<String> splitCommand(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : input.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

}