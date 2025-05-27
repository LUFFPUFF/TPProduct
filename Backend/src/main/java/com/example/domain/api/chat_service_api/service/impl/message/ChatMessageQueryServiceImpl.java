package com.example.domain.api.chat_service_api.service.impl.message;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.domain.api.chat_service_api.exception_handler.ChatNotFoundException;
import com.example.domain.api.chat_service_api.mapper.ChatMessageMapper;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.service.chat.IChatQueryService;
import com.example.domain.api.chat_service_api.service.message.IChatMessageQueryService;
import com.example.domain.api.chat_service_api.util.ChatValidationUtil;
import com.example.domain.api.chat_service_api.util.MdcUtil;
import com.example.domain.security.model.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageQueryServiceImpl implements IChatMessageQueryService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final IChatQueryService chatQueryService;
    private final ChatValidationUtil chatValidationUtil;

    private static final String OPERATION_GET_MESSAGES = "getMessagesByChatId";
    private static final String OPERATION_FIND_FIRST_MESSAGE = "findFirstMessageEntityByChatId";

    private static final String KEY_CHAT_ID = "chatId";
    private static final String KEY_USER_ID = "userId";

    @Override
    public List<MessageDto> getMessagesByChatId(Integer chatId, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    log.debug("User {} requesting messages for chat ID {}", userContext.getUserId(), chatId);

                    Chat chat = chatQueryService.findChatEntityById(chatId)
                            .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found."));

                    chatValidationUtil.ensureChatBelongsToCompany(chat, userContext.getCompanyId(), OPERATION_GET_MESSAGES);


                    List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderBySentAtAsc(chatId);

                    return messages.stream()
                            .map(chatMessageMapper::toDto)
                            .collect(Collectors.toList());
                },
                "operation", OPERATION_GET_MESSAGES,
                KEY_CHAT_ID, chatId,
                KEY_USER_ID, userContext.getUserId()
        );
    }

    @Override
    public Optional<ChatMessage> findFirstMessageEntityByChatId(Integer chatId, UserContext userContext) throws AccessDeniedException {
        return MdcUtil.withContext(
                () -> {
                    String requester = (userContext != null && userContext.getUserId() != null)
                            ? "User " + userContext.getUserId()
                            : "System/Unknown";
                    log.debug("{} requesting first message for chat ID {}", requester, chatId);

                    if (userContext != null) {
                        Chat chat = chatQueryService.findChatEntityById(chatId)
                                .orElseThrow(() -> new ChatNotFoundException("Chat with ID " + chatId + " not found when finding first message."));
                        chatValidationUtil.ensureChatBelongsToCompany(chat, userContext.getCompanyId(), OPERATION_FIND_FIRST_MESSAGE);
                    }

                    Optional<ChatMessage> firstMessage = chatMessageRepository.findFirstByChatIdOrderBySentAtAsc(chatId);
                    if (firstMessage.isPresent()) {
                        log.info("Found first message ID {} for chat ID {}", firstMessage.get().getId(), chatId);
                    } else {
                        log.info("No messages found for chat ID {}", chatId);
                    }
                    return firstMessage;
                },
                "operation", OPERATION_FIND_FIRST_MESSAGE,
                KEY_CHAT_ID, chatId,
                KEY_USER_ID, (userContext != null ? userContext.getUserId() : "System")
        );
    }
}
