package com.sep.vox.infrastructure.worker;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sep.vox.infrastructure.service.ExchangeRateRefreshService;

// Tỷ giá USD->VND trước đây là hằng số tĩnh phải tự tay sửa .env (xem QuotaSellingPriceProperties)
// -- job này tự động lấy tỷ giá thật định kỳ, để tokenUnitPrice mà CreatePlanUseCase/UpdatePlanUseCase
// tự tính không bị lệch xa thị trường. Chạy hằng ngày (không phải hằng tuần như
// QuotaPricingCalibrationJob) vì tỷ giá đổi nhanh hơn nhiều so với chi phí AI calibrate.
@Component
public class ExchangeRateRefreshJob {

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        run();
    }

    private final ExchangeRateRefreshService exchangeRateRefreshService;

    public ExchangeRateRefreshJob(ExchangeRateRefreshService exchangeRateRefreshService) {
        this.exchangeRateRefreshService = exchangeRateRefreshService;
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void run() {
        exchangeRateRefreshService.refresh();
    }
}
