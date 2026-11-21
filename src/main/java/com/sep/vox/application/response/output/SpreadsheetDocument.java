package com.sep.vox.application.response.output;

import java.util.List;

/** A one-sheet workbook described without any spreadsheet library type. Row 0 is the header. */
public record SpreadsheetDocument(
    String sheetName,
    List<String> headers,
    List<List<SpreadsheetCell>> rows,
    List<ColumnDropdown> dropdowns,
    SpreadsheetOptions options
) {
}
