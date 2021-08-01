package com.studyolle.modules.study.event;

import com.studyolle.modules.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
