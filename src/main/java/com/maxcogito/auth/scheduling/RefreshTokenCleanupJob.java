// src/main/java/com/maxcogito/auth/scheduling/RefreshTokenCleanupJob.java
package com.maxcogito.auth.scheduling;

import com.maxcogito.auth.service.AdminSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final AdminSessionService adminSessionService;

    public RefreshTokenCleanupJob(AdminSessionService adminSessionService) {
        this.adminSessionService = adminSessionService;
    }

    /**
     * Run once a day to delete revoked or expired refresh tokens.
     *
     * Cron format: second minute hour day-of-month month day-of-week
     * Here: 0 15 3 * * *  → every day at 03:15 server time.
     */
    @Scheduled(cron = "${app.tokens.cleanup.cron:0 15 3 * * *}")
    public void cleanupRevokedAndExpiredTokens() {
        int deleted = adminSessionService.purgeRevokedOrExpiredTokens();
        if (deleted > 0) {
            log.info("Daily refresh token cleanup: deleted {} revoked/expired tokens", deleted);
        } else {
            log.debug("Daily refresh token cleanup: nothing to delete");
        }
    }
}

