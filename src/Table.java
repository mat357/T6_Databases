import java.util.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
public class Table {
    private String name;
    private String fileName;
    private List<Column> columns;
    private List<Row> rows;

    public Table(String name, String fileName) {
        this.name = name;
        this.fileName = fileName;
        this.columns = new ArrayList<>();
        this.rows = new ArrayList<>();
    }

    public List<Row> getRows() {
        return rows;
    }

    private static String removeQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String escape(String value) {
        if (value.contains(" ") || value.contains(",") || value.contains(";") || value.contains("|")) {
            return "\"" + value.replace("\"", "") + "\"";
        }
        return value;
    }

    private boolean equalsValue(String first, String second) {
        return removeQuotes(first).equals(removeQuotes(second));
    }

    private static String pad(String text, int width) {
        return text + " ".repeat(width - text.length());
    }

    private static boolean isInteger(String text) {
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static List<String> splitValues(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (!inQuotes && (c == ',' || c == ';' || c == '|')) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        result.add(current.toString().trim());

        if (result.size() == 1 && !line.contains("\"")) {
            result.clear();
            for (String part : line.trim().split("\\s+")) {
                result.add(part);
            }
        }

        return result;
    }

    public void insert(List<String> values) {
        if (values.size() != columns.size()) {
            throw new IllegalArgumentException("Invalid number of values.");
        }

        List<String> fixedValues = new ArrayList<>();

        for (int i = 0; i < values.size(); i++) {
            String value = removeQuotes(values.get(i).trim());
            if (!columns.get(i).getType().isValid(value)) {
                throw new IllegalArgumentException("Invalid value for column " + columns.get(i).getName());
            }
            fixedValues.add(value);
        }

        rows.add(new Row(fixedValues));
    }
    public static Table loadFromFile(String fileName) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(fileName));
        int index = 0;

        while (index < lines.size() && lines.get(index).trim().isEmpty()) {
            index++;
        }

        if (index >= lines.size()) {
            throw new IllegalArgumentException("Empty table file.");
        }

        String tableName = removeQuotes(lines.get(index++).trim());
        Table table = new Table(tableName, fileName);

        while (index < lines.size() && lines.get(index).trim().isEmpty()) {
            index++;
        }

        if (index >= lines.size()) {
            throw new IllegalArgumentException("Missing columns.");
        }

        String columnLine = lines.get(index++).trim();

        if (isInteger(columnLine)) {
            int count = Integer.parseInt(columnLine);
            for (int i = 0; i < count; i++) {
                String[] parts = lines.get(index++).trim().split("\\s+|:");
                table.columns.add(new Column(parts[0], DataType.fromString(parts[1])));
            }
        } else {
            List<String> columnParts = splitValues(columnLine);
            for (String part : columnParts) {
                String[] pieces = part.trim().split("\\s+|:");
                if (pieces.length >= 2) {
                    table.columns.add(new Column(pieces[0], DataType.fromString(pieces[1])));
                }
            }
        }

        if (index < lines.size() && isInteger(lines.get(index).trim())) {
            index++;
        }

        while (index < lines.size()) {
            String line = lines.get(index++).trim();
            if (!line.isEmpty()) {
                table.insert(splitValues(line));
            }
        }

        return table;
    }
}
