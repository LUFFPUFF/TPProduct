package com.example.domain.api.company_module.service.impl;

import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.CompanyRepository;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.company_module.exception_handler_company.NotFoundCompanyException;
// import com.example.domain.api.company_module.service.CompanyService; // Не нужен в тесте реализации
import com.example.domain.dto.CompanyDto;
import com.example.domain.dto.CompanyWithMembersDto;
import com.example.domain.dto.MemberDto; // Предполагаемый импорт
import com.example.domain.dto.mapper.MapperDto;
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
import org.springframework.dao.DataAccessException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyServiceImplTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private MapperDto mapperDto;
    @Mock private UserRepository userRepository;
    // CompanyMembersService не инжектируется напрямую в CompanyServiceImpl

    @InjectMocks
    private CompanyServiceImpl companyService;

    @Captor private ArgumentCaptor<Company> companyCaptor;
    @Captor private ArgumentCaptor<CompanyDto> companyDtoCaptor;

    private Company testCompany;
    private CompanyDto testCompanyDto;
    private User testUser;
    private MemberDto testMemberDto;
    private CompanyWithMembersDto testCompanyWithMembersDto;

    private final Integer COMPANY_ID = 1;
    private final String COMPANY_NAME = "Test Inc.";
    private final String COMPANY_EMAIL = "contact@testinc.com";
    private final String USER_EMAIL = "user@testinc.com";
    private final Integer USER_ID = 10;


    @BeforeEach
    void setUp() {
        reset(companyRepository, mapperDto, userRepository);

        testCompany = new Company();
        testCompany.setId(COMPANY_ID);
        testCompany.setName(COMPANY_NAME);
        testCompany.setContactEmail(COMPANY_EMAIL);
        testCompany.setCreatedAt(LocalDateTime.now().minusDays(1));
        testCompany.setUpdatedAt(LocalDateTime.now().minusHours(1));

        testCompanyDto = CompanyDto.builder()
                .id(COMPANY_ID)
                .name(COMPANY_NAME)
                .contactEmail(COMPANY_EMAIL)
                .createdAt(testCompany.getCreatedAt())
                .updatedAt(testCompany.getUpdatedAt())
                .build();

        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setEmail(USER_EMAIL);
        testUser.setFullName("Test User FullName");
        testUser.setCompany(testCompany);

        testMemberDto = MemberDto.builder()
                .email(USER_EMAIL)
                .fullName("Test User FullName")
                .build();

        testCompanyWithMembersDto = CompanyWithMembersDto.builder()
                .company(testCompanyDto)
                .members(List.of(testMemberDto))
                .build();
    }

    // --- Тесты для createCompany ---
    @Test
    void createCompany_ValidDto_ShouldSaveAndReturnCompanyEntity() {
        // Arrange
        CompanyDto inputDto = CompanyDto.builder().name("New Corp").contactEmail("new@corp.com").build();
        Company mappedEntity = new Company(); // Сущность, возвращаемая маппером
        mappedEntity.setName(inputDto.getName());
        mappedEntity.setContactEmail(inputDto.getContactEmail());
        // createdAt и updatedAt устанавливаются в сервисе

        when(mapperDto.toEntityCompany(inputDto)).thenReturn(mappedEntity);
        // Мокируем save так, чтобы он вернул переданную сущность (с установленными датами)
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company companyToSave = invocation.getArgument(0);
            // Имитируем, что ID присваивается БД
            if (companyToSave.getId() == null) companyToSave.setId(COMPANY_ID + 1);
            // Даты уже должны быть установлены сервисом
            assertNotNull(companyToSave.getCreatedAt());
            assertNotNull(companyToSave.getUpdatedAt());
            return companyToSave;
        });

        // Act
        Company result = companyService.createCompany(inputDto);

        // Assert
        assertNotNull(result);
        assertEquals(inputDto.getName(), result.getName());
        assertEquals(inputDto.getContactEmail(), result.getContactEmail());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        assertEquals(result.getCreatedAt(), result.getUpdatedAt()); // Проверяем, что они равны при создании

        verify(mapperDto).toEntityCompany(inputDto);
        verify(companyRepository).save(companyCaptor.capture());
        Company savedCompany = companyCaptor.getValue();
        assertEquals(mappedEntity.getName(), savedCompany.getName());
        assertNotNull(savedCompany.getCreatedAt()); // Проверяем, что сервис установил
        assertNotNull(savedCompany.getUpdatedAt());// Проверяем, что сервис установил
    }

    @Test
    void createCompany_MapperFails_ShouldThrowException() {
        CompanyDto inputDto = CompanyDto.builder().name("Fail Corp").build();
        RuntimeException mapperException = new RuntimeException("Mapping to entity failed");
        when(mapperDto.toEntityCompany(inputDto)).thenThrow(mapperException);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            companyService.createCompany(inputDto);
        });
        assertEquals(mapperException, thrown);
        verifyNoInteractions(companyRepository);
    }

    @Test
    void createCompany_RepositorySaveFails_ShouldThrowException() {
        CompanyDto inputDto = CompanyDto.builder().name("Fail Corp").build();
        Company mappedEntity = new Company();
        when(mapperDto.toEntityCompany(inputDto)).thenReturn(mappedEntity);
        DataAccessException dbException = new DataAccessException("Save failed") {};
        when(companyRepository.save(any(Company.class))).thenThrow(dbException);

        DataAccessException thrown = assertThrows(DataAccessException.class, () -> {
            companyService.createCompany(inputDto);
        });
        assertEquals(dbException, thrown);
        verify(companyRepository).save(any(Company.class));
    }

    // --- Тесты для findCompany (по email) ---
    @Test
    void findCompany_UserAndCompanyExist_ShouldReturnCompanyWithMembersDto() {
        // Arrange
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser));
        when(mapperDto.toDtoCompany(testCompany)).thenReturn(testCompanyDto);
        when(userRepository.getAllByCompanyId(COMPANY_ID)).thenReturn(List.of(testUser)); // Для findMembers

        // Act
        CompanyWithMembersDto result = companyService.findCompany(USER_EMAIL);

        // Assert
        assertNotNull(result);
        assertEquals(testCompanyDto, result.getCompany());
        assertNotNull(result.getMembers());
        assertEquals(1, result.getMembers().size());
        assertEquals(testMemberDto.getEmail(), result.getMembers().get(0).getEmail());

        verify(userRepository).findByEmail(USER_EMAIL);
        verify(mapperDto).toDtoCompany(testCompany);
        verify(userRepository).getAllByCompanyId(COMPANY_ID);
    }

    @Test
    void findCompany_UserNotFound_ShouldThrowNotFoundCompanyException() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
        assertThrows(NotFoundCompanyException.class, () -> {
            companyService.findCompany(USER_EMAIL);
        });
        verifyNoInteractions(mapperDto);
    }

    @Test
    void findCompany_UserFoundButNoCompany_ShouldThrowNotFoundCompanyException() {
        testUser.setCompany(null); // Пользователь без компании
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser));
        assertThrows(NotFoundCompanyException.class, () -> {
            companyService.findCompany(USER_EMAIL);
        });
        verifyNoInteractions(mapperDto);
    }

    @Test
    void findCompany_NoMembersFound_ShouldReturnDtoWithEmptyMembersList() {
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser));
        when(mapperDto.toDtoCompany(testCompany)).thenReturn(testCompanyDto);
        when(userRepository.getAllByCompanyId(COMPANY_ID)).thenReturn(Collections.emptyList()); // Нет участников

        CompanyWithMembersDto result = companyService.findCompany(USER_EMAIL);

        assertNotNull(result);
        assertEquals(testCompanyDto, result.getCompany());
        assertNotNull(result.getMembers());
        assertTrue(result.getMembers().isEmpty());
    }

    // --- Тесты для disbandCompany ---
    @Test
    void disbandCompany_ShouldDoNothing() {
        // Метод пуст, просто проверяем, что не падает и ничего не вызывает
        assertDoesNotThrow(() -> companyService.disbandCompany(USER_EMAIL));
        verifyNoInteractions(companyRepository, userRepository, mapperDto); // Если бы была логика, тут были бы verify
    }


    // --- Тесты для findCompanyWithId ---
    @Test
    void findCompanyWithId_CompanyExists_ShouldReturnCompanyWithMembersDto() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(testCompany));
        when(mapperDto.toDtoCompany(testCompany)).thenReturn(testCompanyDto);
        when(userRepository.getAllByCompanyId(COMPANY_ID)).thenReturn(List.of(testUser));

        CompanyWithMembersDto result = companyService.findCompanyWithId(COMPANY_ID);

        assertNotNull(result);
        assertEquals(testCompanyDto, result.getCompany());
        assertEquals(1, result.getMembers().size());
        assertEquals(testMemberDto.getEmail(), result.getMembers().get(0).getEmail());

        verify(companyRepository).findById(COMPANY_ID);
        verify(mapperDto).toDtoCompany(testCompany);
        verify(userRepository).getAllByCompanyId(COMPANY_ID);
    }

    @Test
    void findCompanyWithId_CompanyNotFound_ShouldThrowNotFoundCompanyException() {
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundCompanyException.class, () -> {
            companyService.findCompanyWithId(COMPANY_ID);
        });
        verifyNoInteractions(mapperDto, userRepository);
    }

    // --- Тесты для findMembers (можно сделать public в тесте или тестировать косвенно) ---
    @Test
    void findMembers_ShouldMapUsersToMemberDtos() {
        // Arrange
        User user2 = new User();
        user2.setId(USER_ID + 1);
        user2.setEmail("user2@testinc.com");
        user2.setFullName("User Two FullName");

        when(userRepository.getAllByCompanyId(COMPANY_ID)).thenReturn(List.of(testUser, user2));

        // Act
        List<MemberDto> members = companyService.findMembers(testCompany); // Тестируем публичный findMembers

        // Assert
        assertNotNull(members);
        assertEquals(2, members.size());
        assertTrue(members.stream().anyMatch(m -> m.getEmail().equals(USER_EMAIL) && m.getFullName().equals("Test User FullName")));
        assertTrue(members.stream().anyMatch(m -> m.getEmail().equals("user2@testinc.com") && m.getFullName().equals("User Two FullName")));
        verify(userRepository).getAllByCompanyId(COMPANY_ID);
    }

    @Test
    void findMembers_NoUsersInCompany_ShouldReturnEmptyList() {
        when(userRepository.getAllByCompanyId(COMPANY_ID)).thenReturn(Collections.emptyList());
        List<MemberDto> members = companyService.findMembers(testCompany);
        assertNotNull(members);
        assertTrue(members.isEmpty());
    }
}