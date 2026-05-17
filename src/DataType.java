public enum DataType {
    INTEGER, FLOAT, STRING, NULL;

    public static DataType fromString(String text) {
        String t = text.trim().toUpperCase();

        if (t.equals("INT")) {
            return INTEGER;
        }
        if (t.equals("REAL") || t.equals("DOUBLE")) {
            return FLOAT;
        }
        if (t.equals("TEXT")) {
            return STRING;
        }

        return DataType.valueOf(t);
    }

    public boolean isValid(String value) {
        if (value == null || value.equalsIgnoreCase("NULL")) {
            return true;
        }

        try {
            switch (this) {
                case INTEGER:
                    Integer.parseInt(value);
                    return true;
                case FLOAT:
                    Double.parseDouble(value);
                    return true;
                case STRING:
                    return true;
                case NULL:
                    return value.equalsIgnoreCase("NULL");
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isNumeric() {
        return this == INTEGER || this == FLOAT;
    }
}
