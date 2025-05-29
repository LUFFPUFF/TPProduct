package com.example.domain.api.ans_api_module.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatMessageSenderType;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.crm_module.client.Client;
import com.example.domain.api.ans_api_module.answer_finder.domain.dto.PredefinedAnswerDto; // Правильный импорт
import com.example.domain.api.ans_api_module.answer_finder.dto.AnswerSearchResultItem;
import com.example.domain.api.ans_api_module.answer_finder.service.AnswerSearchService;
import com.example.domain.api.ans_api_module.generation.exception.MLException;
import com.example.domain.api.ans_api_module.generation.model.enums.GenerationType;
import com.example.domain.api.ans_api_module.event.AutoResponderEscalationEvent;
import com.example.domain.api.ans_api_module.exception.AutoResponderException;
import com.example.domain.api.ans_api_module.generation.service.ITextGenerationService;
import com.example.domain.api.chat_service_api.exception_handler.exception.ExternalMessagingException;
import com.example.domain.api.chat_service_api.integration.service.IExternalMessagingService;
import com.example.domain.api.chat_service_api.mapper.ChatMessageMapper;
import com.example.domain.api.chat_service_api.model.dto.ChatDTO;
import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import com.example.domain.api.chat_service_api.model.dto.client.ClientInfoDTO;
import com.example.domain.api.chat_service_api.model.rest.mesage.SendMessageRequestDTO;
import com.example.domain.api.chat_service_api.service.IChatMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;


