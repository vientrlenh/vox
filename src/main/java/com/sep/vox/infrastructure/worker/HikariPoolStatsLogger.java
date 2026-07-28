package com.sep.vox.infrastructure.worker;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

/**
 * In số liệu pool connection mỗi 5s để khoanh vùng nghi vấn "quá tải request đồng thời"
 * (nhiều request cùng chờ connection rồi timeout) — không phải do 1 query cụ thể chậm.
 * Xóa/tắt sau khi đã tìm ra nguyên nhân, đây chỉ là công cụ chẩn đoán tạm thời.
 */
@Component
public class HikariPoolStatsLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(HikariPoolStatsLogger.class);

    private final DataSource dataSource;

    public HikariPoolStatsLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Scheduled(fixedDelay = 5000)
    public void logPoolStats() {
        if (!(dataSource instanceof HikariDataSource hikariDataSource)) {
            return;
        }
        var pool = hikariDataSource.getHikariPoolMXBean();
        if (pool == null) {
            return;
        }
        LOGGER.info("[hikari-pool] active={} idle={} awaitingConnection={} total={} max={}",
            pool.getActiveConnections(),
            pool.getIdleConnections(),
            pool.getThreadsAwaitingConnection(),
            pool.getTotalConnections(),
            hikariDataSource.getMaximumPoolSize());
    }
}
