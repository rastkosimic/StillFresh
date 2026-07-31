package com.stillfresh.app.paymentservice.service;

import com.stillfresh.app.paymentservice.model.PlatformSetting;
import com.stillfresh.app.paymentservice.repository.PlatformSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Runtime-editable platform settings. Currently exposes the global platform fee percentage,
 * stored in the {@code platform_settings} table and cached (Redis) so it is read cheaply on
 * every payment. The cache is evicted whenever the value is updated via the admin endpoint.
 *
 * <p>Fee changes affect only new authorizations: the fee is read at preauthorize time and the
 * resulting split is stored on each {@code payment_transactions} row.</p>
 */
@Service
public class PlatformSettingsService {

    private static final Logger logger = LoggerFactory.getLogger(PlatformSettingsService.class);

    public static final String FEE_PERCENT_KEY = "fee_percent";
    public static final String FEE_CACHE = "platformFee";

    @Autowired
    private PlatformSettingRepository repository;

    /** Seed/fallback used only when the {@code fee_percent} row is missing. */
    @Value("${platform.fee.percent:10.0}")
    private double defaultFeePercent;

    /**
     * Returns the current global platform fee percentage (e.g. {@code 10.0} for 10%).
     * Seeds the row with the configured default the first time it is read.
     */
    @Cacheable(value = FEE_CACHE, key = "'fee_percent'")
    public double getFeePercent() {
        return repository.findById(FEE_PERCENT_KEY)
                .map(setting -> parse(setting.getValue()))
                .orElseGet(() -> {
                    double seeded = sanitize(defaultFeePercent);
                    repository.save(new PlatformSetting(FEE_PERCENT_KEY, Double.toString(seeded)));
                    logger.info("Seeded platform fee_percent to default {}%", seeded);
                    return seeded;
                });
    }

    /**
     * Updates the global platform fee percentage and evicts the cache.
     *
     * @param feePercent new fee percentage in the inclusive range [0, 100]
     * @return the persisted value
     * @throws IllegalArgumentException if the value is out of range
     */
    @CacheEvict(value = FEE_CACHE, key = "'fee_percent'")
    public double setFeePercent(double feePercent) {
        if (feePercent < 0.0 || feePercent > 100.0) {
            throw new IllegalArgumentException("feePercent must be between 0 and 100");
        }
        PlatformSetting setting = repository.findById(FEE_PERCENT_KEY)
                .orElseGet(() -> new PlatformSetting(FEE_PERCENT_KEY, null));
        setting.setValue(Double.toString(feePercent));
        repository.save(setting);
        logger.info("Platform fee_percent updated to {}%", feePercent);
        return feePercent;
    }

    private double parse(String value) {
        try {
            return sanitize(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            logger.warn("Invalid stored fee_percent '{}'; falling back to default {}%", value, defaultFeePercent);
            return sanitize(defaultFeePercent);
        }
    }

    private double sanitize(double value) {
        if (value < 0.0) return 0.0;
        if (value > 100.0) return 100.0;
        return value;
    }
}
