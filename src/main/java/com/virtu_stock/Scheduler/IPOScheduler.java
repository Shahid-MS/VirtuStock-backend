package com.virtu_stock.Scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.virtu_stock.Service.AsyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class IPOScheduler {

    @Value("${mail.admin}")
    private String adminMail;

    private final AsyncService asyncService;

    // @Scheduled(cron = "0 20 9 * * MON-FRI", zone = "Asia/Kolkata")
    // public void scheduledIPOFetch() {
    // log.info("IPO fetch scheduler triggered");

    // asyncService.fetchIPOInBackground(
    // "open", null, 1, adminMail);
    // }

    @Scheduled(cron = "0 0 8,16,20,23 * * *", zone = "Asia/Kolkata")
    @Scheduled(cron = "0 30 9,13,14,17,18 * * *", zone = "Asia/Kolkata")
    @Scheduled(cron = "0 15 10,12,15 * * *", zone = "Asia/Kolkata")
    public void scheduledIPOUpdate() {
        log.info("IPO update scheduler triggered");

        asyncService.fetchAndUpdateIPOInBackground(
                null, null, 1, null, adminMail);
    }

    @Scheduled(cron = "0 5 8,16,20,23 * * MON-FRI", zone = "Asia/Kolkata")
    @Scheduled(cron = "0 35 9,13,14,17,18 * * MON-FRI", zone = "Asia/Kolkata")
    @Scheduled(cron = "0 20 10,12,15 * * MON-FRI", zone = "Asia/Kolkata")
    public void scheduledSubscriptionUpdate() {
        log.info("Subscription update scheduler triggered");

        asyncService.fetchAndUpdateSubscriptionInBackground(
                adminMail);
    }

    @Scheduled(cron = "0 40 9 * * MON-FRI", zone = "Asia/Kolkata")
    @Scheduled(cron = "0 10 20 * * MON-FRI", zone = "Asia/Kolkata")
    public void scheduledIPOUpdateBySlug() {
        log.info("IPO update by Slug scheduler triggered");

        asyncService.updateIposBySlug(adminMail);
    }
}
