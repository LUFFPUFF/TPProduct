package com.example.domain.api.subscription_module.service.impl;

import com.example.domain.api.subscription_module.config.SubscriptionConfig;
import com.example.domain.dto.SubscriptionPriceReqDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*; // Импортируем все Assertions
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionPriceCalculateServiceImplTest {

    @Mock
    private SubscriptionConfig subscriptionConfig;

    @InjectMocks
    private SubscriptionPriceCalculateServiceImpl priceCalculateService;

    private final BigDecimal BASE_PRICE = new BigDecimal("1000.00");
    // Константа для максимальной суммарной скидки
    private final BigDecimal MAX_TOTAL_DISCOUNT = new BigDecimal("0.50");

    @BeforeEach
    void setUp() {
        when(subscriptionConfig.getPrice()).thenReturn(BASE_PRICE);
    }

    // --- Тесты для calculateDiscountMonths ---

    @ParameterizedTest(name = "Months: {0}, Expected Discount: {1}")
    @CsvSource({
            "1,  0.00",  // <= 1
            "0,  0.00",  // < 1 (граничный случай)
            "-5, 0.00", // < 1 (граничный случай)
            "6,  0.125", // (6/12)*0.25 = 0.125
            "12, 0.25",  // (12/12)*0.25 = 0.25 (Максимум)
            "13, 0.25",  // Свыше максимума
            "24, 0.25"   // Свыше максимума
    })
    void calculateDiscountMonths_VariousMonths_ShouldReturnCorrectDiscount(int months, String expectedDiscountStr) {
        BigDecimal expectedDiscount = new BigDecimal(expectedDiscountStr);
        BigDecimal result = priceCalculateService.calculateDiscountMonths(months);
        // Используем 3 знака после запятой для точности 0.125
        assertEquals(0, result.setScale(3, RoundingMode.HALF_UP).compareTo(expectedDiscount.setScale(3, RoundingMode.HALF_UP)),
                "Discount for " + months + " months should be " + expectedDiscount);
    }

    // --- Тесты для calculateDiscountPeople ---

    @ParameterizedTest(name = "Operators: {0}, Expected Discount: {1}")
    @CsvSource({
            "1,  0.00",  // <= 1
            "0,  0.00",  // < 1 (граничный случай)
            "-5, 0.00", // < 1 (граничный случай)
            "5,  0.125", // (5/10)*0.25 = 0.125
            "10, 0.25",  // (10/10)*0.25 = 0.25 (Максимум)
            "11, 0.25",  // Свыше максимума
            "20, 0.25"   // Свыше максимума
    })
    void calculateDiscountPeople_VariousOperators_ShouldReturnCorrectDiscount(int operators, String expectedDiscountStr) {
        BigDecimal expectedDiscount = new BigDecimal(expectedDiscountStr);
        BigDecimal result = priceCalculateService.calculateDiscountPeople(operators);
        // Используем 3 знака после запятой
        assertEquals(0, result.setScale(3, RoundingMode.HALF_UP).compareTo(expectedDiscount.setScale(3, RoundingMode.HALF_UP)),
                "Discount for " + operators + " operators should be " + expectedDiscount);
    }

    // --- Тесты для calculateTotalPrice ---

    // Пересчитываем ожидаемые цены с учетом формулы totalDiscount = min(0.5, monthsDiscount + peopleDiscount)
    // Base Price = 1000
    @ParameterizedTest(name = "{index}: {0} months, {1} operators => Expected Price = {2}")
    @CsvSource({
            // months, ops, expectedPrice (Пересчитано!)
            "1,  1,   1000.00",  // Скидки: 0 + 0 = 0. Total = 0. Price = 1000*1*1*(1-0) = 1000.00
            "6,  4,   18600.00", // Скидки: 0.125 + 0.1 = 0.225. Total = 0.225. Price = 24000 * 0.775 = 18600.00
            "12, 1,   9000.00",  // Скидки: 0.25 + 0 = 0.25. Total = 0.25. Price = 12000 * 0.75 = 9000.00
            "1, 10,   7500.00",  // Скидки: 0 + 0.25 = 0.25. Total = 0.25. Price = 10000 * 0.75 = 7500.00
            // ИСПРАВЛЕНО: Ожидаемая цена для 12, 10
            "12, 10,  60000.00", // Скидки: 0.25 + 0.25 = 0.50. Total = 0.50. Price = 120000 * 0.50 = 60000.00
            // ИСПРАВЛЕНО: Ожидаемая цена для 6, 20
            "6,  20,  75000.00", // Скидки: 0.125 + 0.25 = 0.375. Total = 0.375. Price = 120000 * 0.625 = 75000.00
            "24, 20, 240000.00"  // Скидки: 0.25 + 0.25 = 0.50. Total = 0.50. Price = 480000 * 0.50 = 240000.00
    })
    void calculateTotalPrice_VariousInputs_ShouldApplyCorrectDiscount(int months, int operators, String expectedPriceStr) {
        // Arrange
        SubscriptionPriceReqDto request = SubscriptionPriceReqDto.builder()
                .months_count(months)
                .operators_count(operators)
                .build();
        // Используем BigDecimal для ожидаемого значения
        BigDecimal expectedPrice = new BigDecimal(expectedPriceStr).setScale(2, RoundingMode.HALF_UP); // Округляем ожидаемое значение для консистентности

        // Act
        BigDecimal result = priceCalculateService.calculateTotalPrice(request);

        // Assert
        // ИСПРАВЛЕНО: Убираем setScale из 'result', сравниваем результат "как есть" с округленным ожидаемым
        // Так как финальный результат сервиса не округляется, но ожидаемое значение удобнее задавать с 2 знаками
        assertNotNull(result, "Result should not be null");
        assertEquals(0, result.compareTo(expectedPrice),
                "Total price for " + months + " months and " + operators + " operators.\nExpected: " + expectedPrice + "\nActual:   " + result);
    }

    @Test
    void calculateTotalPrice_InvalidInput_ShouldHandleAsPerImplementation() {
        SubscriptionPriceReqDto requestNegativeMonths = SubscriptionPriceReqDto.builder().months_count(-1).operators_count(5).build();
        SubscriptionPriceReqDto requestZeroOperators = SubscriptionPriceReqDto.builder().months_count(6).operators_count(0).build();
        SubscriptionPriceReqDto requestNullMonths = SubscriptionPriceReqDto.builder().months_count(null).operators_count(5).build();
        SubscriptionPriceReqDto requestNullOperators = SubscriptionPriceReqDto.builder().months_count(5).operators_count(null).build();

        assertThrows(NullPointerException.class, () -> {
            priceCalculateService.calculateTotalPrice(requestNullMonths);
        }, "Should throw NPE for null months");
        assertThrows(NullPointerException.class, () -> {
            priceCalculateService.calculateTotalPrice(requestNullOperators);
        }, "Should throw NPE for null operators");

        BigDecimal resultNegativeMonths = priceCalculateService.calculateTotalPrice(requestNegativeMonths);
        assertEquals(0, resultNegativeMonths.compareTo(new BigDecimal("-4375.00")), "Price for negative months should be calculated");

        BigDecimal resultZeroOperators = priceCalculateService.calculateTotalPrice(requestZeroOperators);
        assertEquals(0, resultZeroOperators.compareTo(BigDecimal.ZERO), "Price for zero operators should be zero");
    }
}