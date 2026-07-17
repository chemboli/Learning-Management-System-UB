package Final.year.project.SmartLearning.shared;

import java.util.List;

/**
 * Minimal CSV writer — no external dependency needed for the simple flat
 * tables this app exports (scores, user lists). Handles the RFC 4180 basics:
 * quote any field containing a comma, quote, or newline, and double up
 * internal quotes.
 */
public final class CsvWriter {

    private CsvWriter() {}

    public static String write(List<String> header, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();

        // A UTF-8 BOM helps Excel detect encoding correctly for non-ASCII names.
        sb.append('\uFEFF');

        sb.append(toLine(header));

        for (List<String> row : rows) {
            sb.append(toLine(row));
        }

        return sb.toString();
    }

    private static String toLine(List<String> fields) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) line.append(',');
            line.append(escape(fields.get(i)));
        }
        line.append("\r\n");
        return line.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";

        boolean needsQuoting = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");

        if (!needsQuoting) return value;

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
