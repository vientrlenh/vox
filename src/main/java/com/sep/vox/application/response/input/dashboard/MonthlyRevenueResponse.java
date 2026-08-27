package com.sep.vox.application.response.input.dashboard;

import java.math.BigDecimal;

public record MonthlyRevenueResponse(
    String month,
    BigDecimal amount
) {
    
}
