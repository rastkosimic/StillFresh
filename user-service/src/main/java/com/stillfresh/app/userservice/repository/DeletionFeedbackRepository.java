package com.stillfresh.app.userservice.repository;

import com.stillfresh.app.userservice.model.DeletionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletionFeedbackRepository extends JpaRepository<DeletionFeedback, Long> {
}
