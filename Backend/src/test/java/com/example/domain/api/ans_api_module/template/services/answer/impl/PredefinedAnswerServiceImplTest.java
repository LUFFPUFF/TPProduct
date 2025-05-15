package com.example.domain.api.ans_api_module.template.services.answer.impl;

import com.example.database.model.ai_module.PredefinedAnswer;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.repository.ai_module.PredefinedAnswerRepository;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.domain.api.ans_api_module.template.dto.request.PredefinedAnswerUploadDto;
import com.example.domain.api.ans_api_module.template.dto.response.AnswerResponse;
import com.example.domain.api.ans_api_module.template.mapper.PredefinedAnswerMapper;
import com.example.domain.dto.CompanyDto;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PredefinedAnswerServiceImplTest {

    @Mock private PredefinedAnswerRepository answerRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PredefinedAnswerMapper answerMapper;

    @InjectMocks
    private PredefinedAnswerServiceImpl predefinedAnswerService;

    @Captor private ArgumentCaptor<PredefinedAnswer> predefinedAnswerCaptor;
    @Captor private ArgumentCaptor<Specification<PredefinedAnswer>> specificationCaptor;
    // dtoCaptor не используется, можно удалить, если не планируется для updateFromDto
    // @Captor private ArgumentCaptor<PredefinedAnswerUploadDto> dtoCaptor;


    private PredefinedAnswerUploadDto testUploadDto;
    private PredefinedAnswer testAnswerEntity;
    private Company testCompany;
    private final Integer COMPANY_ID = 1;
    private final Integer ANSWER_ID = 10;
    private final String CATEGORY = "General";
    // ИСПРАВЛЕНО: Используем title
    private final String TITLE = "Test Title";
    private final String ANSWER_TEXT = "Test Answer.";


    @BeforeEach
    void setUp() {
        reset(answerRepository, companyRepository, answerMapper);

        testCompany = new Company();
        testCompany.setId(COMPANY_ID);
        testCompany.setName("TestCorp");

        CompanyDto companyDto = CompanyDto.builder().id(COMPANY_ID).build();
        testUploadDto = PredefinedAnswerUploadDto.builder()
                .companyDto(companyDto)
                .category(CATEGORY)
                // ИСПРАВЛЕНО: .title() вместо .question()
                .title(TITLE)
                .answer(ANSWER_TEXT)
                // ИСПРАВЛЕНО: tags отсутствует в DTO
                // .tags(List.of("test", "junit"))
                .build();

        testAnswerEntity = new PredefinedAnswer();
        testAnswerEntity.setId(ANSWER_ID);
        testAnswerEntity.setCompany(testCompany);
        testAnswerEntity.setCategory(CATEGORY);
        // ИСПРАВЛЕНО: setTitle() вместо setQuestion()
        testAnswerEntity.setTitle(TITLE);
        testAnswerEntity.setAnswer(ANSWER_TEXT);
        // ИСПРАВЛЕНО: поле tags отсутствует в сущности
        // testAnswerEntity.setTags("test,junit");
        testAnswerEntity.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    private AnswerResponse createExpectedResponse(PredefinedAnswer entity) {
        return new AnswerResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getAnswer(), // ИСПРАВЛЕНО: Используем getAnswer()
                entity.getCategory(),
                entity.getCompany().getName(),
                entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                true
        );
    }

    // --- Тесты для createAnswer ---
    @Test
    void createAnswer_ValidDto_ShouldSaveAndReturnResponse() {
        // Arrange
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(testCompany));
        PredefinedAnswer newEntityNoId = new PredefinedAnswer();
        // newEntityNoId.setCompany(testCompany); // Компания установится сервисом
        newEntityNoId.setCategory(CATEGORY);
        // ИСПРАВЛЕНО: setTitle
        newEntityNoId.setTitle(TITLE);
        newEntityNoId.setAnswer(ANSWER_TEXT);
        // ИСПРАВЛЕНО: tags отсутствует

        when(answerMapper.toEntity(testUploadDto)).thenReturn(newEntityNoId);
        when(answerRepository.save(any(PredefinedAnswer.class))).thenAnswer(invocation -> {
            PredefinedAnswer saved = invocation.getArgument(0);
            saved.setId(ANSWER_ID);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        // Act
        AnswerResponse result = predefinedAnswerService.createAnswer(testUploadDto);

        // Assert
        assertNotNull(result);
        assertEquals(ANSWER_ID, result.getId());
        // ИСПРАВЛЕНО: getAnswer()
        assertEquals(ANSWER_TEXT, result.getAnswer());

        verify(companyRepository).findById(COMPANY_ID);
        verify(answerMapper).toEntity(testUploadDto);
        verify(answerRepository).save(predefinedAnswerCaptor.capture());
        PredefinedAnswer savedInDb = predefinedAnswerCaptor.getValue();
        assertEquals(testCompany, savedInDb.getCompany());
        assertNotNull(savedInDb.getCreatedAt());
    }

    @Test
    void createAnswer_CompanyNotFound_ShouldThrowEntityNotFoundException() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> {
            predefinedAnswerService.createAnswer(testUploadDto);
        });
        assertTrue(ex.getMessage().contains("Company with id " + COMPANY_ID + " not found"));
        verifyNoInteractions(answerMapper, answerRepository);
    }

    // --- Тесты для updateAnswer ---
    @Test
    void updateAnswer_ExistingAnswer_ShouldUpdateAndReturnResponse() {
        // Arrange
        String updatedCategory = "Updated Category";
        String updatedTitle = "Updated Title";
        String updatedAnswerText = "Updated Answer.";

        PredefinedAnswerUploadDto updateDto = PredefinedAnswerUploadDto.builder()
                .companyDto(CompanyDto.builder().id(COMPANY_ID).build()) // Компания обычно не меняется
                .category(updatedCategory)
                .title(updatedTitle)
                .answer(updatedAnswerText)
                // .tags(List.of("updated")) // Убрано, т.к. нет в DTO
                .build();

        // Создаем копию, чтобы не модифицировать testAnswerEntity из setUp для других тестов
        PredefinedAnswer existingAnswer = new PredefinedAnswer();
        existingAnswer.setId(ANSWER_ID);
        existingAnswer.setCompany(testCompany); // Устанавливаем компанию
        existingAnswer.setCategory(testAnswerEntity.getCategory()); // Старая категория
        existingAnswer.setTitle(testAnswerEntity.getTitle());       // Старый title
        existingAnswer.setAnswer(testAnswerEntity.getAnswer());     // Старый answer
        existingAnswer.setCreatedAt(testAnswerEntity.getCreatedAt());// Старая дата

        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(existingAnswer));

        // ИСПРАВЛЕНО: Используем doAnswer для имитации того, что маппер обновляет поля
        // Это важно, так как buildResponseFromEntity будет использовать измененный existingAnswer
        doAnswer(invocation ->   {
            PredefinedAnswerUploadDto dtoArg = invocation.getArgument(0);
            PredefinedAnswer entityArg = invocation.getArgument(1); // Это наш existingAnswer

            // Имитируем обновление полей, как это сделал бы MapStruct с @MappingTarget
            if (dtoArg.getCategory() != null) entityArg.setCategory(dtoArg.getCategory());
            if (dtoArg.getTitle() != null) entityArg.setTitle(dtoArg.getTitle());
            if (dtoArg.getAnswer() != null) entityArg.setAnswer(dtoArg.getAnswer());
            // Другие поля, если они обновляются маппером
            return null; // Метод updateFromDto - void
        }).when(answerMapper).updateFromDto(eq(updateDto), eq(existingAnswer));

        // save должен вернуть тот же (обновленный) экземпляр existingAnswer
        when(answerRepository.save(existingAnswer)).thenReturn(existingAnswer);

        // Act
        AnswerResponse result = predefinedAnswerService.updateAnswer(ANSWER_ID, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(ANSWER_ID, result.getId());
        // Теперь эти проверки должны проходить, так как existingAnswer был "обновлен" моком
        assertEquals(updatedAnswerText, result.getAnswer());
        assertEquals(updatedCategory, result.getCategory());
        assertEquals(updatedTitle, result.getTitle()); // Проверяем и title
        assertEquals(testCompany.getName(), result.getCompanyName()); // Имя компании из buildResponseFromEntity
        // assertEquals(testAnswerEntity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(), result.getCreatedAt()); // Дата создания не должна меняться

        verify(answerRepository).findById(ANSWER_ID);
        verify(answerMapper).updateFromDto(eq(updateDto), eq(existingAnswer));
        verify(answerRepository).save(existingAnswer);
    }

    @Test
    void updateAnswer_NotFound_ShouldThrowEntityNotFoundException() {
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> {
            predefinedAnswerService.updateAnswer(ANSWER_ID, testUploadDto);
        });
        verifyNoInteractions(answerMapper);
        verify(answerRepository, never()).save(any());
    }

    // --- Тесты для deleteAnswer ---
    @Test
    void deleteAnswer_ExistingAnswer_ShouldDelete() {
        when(answerRepository.existsById(ANSWER_ID)).thenReturn(true);
        doNothing().when(answerRepository).deleteById(ANSWER_ID);

        assertDoesNotThrow(() -> predefinedAnswerService.deleteAnswer(ANSWER_ID));

        verify(answerRepository).existsById(ANSWER_ID);
        verify(answerRepository).deleteById(ANSWER_ID);
    }

    @Test
    void deleteAnswer_NotFound_ShouldThrowEntityNotFoundException() {
        when(answerRepository.existsById(ANSWER_ID)).thenReturn(false);
        assertThrows(EntityNotFoundException.class, () -> {
            predefinedAnswerService.deleteAnswer(ANSWER_ID);
        });
        verify(answerRepository).existsById(ANSWER_ID);
        verify(answerRepository, never()).deleteById(anyInt());
    }

    // --- Тесты для getAnswerById ---
    @Test
    void getAnswerById_ExistingAnswer_ShouldReturnResponse() {
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(testAnswerEntity));
        AnswerResponse expectedResponse = createExpectedResponse(testAnswerEntity);

        AnswerResponse result = predefinedAnswerService.getAnswerById(ANSWER_ID);

        assertNotNull(result);
        assertEquals(expectedResponse.getId(), result.getId());
        // ИСПРАВЛЕНО: getAnswer()
        assertEquals(expectedResponse.getAnswer(), result.getAnswer());
    }

    @Test
    void getAnswerById_NotFound_ShouldThrowEntityNotFoundException() {
        when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> {
            predefinedAnswerService.getAnswerById(ANSWER_ID);
        });
    }

    // --- Тесты для searchAnswers ---
    @SuppressWarnings("unchecked")
    @Test
    void searchAnswers_WithAllParams_ShouldCallRepositoryAndMapToPage() {
        Pageable pageable = PageRequest.of(0, 10);
        List<PredefinedAnswer> answerList = List.of(testAnswerEntity);
        Page<PredefinedAnswer> answerPage = new PageImpl<>(answerList, pageable, answerList.size());
        AnswerResponse expectedResponse = createExpectedResponse(testAnswerEntity);

        when(answerRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(answerPage);

        Page<AnswerResponse> result = predefinedAnswerService.searchAnswers("test", COMPANY_ID, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(expectedResponse.getId(), result.getContent().get(0).getId());

        verify(answerRepository).findAll(specificationCaptor.capture(), eq(pageable));
    }

    @Test
    void searchAnswers_NoResults_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(answerRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty(pageable));
        Page<AnswerResponse> result = predefinedAnswerService.searchAnswers("query", COMPANY_ID, pageable);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- Тесты для getAnswersByCategory ---
    @Test
    void getAnswersByCategory_ValidCategory_ShouldReturnList() {
        List<PredefinedAnswer> answerList = List.of(testAnswerEntity);
        when(answerRepository.findByCategoryIgnoreCase(CATEGORY)).thenReturn(answerList);
        AnswerResponse expectedResponse = createExpectedResponse(testAnswerEntity);

        List<AnswerResponse> result = predefinedAnswerService.getAnswersByCategory(CATEGORY);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedResponse.getId(), result.get(0).getId());
        verify(answerRepository).findByCategoryIgnoreCase(CATEGORY);
    }

    @Test
    void getAnswersByCategory_EmptyCategory_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            predefinedAnswerService.getAnswersByCategory("  ");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            predefinedAnswerService.getAnswersByCategory(null);
        });
    }

    // --- Тесты для deleteByCompanyIdAndCategory ---
    @Test
    void deleteByCompanyIdAndCategory_ValidParams_ShouldCallRepository() {
        when(answerRepository.deleteByCompanyIdAndCategory(COMPANY_ID, CATEGORY)).thenReturn(5);
        int deletedCount = predefinedAnswerService.deleteByCompanyIdAndCategory(COMPANY_ID, CATEGORY);
        assertEquals(5, deletedCount);
        verify(answerRepository).deleteByCompanyIdAndCategory(COMPANY_ID, CATEGORY);
    }

    @Test
    void deleteByCompanyIdAndCategory_NullCompanyId_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            predefinedAnswerService.deleteByCompanyIdAndCategory(null, CATEGORY);
        });
    }

    @Test
    void deleteByCompanyIdAndCategory_EmptyCategory_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            predefinedAnswerService.deleteByCompanyIdAndCategory(COMPANY_ID, " ");
        });
    }

    // --- Тесты для getAllAnswers ---
    @Test
    void getAllAnswers_ShouldReturnAllMappedAnswers() {
        List<PredefinedAnswer> answerList = List.of(testAnswerEntity);
        when(answerRepository.findAll()).thenReturn(answerList);
        AnswerResponse expectedResponse = createExpectedResponse(testAnswerEntity);

        List<AnswerResponse> result = predefinedAnswerService.getAllAnswers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedResponse.getId(), result.get(0).getId());
        verify(answerRepository).findAll();
    }

    @Test
    void getAllAnswers_NoAnswers_ShouldReturnEmptyList() {
        when(answerRepository.findAll()).thenReturn(Collections.emptyList());
        List<AnswerResponse> result = predefinedAnswerService.getAllAnswers();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(answerRepository).findAll();
    }
}