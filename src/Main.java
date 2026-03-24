import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Databases Started. Type 'help' for commands.");

        boolean isRunning = true;
        boolean isFileOpened = false;

        while (isRunning) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            String[] words = input.split(" ");
            String command = words[0].toLowerCase();

            switch (command) {
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
}