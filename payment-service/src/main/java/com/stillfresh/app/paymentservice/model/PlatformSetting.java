package com.stillfresh.app.paymentservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Simple key/value store for runtime-editable platform settings (e.g. the global platform fee
 * percentage). Single row per key; current value only (overwritten on change).
 */
@Entity
@Table(name = "platform_settings")
public class PlatformSetting {

    @Id
    @Column(name = "setting_key", length = 64, nullable = false)
    private String key;

    @Column(name = "setting_value", length = 255, nullable = false)
    private String value;

    public PlatformSetting() {}

    public PlatformSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
