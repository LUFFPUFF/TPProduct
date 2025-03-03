package com.example.domain.dto;

import com.example.database.model.company_subscription_module.user_roles.user.Gender;
import com.example.database.model.company_subscription_module.user_roles.user.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDto {

    @NotNull(message = "User ID не может быть пустым")
    private Integer id;

    @NotNull(message = "Company ID не может быть пустым")
    private CompanyDto companyDto;

    @NotNull(message = "Имя не может быть пустым")
    @Size(max = 50)
    private String fullName;

    @NotNull(message = "Email не может быть пустым")
    @Size(max = 50)
    private String email;

    @NotNull(message = "Статус не может быть пустым")
    private UserStatus status;


    private LocalDateTime dateOfBirth;

    @NotNull(message = "Гендер не может быть пустым")
    private Gender gender;


    private String profilePicture;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
