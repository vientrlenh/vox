package com.sep.vox.application.response.output;

import java.util.List;

/** A dropdown list that restricts a column to the given values, from {@code firstRow} to {@code lastRow} (0-based). */
public record ColumnDropdown(int columnIndex, int firstRow, int lastRow, List<String> allowedValues) {
}
