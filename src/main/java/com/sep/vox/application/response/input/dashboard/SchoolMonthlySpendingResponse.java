package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;

public record SchoolMonthlySpendingResponse(
    String month,
    BigDecimal amount,
    BigDecimal subscriptionAmount,
    BigDecimal tokenTopUpAmount
) {
    
}
