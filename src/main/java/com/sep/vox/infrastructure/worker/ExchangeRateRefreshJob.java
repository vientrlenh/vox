package com.sep.vox.infrastructure.worker;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.infrastructure.service.ExchangeRateRefreshService;

// The USD->VND exchange rate used to be a static constant that had to be edited by hand in .env (see QuotaSellingPriceProperties)
// -- this job periodically fetches the real rate automatically, so the quota money on orders and the fxRateUsed of each
// usage do not drift far from the market. Runs daily (not weekly like
// QuotaPricingCalibrationJob) because the exchange rate changes much faster than the AI cost being calibrated.
@Component
@RequiredArgsConstructor
public class ExchangeRateRefreshJob {

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        run();
    }

    private final ExchangeRateRefreshService exchangeRateRefreshService;

    @Scheduled(cron = "0 0 4 * * *")
    public void run() {
        exchangeRateRefreshService.refresh();
    }
}