import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutoResponderServiceImplTest {

    @Mock private AnswerSearchService answerSearchService;
    @Mock private ITextGenerationService textProcessingService;
    @Mock private IChatMessageService chatMessageService;
    @Mock private ChatMessageMapper messageMapper;
    @Mock private IExternalMessagingService externalMessagingService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AutoResponderServiceImpl autoResponderService;

    @Captor private ArgumentCaptor<SendMessageRequestDTO> saveMessageRequestCaptor;
    @Captor private ArgumentCaptor<String> externalMessageContentCaptor;
    @Captor private ArgumentCaptor<AutoResponderEscalationEvent> escalationEventCaptor;
    @Captor private ArgumentCaptor<GenerationType> generationTypeCaptor;

    private Chat testChat;
    private MessageDto testMessageDto;
    private ChatMessage testChatMessage;
    private Client testClient;
    private Company testCompany;
    private final Integer CHAT_ID = 1;
    private final Integer CLIENT_ID = 5;
    private final Integer COMPANY_ID = 10;
    private final String CLIENT_QUERY = "Test query";
    private final String CORRECTED_QUERY = "Corrected test query";
    private final String ORIGINAL_ANSWER = "Original answer text";
    private final String REWRITTEN_ANSWER = "Rewritten answer text";
    private final String ERROR_MESSAGE_FOR_CLIENT = "Извините, возникла проблема при обработке вашего запроса. Передаю ваш вопрос оператору.";
    private final String UNEXPECTED_ERROR_MESSAGE_FOR_CLIENT = "Извините, произошла непредвиденная ошибка. Передаю ваш вопрос оператору.";
    private final String clientPreviousMessages = "Привет";


    @BeforeEach
    void setUp() throws AccessDeniedException {
        reset(answerSearchService, textProcessingService, chatMessageService, messageMapper,
                externalMessagingService, eventPublisher);

        testCompany = new Company();
        testCompany.setId(COMPANY_ID);
        testClient = new Client();
        testClient.setId(CLIENT_ID);
        testChat = new Chat();
        testChat.setId(CHAT_ID);
        testChat.setStatus(ChatStatus.PENDING_AUTO_RESPONDER);
        testChat.setClient(testClient);
        testChat.setCompany(testCompany);

        ChatDTO chatDTO = new ChatDTO();
        chatDTO.setId(CHAT_ID);
        ClientInfoDTO clientInfoDTO = new ClientInfoDTO();
        clientInfoDTO.setId(CLIENT_ID);
        chatDTO.setClient(clientInfoDTO);

        testMessageDto = new MessageDto();
        testMessageDto.setId(101);
        testMessageDto.setContent(CLIENT_QUERY);
        testMessageDto.setChatDto(chatDTO);
        testMessageDto.setSenderType(ChatMessageSenderType.CLIENT);

        testChatMessage = new ChatMessage();
        testChatMessage.setId(101);
        testChatMessage.setContent(CLIENT_QUERY);
        testChatMessage.setChat(testChat);
        testChatMessage.setSenderClient(testClient);
        testChatMessage.setSenderType(ChatMessageSenderType.CLIENT);

        when(chatMessageService.findChatEntityById(CHAT_ID)).thenReturn(Optional.of(testChat));
        when(chatMessageService.findFirstMessageByChatId(CHAT_ID)).thenReturn(Optional.of(testChatMessage));
        when(messageMapper.toDto(testChatMessage)).thenReturn(testMessageDto);
        when(textProcessingService.processQuery(eq(CLIENT_QUERY), eq(GenerationType.CORRECTION), clientPreviousMessages))
                .thenReturn(CORRECTED_QUERY);
        when(textProcessingService.processQuery(eq(ORIGINAL_ANSWER), eq(GenerationType.REWRITE), clientPreviousMessages))
                .thenReturn(REWRITTEN_ANSWER);
    }

    @Test
    void processNewPendingChat_ChatNotFound_ShouldThrowException() throws AccessDeniedException {
        when(chatMessageService.findChatEntityById(CHAT_ID)).thenReturn(Optional.empty());
        AutoResponderException exception = assertThrows(AutoResponderException.class,
                () -> autoResponderService.processNewPendingChat(CHAT_ID));
        assertEquals("AutoResponder: Chat ID " + CHAT_ID + " not found.", exception.getMessage());
        verify(chatMessageService).findChatEntityById(CHAT_ID);
        verify(chatMessageService, never()).findFirstMessageByChatId(anyInt());
    }

    @Test
    void processNewPendingChat_WrongStatus_ShouldLogAndReturn() throws AutoResponderException, AccessDeniedException {
        testChat.setStatus(ChatStatus.IN_PROGRESS);
        when(chatMessageService.findChatEntityById(CHAT_ID)).thenReturn(Optional.of(testChat));

        autoResponderService.processNewPendingChat(CHAT_ID);

        verify(chatMessageService).findChatEntityById(CHAT_ID);
        verify(chatMessageService, never()).findFirstMessageByChatId(anyInt());
        // ИСПРАВЛЕНО: Удалена строка verify(autoResponderService...)
        verifyNoInteractions(messageMapper, textProcessingService, answerSearchService, externalMessagingService, eventPublisher);
    }


    @Test
    void processNewPendingChat_WhenNoFirstMessage_ShouldNotFailAndNotProcess() throws AutoResponderException, AccessDeniedException {
        when(chatMessageService.findFirstMessageByChatId(CHAT_ID)).thenReturn(Optional.empty());

        autoResponderService.processNewPendingChat(CHAT_ID);

        verify(chatMessageService).findChatEntityById(CHAT_ID);
        verify(chatMessageService).findFirstMessageByChatId(CHAT_ID);
        verify(messageMapper, never()).toDto(any());
        // ИСПРАВЛЕНО: Удалена строка verify(autoResponderService...)
        verifyNoInteractions(textProcessingService, answerSearchService, externalMessagingService, eventPublisher);
    }

    @Test
    void processNewPendingChat_MessageMapperThrowsException_ShouldThrowAutoResponderException() {
        RuntimeException mapperException = new RuntimeException("Mapping failed");
        when(messageMapper.toDto(testChatMessage)).thenThrow(mapperException);

        AutoResponderException exception = assertThrows(AutoResponderException.class,
                () -> autoResponderService.processNewPendingChat(CHAT_ID));
        assertEquals("Error during initial auto-responder processing for chat " + CHAT_ID, exception.getMessage());
        assertEquals(mapperException, exception.getCause());
    }


    // --- Тесты для processIncomingMessage (остаются без изменений) ---
    @Test
    void processIncomingMessage_ShouldProcessCorrectAndSendRewrittenAnswer() throws Exception {
        PredefinedAnswerDto answerDto = new PredefinedAnswerDto();
        answerDto.setAnswer(ORIGINAL_ANSWER);
        AnswerSearchResultItem searchResult = new AnswerSearchResultItem(answerDto, 0.9);
        when(answerSearchService.findRelevantAnswers(CORRECTED_QUERY, COMPANY_ID, null)).thenReturn(List.of(searchResult));

        autoResponderService.processIncomingMessage(testMessageDto, testChat);

        verify(textProcessingService).processQuery(CLIENT_QUERY, GenerationType.CORRECTION, clientPreviousMessages);
        verify(answerSearchService).findRelevantAnswers(CORRECTED_QUERY, COMPANY_ID, null);
        verify(textProcessingService).processQuery(ORIGINAL_ANSWER, GenerationType.REWRITE, clientPreviousMessages);
        verify(chatMessageService).processAndSaveMessage(saveMessageRequestCaptor.capture(), eq(CLIENT_ID), eq(ChatMessageSenderType.AUTO_RESPONDER));
        verify(externalMessagingService).sendMessageToExternal(eq(CHAT_ID), externalMessageContentCaptor.capture());
        assertEquals(REWRITTEN_ANSWER, externalMessageContentCaptor.getValue());
        assertEquals(CLIENT_ID, saveMessageRequestCaptor.getValue().getSenderId());
        assertEquals(REWRITTEN_ANSWER, saveMessageRequestCaptor.getValue().getContent());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void processIncomingMessage_ShouldUseOriginalAnswer_WhenRewriteFails() throws Exception {
        PredefinedAnswerDto answerDto = new PredefinedAnswerDto();
        answerDto.setAnswer(ORIGINAL_ANSWER);
        AnswerSearchResultItem searchResult = new AnswerSearchResultItem(answerDto, 0.9);
        when(answerSearchService.findRelevantAnswers(CORRECTED_QUERY, COMPANY_ID, null)).thenReturn(List.of(searchResult));
        MLException rewriteException = new MLException("Rewrite failed", 500);
        when(textProcessingService.processQuery(eq(ORIGINAL_ANSWER), eq(GenerationType.REWRITE), clientPreviousMessages)).thenThrow(rewriteException);

        autoResponderService.processIncomingMessage(testMessageDto, testChat);

        verify(textProcessingService).processQuery(CLIENT_QUERY, GenerationType.CORRECTION, clientPreviousMessages);
        verify(answerSearchService).findRelevantAnswers(CORRECTED_QUERY, COMPANY_ID, null);
        verify(textProcessingService).processQuery(ORIGINAL_ANSWER, GenerationType.REWRITE, clientPreviousMessages);
        verify(chatMessageService).processAndSaveMessage(saveMessageRequestCaptor.capture(), eq(CLIENT_ID), eq(ChatMessageSenderType.AUTO_RESPONDER));
        verify(externalMessagingService).sendMessageToExternal(eq(CHAT_ID), externalMessageContentCaptor.capture());
        assertEquals(ORIGINAL_ANSWER, externalMessageContentCaptor.getValue());
        assertEquals(ORIGINAL_ANSWER, saveMessageRequestCaptor.getValue().getContent());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void processIncomingMessage_WrongStatus_ShouldDoNothing() throws AutoResponderException {
        testChat.setStatus(ChatStatus.ASSIGNED);
        autoResponderService.processIncomingMessage(testMessageDto, testChat);
        verifyNoInteractions(textProcessingService, answerSearchService, externalMessagingService, eventPublisher, chatMessageService);
    }

    @Test
    void processIncomingMessage_NotClientSender_ShouldDoNothing() throws AutoResponderException {
        testMessageDto.setSenderType(ChatMessageSenderType.OPERATOR);
        autoResponderService.processIncomingMessage(testMessageDto, testChat);
        verifyNoInteractions(textProcessingService, answerSearchService, externalMessagingService, eventPublisher, chatMessageService);
    }

    @Test
    void processIncomingMessage_EmptyQuery_ShouldDoNothing() throws AutoResponderException {
        testMessageDto.setContent("   ");
        autoResponderService.processIncomingMessage(testMessageDto, testChat);
        verifyNoInteractions(textProcessingService, answerSearchService, externalMessagingService, eventPublisher, chatMessageService);
    }

    @Test
    void processIncomingMessage_UnexpectedException_ShouldEscalateAndSendErrorMessage() throws Exception {
        RuntimeException unexpectedException = new RuntimeException("Something went wrong");
        when(answerSearchService.findRelevantAnswers(CORRECTED_QUERY, COMPANY_ID, null)).thenThrow(unexpectedException);

        AutoResponderException exception = assertThrows(AutoResponderException.class,
                () -> autoResponderService.processIncomingMessage(testMessageDto, testChat));
        assertEquals("Unexpected error in auto-responder processing for chat " + CHAT_ID, exception.getMessage());
        assertEquals(unexpectedException, exception.getCause());
        verify(eventPublisher).publishEvent(escalationEventCaptor.capture());
        assertEquals(CHAT_ID, escalationEventCaptor.getValue().getChatId());
        assertEquals(CLIENT_ID, escalationEventCaptor.getValue().getClientId());
        verify(chatMessageService).processAndSaveMessage(saveMessageRequestCaptor.capture(), eq(CLIENT_ID), eq(ChatMessageSenderType.AUTO_RESPONDER));
        verify(externalMessagingService).sendMessageToExternal(eq(CHAT_ID), externalMessageContentCaptor.capture());
        assertEquals(UNEXPECTED_ERROR_MESSAGE_FOR_CLIENT, externalMessageContentCaptor.getValue());
    }


    @Test
    void processIncomingMessage_ExternalMessagingFails_ShouldLogButNotThrow() throws Exception {
        PredefinedAnswerDto answerDto = new PredefinedAnswerDto();
        answerDto.setAnswer(ORIGINAL_ANSWER);
        AnswerSearchResultItem searchResult = new AnswerSearchResultItem(answerDto, 0.9);
        when(answerSearchService.findRelevantAnswers(CORRECTED_QUERY, COMPANY_ID, null)).thenReturn(List.of(searchResult));
        when(textProcessingService.processQuery(eq(ORIGINAL_ANSWER), eq(GenerationType.REWRITE), clientPreviousMessages)).thenReturn(REWRITTEN_ANSWER);
        ExternalMessagingException messagingException = new ExternalMessagingException("Send failed");
        doThrow(messagingException).when(externalMessagingService).sendMessageToExternal(eq(CHAT_ID), eq(REWRITTEN_ANSWER));

        assertDoesNotThrow(() -> autoResponderService.processIncomingMessage(testMessageDto, testChat));

        verify(chatMessageService).processAndSaveMessage(any(), any(), any());
        verify(externalMessagingService).sendMessageToExternal(eq(CHAT_ID), eq(REWRITTEN_ANSWER));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void processIncomingMessage_InternalSaveFails_ShouldLogAndAttemptExternalSend() throws Exception {
        PredefinedAnswerDto answerDto = new PredefinedAnswerDto();
        answerDto.setAnswer(ORIGINAL_ANSWER);
        AnswerSearchResultItem searchResult = new AnswerSearchResultItem(answerDto, 0.9);
        when(answerSearchService.findRelevantAnswers(CORRECTED_QUERY, COMPANY_ID, null)).thenReturn(List.of(searchResult));
        when(textProcessingService.processQuery(eq(ORIGINAL_ANSWER), eq(GenerationType.REWRITE), clientPreviousMessages)).thenReturn(REWRITTEN_ANSWER);
        RuntimeException saveException = new RuntimeException("DB error");
        when(chatMessageService.processAndSaveMessage(any(), any(), any())).thenThrow(saveException);

        assertDoesNotThrow(() -> autoResponderService.processIncomingMessage(testMessageDto, testChat));

        verify(chatMessageService).processAndSaveMessage(any(), any(), any());
        verify(externalMessagingService).sendMessageToExternal(eq(CHAT_ID), eq(REWRITTEN_ANSWER));
        verify(eventPublisher, never()).publishEvent(any());
    }


    // --- Тест для stopForChat ---
    @Test
    void stopForChat_ShouldCompleteWithoutErrors() {
        assertDoesNotThrow(() -> autoResponderService.stopForChat(1));
    }
}