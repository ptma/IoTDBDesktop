package org.apache.iotdb.tool.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;

import java.io.IOException;
import java.io.PrintWriter;

public class CSVPrinterWrapper {

    private final String filePath;
    private final CSVFormat csvFormat;
    private final TextPrinter textPrinter;
    private CSVPrinter csvPrinter;

    public CSVPrinterWrapper(String filePath, TextPrinter textPrinter) {
        this.filePath = filePath;
        this.textPrinter = textPrinter;
        this.csvFormat =
            CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setEscape('\\')
                .setQuoteMode(QuoteMode.NONE)
                .build();
    }

    public void printRecord(final Iterable<?> values) throws IOException {
        if (csvPrinter == null) {
            csvPrinter = csvFormat.print(new PrintWriter(filePath));
        }
        csvPrinter.printRecord(values);
    }

    public void print(Object value) {
        if (csvPrinter == null) {
            try {
                csvPrinter = csvFormat.print(new PrintWriter(filePath));
            } catch (IOException e) {
                textPrinter.printException(e);
                return;
            }
        }
        try {
            csvPrinter.print(value);
        } catch (IOException e) {
            textPrinter.printException(e);
        }
    }

    public void println() throws IOException {
        csvPrinter.println();
    }

    public void close() throws IOException {
        if (csvPrinter != null) {
            csvPrinter.close();
        }
    }

    public void flush() throws IOException {
        if (csvPrinter != null) {
            csvPrinter.flush();
        }
    }

}
