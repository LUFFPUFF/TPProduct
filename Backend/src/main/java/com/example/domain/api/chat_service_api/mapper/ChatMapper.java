package com.example.domain.api.chat_service_api.mapper;

import com.example.database.model.chats_messages_module.ChatAttachment;
import com.example.database.model.chats_messages_module.ChatMessage;
import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.dto.ChatAttachmentDto;
import com.example.domain.dto.MessageDto;
import com.example.domain.dto.ChatDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    @Mapping(source = "clientDto", target = "client")
    @Mapping(source = "userDto", target = "user")
    @Mapping(source = "chatChannel", target = "chatChannel")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdAt", target = "createdAt")
    Chat toEntityChat(ChatDto chatDto);

    @Mapping(source = "client", target = "clientDto")
    @Mapping(source = "user", target = "userDto")
    @Mapping(source = "chatChannel", target = "chatChannel")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdAt", target = "createdAt")
    ChatDto toDtoChat(Chat chat);

    @Mapping(source = "chatDto", target = "chat")
    @Mapping(source = "content", target = "content")
    @Mapping(source = "sentAt", target = "sentAt")
    ChatMessage toEntityChatMessage(MessageDto chatMessageDto);

    @Mapping(source = "chat", target = "chatDto")
    @Mapping(source = "content", target = "content")
    @Mapping(source = "sentAt", target = "sentAt")
    MessageDto toDtoChatMessage(ChatMessage chatMessage);

    @Mapping(source = "messageDto", target = "chatMessage")
    @Mapping(source = "fileUrl", target = "fileUrl")
    @Mapping(source = "fileType", target = "fileType")
    ChatAttachment toEntityChatAttachment(ChatAttachmentDto chatAttachmentDto);

    @Mapping(source = "chatMessage", target = "messageDto")
    @Mapping(source = "fileUrl", target = "fileUrl")
    @Mapping(source = "fileType", target = "fileType")
    ChatAttachmentDto toDtoChatAttachment(ChatAttachment chatAttachment);

    default Date map(LocalDateTime value) {
        return value == null ? null : Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    default LocalDateTime map(Date value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

    default Client mapClient(Integer clientId) {
        if (clientId == null) {
            return null;
        }
        Client client = new Client();
        client.setId(clientId);
        return client;
    }

    default User mapUser(Integer userId) {
        if (userId == null) {
            return null;
        }
        User user = new User();
        user.setId(userId);
        return user;
    }

    default Chat mapChat(Integer chatId) {
        if (chatId == null) {
            return null;
        }
        Chat chat = new Chat();
        chat.setId(chatId);
        return chat;
    }

    default ChatMessage mapChatMessage(Integer chatMessageId) {
        if (chatMessageId == null) {
            return null;
        }
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(chatMessageId);
        return chatMessage;
    }
}

