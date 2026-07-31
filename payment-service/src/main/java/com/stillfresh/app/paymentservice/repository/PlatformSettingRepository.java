package com.stillfresh.app.paymentservice.repository;

import com.stillfresh.app.paymentservice.model.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, String> {
}
