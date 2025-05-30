package com.example.database.repository.chats_messages_module;

import com.example.database.model.chats_messages_module.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Integer> {
}
