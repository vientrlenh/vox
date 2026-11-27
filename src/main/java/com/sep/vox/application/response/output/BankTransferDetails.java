package com.sep.vox.application.response.output;

import java.math.BigDecimal;

/**
 * The details for the school to transfer money ITSELF when the banking app cannot scan the code.
 *
 * <p>Comes alongside the QR string and does not replace it: a code that cannot be scanned is common (poor camera, old
 * app, a screenshot), and then this set of numbers is the only thing that can save the transaction.
 *
 * <p>{@code transferContent} is the most DANGEROUS field here. It is what the gateway uses to match the money
 * to the order; if the user mistypes it the money still lands in the account but no order receives it, and the system has
 * no refund flow -- it must be reconciled by hand. The UI must highlight it and let people copy it, not make them type it.
 */
public record BankTransferDetails(
    String bankBin,
    String accountNumber,
    String accountName,
    BigDecimal amountVnd,
    String transferContent
) {
}
