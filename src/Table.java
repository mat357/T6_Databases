import java.util.*;
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
}
