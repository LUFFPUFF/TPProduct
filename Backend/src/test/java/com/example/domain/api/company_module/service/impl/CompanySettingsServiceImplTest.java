package com.example.domain.api.company_module.service.impl;

import com.example.database.repository.company_subscription_module.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanySettingsServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanySettingsServiceImpl companySettingsService;

    private final String TEST_NAME = "New Company Name";
    private final String TEST_EMAIL = "newowner@example.com";

    @BeforeEach
    void setUp() {
        reset(companyRepository);
    }

    @Test
    void changeName_ShouldReturnNull_AsPerCurrentImplementation() {
        // Act
        String result = companySettingsService.changeName(TEST_NAME);

        // Assert
        assertNull(result, "Expected changeName to return null as it's a stub");
        verifyNoInteractions(companyRepository);
    }

    @Test
    void changeOwner_ShouldReturnEmptyString_AsPerCurrentImplementation() {
        // Act
        String result = companySettingsService.changeOwner(TEST_EMAIL);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals("", result, "Expected changeOwner to return an empty string as it's a stub");
        verifyNoInteractions(companyRepository);
    }
}