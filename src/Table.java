import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

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

    public void saveToFile(String fileName) throws IOException {
        this.fileName = fileName;
        List<String> lines = new ArrayList<>();

        lines.add(name);

        List<String> columnLines = new ArrayList<>();
        for (Column column : columns) {
            columnLines.add(column.getName() + ":" + column.getType());
        }
        lines.add(String.join(",", columnLines));

        for (Row row : rows) {
            List<String> escaped = new ArrayList<>();
            for (String value : row.getValues()) {
                escaped.add(escape(value));
            }
            lines.add(String.join(",", escaped));
        }

        Files.write(Path.of(fileName), lines);
    }

    public void addColumn(Column column) {
        columns.add(column);
        for (Row row : rows) {
            row.addValue("NULL");
        }
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

    public List<Row> select(int columnIndex, String value) {
        checkColumnIndex(columnIndex);
        List<Row> result = new ArrayList<>();
        value = removeQuotes(value);

        for (Row row : rows) {
            if (equalsValue(row.getValue(columnIndex), value)) {
                result.add(row);
            }
        }

        return result;
    }

    public int update(int searchColumn, String searchValue, int targetColumn, String targetValue) {
        checkColumnIndex(searchColumn);
        checkColumnIndex(targetColumn);

        searchValue = removeQuotes(searchValue);
        targetValue = removeQuotes(targetValue);

        if (!columns.get(targetColumn).getType().isValid(targetValue)) {
            throw new IllegalArgumentException("Invalid target value type.");
        }

        int changed = 0;

        for (Row row : rows) {
            if (equalsValue(row.getValue(searchColumn), searchValue)) {
                row.setValue(targetColumn, targetValue);
                changed++;
            }
        }

        return changed;
    }

    public int delete(int searchColumn, String searchValue) {
        checkColumnIndex(searchColumn);
        searchValue = removeQuotes(searchValue);

        int deleted = 0;
        Iterator<Row> iterator = rows.iterator();

        while (iterator.hasNext()) {
            Row row = iterator.next();
            if (equalsValue(row.getValue(searchColumn), searchValue)) {
                iterator.remove();
                deleted++;
            }
        }

        return deleted;
    }

    public int count(int searchColumn, String searchValue) {
        return select(searchColumn, searchValue).size();
    }

    public String aggregate(int searchColumn, String searchValue, int targetColumn, String operation) {
        checkColumnIndex(searchColumn);
        checkColumnIndex(targetColumn);

        if (!columns.get(targetColumn).getType().isNumeric()) {
            throw new IllegalArgumentException("Target column must be numeric.");
        }

        List<Row> selected = select(searchColumn, searchValue);
        if (selected.isEmpty()) {
            return "NULL";
        }

        double result = 0;
        boolean hasValue = false;

        switch (operation.toLowerCase()) {
            case "sum":
                result = 0;
                break;
            case "product":
                result = 1;
                break;
            case "maximum":
            case "max":
                result = Double.NEGATIVE_INFINITY;
                break;
            case "minimum":
            case "min":
                result = Double.POSITIVE_INFINITY;
                break;
            default:
                throw new IllegalArgumentException("Unknown aggregate operation.");
        }

        for (Row row : selected) {
            String value = row.getValue(targetColumn);
            if (value.equalsIgnoreCase("NULL")) {
                continue;
            }

            double number = Double.parseDouble(value);
            hasValue = true;

            switch (operation.toLowerCase()) {
                case "sum":
                    result += number;
                    break;
                case "product":
                    result *= number;
                    break;
                case "maximum":
                case "max":
                    result = Math.max(result, number);
                    break;
                case "minimum":
                case "min":
                    result = Math.min(result, number);
                    break;
            }
        }

        if (!hasValue) {
            return "NULL";
        }

        if (result == (int) result) {
            return String.valueOf((int) result);
        }

        return String.valueOf(result);
    }

    public Table innerJoin(Table other, int thisColumn, int otherColumn) {
        checkColumnIndex(thisColumn);
        other.checkColumnIndex(otherColumn);

        Table result = new Table(name + "_" + other.name + "_join", null);

        for (Column column : columns) {
            result.columns.add(new Column(name + "." + column.getName(), column.getType()));
        }

        for (int i = 0; i < other.columns.size(); i++) {
            if (i != otherColumn) {
                Column column = other.columns.get(i);
                result.columns.add(new Column(other.name + "." + column.getName(), column.getType()));
            }
        }

        for (Row left : rows) {
            String leftValue = left.getValue(thisColumn);
            if (leftValue.equalsIgnoreCase("NULL")) {
                continue;
            }

            for (Row right : other.rows) {
                String rightValue = right.getValue(otherColumn);
                if (!rightValue.equalsIgnoreCase("NULL") && equalsValue(leftValue, rightValue)) {
                    List<String> joined = new ArrayList<>(left.getValues());
                    for (int i = 0; i < right.getValues().size(); i++) {
                        if (i != otherColumn) {
                            joined.add(right.getValue(i));
                        }
                    }
                    result.rows.add(new Row(joined));
                }
            }
        }

        return result;
    }

    public void describe() {
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            System.out.println((i + 1) + ". " + column.getName() + " - " + column.getType());
        }
    }

    public void print() {
        printRows(rows);
    }

    public void printPaged(Scanner scanner) {
        int pageSize = 10;
        int page = 0;

        while (true) {
            int from = page * pageSize;
            int to = Math.min(from + pageSize, rows.size());

            if (from >= rows.size()) {
                System.out.println("No more rows.");
                return;
            }

            printRows(rows.subList(from, to));

            if (rows.size() <= pageSize) {
                return;
            }

            System.out.print("next/prev/exit> ");
            String command = scanner.nextLine().trim().toLowerCase();

            if (command.equals("next") && to < rows.size()) {
                page++;
            } else if (command.equals("prev") && page > 0) {
                page--;
            } else if (command.equals("exit")) {
                return;
            }
        }
    }

    public void printRows(List<Row> printedRows) {
        if (columns.isEmpty()) {
            System.out.println("(empty table)");
            return;
        }

        int[] widths = new int[columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).getName().length();
        }

        for (Row row : printedRows) {
            for (int i = 0; i < row.getValues().size(); i++) {
                widths[i] = Math.max(widths[i], row.getValue(i).length());
            }
        }

        for (int i = 0; i < columns.size(); i++) {
            System.out.print(pad(columns.get(i).getName(), widths[i]) + " ");
        }
        System.out.println();

        for (int width : widths) {
            System.out.print("-".repeat(width) + " ");
        }
        System.out.println();

        for (Row row : printedRows) {
            for (int i = 0; i < row.getValues().size(); i++) {
                System.out.print(pad(row.getValue(i), widths[i]) + " ");
            }
            System.out.println();
        }
    }

    public String getName() {
        return name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setName(String name) {
        this.name = name;
    }

    private void checkColumnIndex(int index) {
        if (index < 0 || index >= columns.size()) {
            throw new IllegalArgumentException("Invalid column number.");
        }
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
}
