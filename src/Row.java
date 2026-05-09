import java.util.List;

class Row {
    private List<String> values;

    public Row(List<String> values) {
        this.values = values;
    }

    public List<String> getValues() {
        return values;
    }

    public String getValue(int index) {
        return values.get(index);
    }

    public void setValue(int index, String value) {
        values.set(index, value);
    }

    public void addValue(String value) {
        values.add(value);
    }
}