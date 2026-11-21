package com.sep.vox.application.response.output;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One cell of a {@link SpreadsheetDocument}. The cell type decides how the spreadsheet program sorts and filters it. */
public sealed interface SpreadsheetCell {

    /** Text cell. When the document neutralizes formulas, text starting with a formula trigger gets a leading quote. */
    record Text(String value) implements SpreadsheetCell {
    }

    /** Numeric cell, so the spreadsheet program can sort and average it. */
    record Number(BigDecimal value) implements SpreadsheetCell {
    }

    /** Date cell: the wall-clock value is written as is, so the caller converts the time zone first. */
    record DateTime(LocalDateTime value, String displayPattern) implements SpreadsheetCell {
    }

    /** Empty cell. Used instead of 0 or an empty string when a value is missing. */
    record Blank() implements SpreadsheetCell {
    }

    static SpreadsheetCell text(String value) {
        return new Text(value);
    }

    static SpreadsheetCell number(BigDecimal value) {
        return value == null ? blank() : new Number(value);
    }

    static SpreadsheetCell dateTime(LocalDateTime value, String displayPattern) {
        return value == null ? blank() : new DateTime(value, displayPattern);
    }

    static SpreadsheetCell blank() {
        return new Blank();
    }
}
