package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.PlatformSetting;
import com.stillfresh.app.paymentservice.repository.PlatformSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Runtime control for the automatic payout pipeline (approve + submit after
 * batch creation). Admins can pause/resume without redeploying.
 */
@Service
public class PayoutAutoControlService {

    private static final Logger logger = LoggerFactory.getLogger(PayoutAutoControlService.class);
    public static final String AUTO_ENABLED_KEY = "payout_auto_enabled";

    @Autowired
    private PlatformSettingRepository repository;

    @Value("${payout.auto.enabled:true}")
    private boolean defaultAutoEnabled;

    public boolean isAutoEnabled() {
        return repository.findById(AUTO_ENABLED_KEY)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(defaultAutoEnabled);
    }

    public void setAutoEnabled(boolean enabled) {
        PlatformSetting setting = repository.findById(AUTO_ENABLED_KEY)
                .orElseGet(() -> new PlatformSetting(AUTO_ENABLED_KEY, null));
        setting.setValue(Boolean.toString(enabled));
        repository.save(setting);
        logger.info("Payout auto pipeline {}", enabled ? "enabled" : "paused");
    }
}
