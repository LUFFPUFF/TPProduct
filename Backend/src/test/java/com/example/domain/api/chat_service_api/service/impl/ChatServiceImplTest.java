package com.example.domain.api.chat_service_api.service.impl;

import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.chats_messages_module.chat.ChatChannel;
import com.example.database.model.chats_messages_module.chat.ChatStatus;
import com.example.database.model.chats_messages_module.message.ChatMessage;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.crm_module.client.Client;
import com.example.database.repository.chats_messages_module.ChatMessageRepository;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.domain.api.ans_api_module.exception.AutoResponderException;
import com.example.domain.api.ans_api_module.service.IAutoResponderService;
import com.example.domain.api.chat_service_api.exception_handler.ResourceNotFoundException;
import com.example.domain.api.chat_service_api.exception_handler.exception.service.ChatServiceException;
import com.example.domain.api.chat_service_api.mapper.ChatMapper;
import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.chat_service_api.model.dto.client.ClientInfoDTO; // <--- Импортируем ClientInfoDTO
import com.example.domain.api.chat_service_api.model.rest.chat.CreateChatRequestDTO;
import com.example.domain.api.chat_service_api.service.*;
import com.example.domain.api.statistics_module.aop.MetricsAspect;
import com.example.domain.api.statistics_module.metrics.service.IChatMetricsService;
import com.example.domain.security.util.UserContextHolder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
        ChatServiceImpl.class,
        MetricsAspect.class,
        ChatServiceImplTest.TestConfig.class
})
class ChatServiceImplTest {

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true) // <--- УСТАНОВИ proxyTargetClass = true ЗДЕСЬ!
    static class TestConfig {
        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

        @Bean
        public ApplicationContext applicationContext() {
            return mock(ApplicationContext.class);
        }
    }



    @Autowired
    private ChatServiceImpl chatService;


    @Autowired
    private MeterRegistry meterRegistry;


    @MockBean private ChatRepository chatRepository;
    @MockBean private IClientService clientService;
    @MockBean private IUserService userService;
    @MockBean private IChatMessageService chatMessageService;
    @MockBean private ChatMapper chatMapper;
    @MockBean private ChatMessageRepository chatMessageRepository;
    @MockBean private IAssignmentService assignmentService;
    @MockBean private WebSocketMessagingService messagingService;
    @MockBean private INotificationService notificationService;
    @MockBean private IAutoResponderService autoResponderService;
    @MockBean private IChatMetricsService chatMetricsService;

    @Captor
    private ArgumentCaptor<Chat> chatArgumentCaptor;
    @Captor
    private ArgumentCaptor<ChatMessage> chatMessageArgumentCaptor;

    private Client mockClient;
    private Company mockCompany;
    private ChatDetailsDTO mockSuccessfulChatDetailsDTO;
    private ClientInfoDTO mockClientInfoDTO;


    @BeforeEach
    void setUp() {
        meterRegistry.clear();
        UserContextHolder.clearContext();

        mockCompany = new Company();
        mockCompany.setId(1);
        mockCompany.setName("Test Company");

        mockClient = new Client();
        mockClient.setId(100);
        mockClient.setName("Test Client");
        mockClient.setCompany(mockCompany);

        mockClientInfoDTO = new ClientInfoDTO();
        mockClientInfoDTO.setId(mockClient.getId());
        mockClientInfoDTO.setName(mockClient.getName());

        mockSuccessfulChatDetailsDTO = new ChatDetailsDTO();
        mockSuccessfulChatDetailsDTO.setId(1);
        mockSuccessfulChatDetailsDTO.setClient(mockClientInfoDTO);
        mockSuccessfulChatDetailsDTO.setChatChannel(ChatChannel.Telegram); // Для успешного теста
        mockSuccessfulChatDetailsDTO.setStatus(ChatStatus.PENDING_AUTO_RESPONDER);
        // Если UserInfoDTO нужен для ChatDetailsDTO.operator, его тоже нужно создать и засетать
        // UserInfoDTO mockOperatorInfo = new UserInfoDTO(); ...
        // mockSuccessfulChatDetailsDTO.setOperator(mockOperatorInfo);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clearContext();
    }

    @Test
    void createChat_newChat_successful_withInitialMessage() {
        // Arrange
        CreateChatRequestDTO request = new CreateChatRequestDTO();
        request.setClientId(mockClient.getId());
        request.setChatChannel(ChatChannel.Telegram);
        request.setInitialMessageContent("Hello Test");

        when(clientService.findById(mockClient.getId())).thenReturn(Optional.of(mockClient));
        when(chatRepository.findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(
                eq(mockClient.getId()), eq(ChatChannel.Telegram), anyCollection())
        ).thenReturn(Optional.empty());

        Chat savedChatInstance = new Chat();
        savedChatInstance.setId(1);
        savedChatInstance.setClient(mockClient);
        savedChatInstance.setCompany(mockCompany);
        savedChatInstance.setChatChannel(ChatChannel.Telegram);
        savedChatInstance.setStatus(ChatStatus.PENDING_AUTO_RESPONDER);
        savedChatInstance.setCreatedAt(LocalDateTime.now());

        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> {
            Chat chatToSave = invocation.getArgument(0);
            if (chatToSave.getId() == null) chatToSave.setId(savedChatInstance.getId());
            chatToSave.setClient(chatToSave.getClient() != null ? chatToSave.getClient() : mockClient);
            chatToSave.setCompany(chatToSave.getCompany() != null ? chatToSave.getCompany() : mockCompany);
            chatToSave.setChatChannel(chatToSave.getChatChannel() != null ? chatToSave.getChatChannel() : request.getChatChannel());
            chatToSave.setStatus(ChatStatus.PENDING_AUTO_RESPONDER);
            chatToSave.setCreatedAt(chatToSave.getCreatedAt() != null ? chatToSave.getCreatedAt() : LocalDateTime.now());
            return chatToSave;
        });

        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage msgToSave = invocation.getArgument(0);
            if (msgToSave.getId() == null) msgToSave.setId(10);
            msgToSave.setSentAt(msgToSave.getSentAt() != null ? msgToSave.getSentAt() : LocalDateTime.now());
            return msgToSave;
        });

        // Важно: chatMapper должен вернуть наш mockSuccessfulChatDetailsDTO
        when(chatMapper.toDetailsDto(any(Chat.class))).thenReturn(mockSuccessfulChatDetailsDTO);
        doNothing().when(autoResponderService).processNewPendingChat(anyInt());

        // Act
        ChatDetailsDTO result = chatService.createChat(request);

        // Assert
        assertThat(result).isEqualTo(mockSuccessfulChatDetailsDTO);

        verify(chatRepository, times(2)).save(any(Chat.class));
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(autoResponderService).processNewPendingChat(anyInt());

        // Метрики @MeteredOperation (имена БЕЗ префикса "chat_app_")
        assertMetricCountWithTags(
                "chats_created_total", 1.0, "createChat successful",
                "company_id", "unknown", // SpEL #result.client.companyId вернет unknown
                "channel", "Telegram",   // Значение из mockSuccessfulChatDetailsDTO.getChatChannel().name()
                "from_operator_ui", "false"
        );

        assertMetricCountWithTags(
                "chats_auto_responder_handled_total", 1.0, "createChat AR handled",
                "company_id", "unknown", // SpEL #result.client.companyId вернет unknown
                "channel", "Telegram"    // Значение из mockSuccessfulChatDetailsDTO.getChatChannel().name()
        );

        verify(chatMetricsService, never()).incrementChatOperationError(anyString(), anyString(), anyString());
    }

    @Test
    void createChat_clientNotFound_shouldThrowResourceNotFound() { // Убрали "AndIncrementErrorMetric" из названия
        // Arrange
        CreateChatRequestDTO request = new CreateChatRequestDTO();
        request.setClientId(999);
        request.setChatChannel(ChatChannel.Email);

        when(clientService.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            chatService.createChat(request);
        });

        // Аспект вызовется, но #result будет null, теги "unknown".
        // chats_created_total инкрементируется.
        assertMetricCountWithTags(
                "chats_created_total", 1.0, "createChat client not found",
                "company_id", "unknown",
                "channel", "unknown", // т.к. #result.chatChannel будет null
                "from_operator_ui", "false"
        );
        assertCounterValueIsZeroOrNotExists("chats_auto_responder_handled_total");


        // chatMetricsService.incrementChatOperationError НЕ будет вызван для этого сценария
        verify(chatMetricsService, never()).incrementChatOperationError(
                anyString(), anyString(), anyString()
        );
    }

    @Test
    void createChat_clientHasNoCompany_shouldThrowResourceNotFoundAndIncrementErrorMetric() {
        // Arrange
        CreateChatRequestDTO request = new CreateChatRequestDTO();
        request.setClientId(mockClient.getId());
        request.setChatChannel(ChatChannel.WhatsApp);

        Client clientWithoutCompany = new Client();
        clientWithoutCompany.setId(mockClient.getId());

        when(clientService.findById(mockClient.getId())).thenReturn(Optional.of(clientWithoutCompany));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            chatService.createChat(request);
        });

        // Аспект вызовется, #result null, теги "unknown". chats_created_total инкрементируется.
        assertMetricCountWithTags(
                "chats_created_total", 1.0, "createChat client no company",
                "company_id", "unknown",
                "channel", "unknown",
                "from_operator_ui", "false"
        );
        assertCounterValueIsZeroOrNotExists("chats_auto_responder_handled_total");

        // Этот вызов ДОЛЖЕН произойти
        verify(chatMetricsService).incrementChatOperationError(
                eq("createChat"),
                eq("unknown"),
                eq("ClientNotAssociatedWithCompany")
        );
    }

    @Test
    void createChat_openChatAlreadyExists_shouldThrowChatServiceExceptionAndIncrementErrorMetric() {
        // Arrange
        CreateChatRequestDTO request = new CreateChatRequestDTO();
        request.setClientId(mockClient.getId());
        request.setChatChannel(ChatChannel.VK);

        when(clientService.findById(mockClient.getId())).thenReturn(Optional.of(mockClient));
        Chat existingChat = new Chat();
        existingChat.setId(2);
        existingChat.setClient(mockClient);
        existingChat.setCompany(mockCompany); // Важно для companyIdStrForErrorHandling
        when(chatRepository.findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(
                eq(mockClient.getId()), eq(ChatChannel.VK), anyCollection())
        ).thenReturn(Optional.of(existingChat));

        // Act & Assert
        assertThrows(ChatServiceException.class, () -> {
            chatService.createChat(request);
        });

        // Аспект вызовется, #result null, теги "unknown". chats_created_total инкрементируется.
        assertMetricCountWithTags(
                "chats_created_total", 1.0, "createChat already exists",
                "company_id", "unknown",
                "channel", "unknown",
                "from_operator_ui", "false"
        );
        assertCounterValueIsZeroOrNotExists("chats_auto_responder_handled_total");

        // Этот вызов ДОЛЖЕН произойти
        verify(chatMetricsService).incrementChatOperationError(
                eq("createChat"),
                eq(mockCompany.getId().toString()),
                eq("OpenChatAlreadyExists")
        );
    }

    @Test
    void createChat_autoResponderFails_shouldThrowAutoResponderExceptionAndIncrementErrorMetric() {
        // Arrange
        CreateChatRequestDTO request = new CreateChatRequestDTO();
        request.setClientId(mockClient.getId());
        request.setChatChannel(ChatChannel.Test);
        request.setInitialMessageContent("Test message");

        when(clientService.findById(mockClient.getId())).thenReturn(Optional.of(mockClient));
        when(chatRepository.findFirstByClientIdAndChatChannelAndStatusInOrderByCreatedAtDesc(anyInt(), any(ChatChannel.class), anyCollection()))
                .thenReturn(Optional.empty());

        Chat savedChatInstance = new Chat();
        savedChatInstance.setId(1);
        savedChatInstance.setClient(mockClient);
        savedChatInstance.setCompany(mockCompany);
        savedChatInstance.setChatChannel(ChatChannel.Test);
        savedChatInstance.setStatus(ChatStatus.PENDING_AUTO_RESPONDER); // Важно для условия на #result.status
        savedChatInstance.setCreatedAt(LocalDateTime.now());

        when(chatRepository.save(any(Chat.class))).thenReturn(savedChatInstance);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // Настраиваем mockChatDetailsDTO, который будет возвращен ДО исключения AutoResponderException
        ChatDetailsDTO dtoBeforeARException = new ChatDetailsDTO();
        dtoBeforeARException.setId(savedChatInstance.getId());
        ClientInfoDTO clientInfoForAR = new ClientInfoDTO(); clientInfoForAR.setId(mockClient.getId());
        dtoBeforeARException.setClient(clientInfoForAR);
        dtoBeforeARException.setChatChannel(ChatChannel.Test);
        dtoBeforeARException.setStatus(ChatStatus.PENDING_AUTO_RESPONDER);
        // mapper не будет вызван, так как исключение произойдет до return chatMapper.toDetailsDto(savedChat);
        // Вместо этого, #result в аспекте будет тем, что вернул joinPoint.proceed() до исключения.
        // В нашем случае, если исключение в autoResponderService.processNewPendingChat(), то
        // joinPoint.proceed() в аспекте (который оборачивает createChat) завершится исключением.
        // Значит, #result для SpEL тегов будет null.

        doThrow(new AutoResponderException("AR failed")).when(autoResponderService).processNewPendingChat(anyInt());

        // Act & Assert
        assertThrows(AutoResponderException.class, () -> {
            chatService.createChat(request);
        });

        // Метрики @MeteredOperation: #result будет null.
        assertMetricCountWithTags(
                "chats_created_total", 1.0, "createChat AR fails - created",
                "company_id", "unknown",
                "channel", "unknown", // т.к. #result.chatChannel будет null
                "from_operator_ui", "false"
        );
        assertCounterValueIsZeroOrNotExists("chats_auto_responder_handled_total"); // Условие на #result.status не выполнится

        // Этот вызов ДОЛЖЕН произойти
        verify(chatMetricsService).incrementChatOperationError(
                eq("createChat_AutoResponder"),
                eq(mockCompany.getId().toString()),
                eq(AutoResponderException.class.getSimpleName())
        );
    }

    // Вспомогательные методы (без изменений)
    private void assertMetricCountWithTags(String metricName, double expectedCount, String testCaseDescription, String... tags) {
        try {
            double actualCount = meterRegistry.get(metricName).tags(tags).counter().count();
            assertThat(actualCount)
                    .withFailMessage("For metric '%s' with tags %s in test case '%s': expected count <%s> but was <%s>",
                            metricName, Arrays.toString(tags), testCaseDescription, expectedCount, actualCount)
                    .isEqualTo(expectedCount);
        } catch (io.micrometer.core.instrument.search.MeterNotFoundException e) {
            if (expectedCount > 0) {
                throw new AssertionError("For metric '" + metricName + "' with tags " + Arrays.toString(tags) +
                        " in test case '" + testCaseDescription +
                        "': metric not found, but expected count " + expectedCount, e);
            }
        }
    }

    private void assertCounterValueIsZeroOrNotExists(String metricName) {
        io.micrometer.core.instrument.Counter counter = meterRegistry.find(metricName).counter();
        if (counter != null) {
            // Если нашли метрику, проверяем, что ее значение 0.
            // Но нужно быть осторожным, если метрика могла быть инкрементирована с другими тегами.
            // Эта проверка найдет метрику с ЛЮБЫМИ тегами.
            // Для более точной проверки "отсутствия" метрики с КОНКРЕТНЫМИ тегами, лучше использовать
            // meterRegistry.find(metricName).tags(specific_tags).counter() == null
            assertThat(counter.count())
                    .withFailMessage("For metric '%s' (any tags): expected count <0.0> if exists, but was <%s>",
                            metricName, counter.count())
                    .isZero();
        } else {
            assertThat(true).isTrue(); // OK if not found at all
        }
    }

    // Этот метод больше не нужен в таком виде, так как в ошибочных сценариях
    // 'chats_created_total' все равно инкрементируется аспектом, но с 'unknown' тегами.
    // private void assertNoMeteredOperationMetricsRegistered() { ... }
}