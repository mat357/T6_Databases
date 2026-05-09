import java.util.LinkedHashMap;
import java.util.Map;

public class Database {
    private String fileName;
    private Map<String, Table> tables;

    public Database() {
        tables = new LinkedHashMap<>();
    }
}
