package com.sep.vox.application.response.output;

/**
 * Presentation switches of a {@link SpreadsheetDocument}.
 *
 * @param emphasizedHeader         bold header text on a grey background
 * @param frozenHeader             the header row stays visible when scrolling
 * @param autoFilter               every column gets a filter arrow over the header and the data rows
 * @param maxColumnWidthCharacters upper bound of the auto-sized column width, or {@code null} for no bound
 * @param neutralizeFormulas       text starting with a formula trigger ({@code = + - @}, tab, carriage return) is
 *                                 prefixed with a single quote
 */
public record SpreadsheetOptions(
    boolean emphasizedHeader,
    boolean frozenHeader,
    boolean autoFilter,
    Integer maxColumnWidthCharacters,
    boolean neutralizeFormulas
) {

    public static SpreadsheetOptions plain() {
        return new SpreadsheetOptions(false, false, false, null, false);
    }
}
