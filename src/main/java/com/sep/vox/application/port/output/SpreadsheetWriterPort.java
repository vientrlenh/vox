package com.sep.vox.application.port.output;

import com.sep.vox.application.response.output.SpreadsheetDocument;

public interface SpreadsheetWriterPort {

    /** Returns the {@code .xlsx} bytes of the document; throws {@link IllegalStateException} if it cannot be written. */
    byte[] write(SpreadsheetDocument document);
}
