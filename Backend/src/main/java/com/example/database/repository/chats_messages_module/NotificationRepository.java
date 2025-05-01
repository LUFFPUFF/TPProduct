package com.example.database.repository.chats_messages_module;

import com.example.database.model.chats_messages_module.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Integer userId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);

    Optional<Notification> findByIdAndUserId(Integer id, Integer userId);
}
