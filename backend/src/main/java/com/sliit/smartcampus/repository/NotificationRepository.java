package com.sliit.smartcampus.repository;

import com.sliit.smartcampus.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    long countByUserIdAndReadFlagFalse(String userId);

    void deleteByUserId(String userId);
}
