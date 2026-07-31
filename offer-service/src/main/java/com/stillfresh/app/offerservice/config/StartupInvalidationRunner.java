package com.stillfresh.app.offerservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.stillfresh.app.offerservice.service.OfferService;

@Component
public class StartupInvalidationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupInvalidationRunner.class);

    @Autowired
    private OfferService offerService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            logger.info("Startup check: invalidating expired offers if any...");
            offerService.invalidateExpiredOffers();
            logger.info("Startup expired-offers invalidation completed.");
        } catch (Exception ex) {
            logger.error("Startup expired-offers invalidation failed: {}", ex.getMessage(), ex);
        }
    }
}

