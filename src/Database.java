import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Database {
    private String fileName;
    private Map<String, Table> tables;

    public Database() {
        tables = new LinkedHashMap<>();
    }

    public void open(String fileName) throws IOException {
        this.fileName = fileName;
        tables.clear();

        Path path = Path.of(fileName);

        if (!Files.exists(path)) {
            Files.createFile(path);
            return;
        }

        List<String> lines = Files.readAllLines(path);

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty() || line.equals("T6_DATABASE")) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length >= 2) {
                Table table = Table.loadFromFile(parts[1]);
                tables.put(table.getName(), table);
            }
        }
    }

    public void save() throws IOException {
        if (fileName == null) {
            throw new IllegalStateException("No database is open.");
        }

        Path databasePath = Path.of(fileName);
        Path parent = databasePath.getParent();

        if (parent == null) {
            parent = Path.of(".");
        }

        List<String> lines = new java.util.ArrayList<>();
        lines.add("T6_DATABASE");

        for (Table table : tables.values()) {
            String tableFile = table.getFileName();

            if (tableFile == null) {
                tableFile = parent.resolve(table.getName() + ".tbl").toString();
            }

            table.saveToFile(tableFile);
            lines.add(table.getName() + "|" + tableFile);
        }

        Files.write(databasePath, lines);
    }

    public void saveAs(String newFileName) throws IOException {
        fileName = newFileName;
        save();
    }

    public void close() {
        fileName = null;
        tables.clear();
    }

    public void importTable(String fileName) throws IOException {
        Table table = Table.loadFromFile(fileName);

        if (tables.containsKey(table.getName())) {
            throw new IllegalArgumentException("Table already exists.");
        }

        tables.put(table.getName(), table);
    }

    public void rename(String oldName, String newName) {
        if (!tables.containsKey(oldName)) {
            throw new IllegalArgumentException("Table not found.");
        }

        if (tables.containsKey(newName)) {
            throw new IllegalArgumentException("Table with new name already exists.");
        }

        Table table = tables.remove(oldName);
        table.setName(newName);
        tables.put(newName, table);
    }

    public Table getTable(String name) {
        Table table = tables.get(name);

        if (table == null) {
            throw new IllegalArgumentException("Table not found.");
        }

        return table;
    }

    public void showTables() {
        if (tables.isEmpty()) {
            System.out.println("No tables.");
            return;
        }

        for (String tableName : tables.keySet()) {
            System.out.println(tableName);
        }
    }
}
