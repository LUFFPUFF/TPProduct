package com.example.domain.dto.company_module;

import com.example.database.model.company_subscription_module.user_roles.user.Gender;
import com.example.database.model.company_subscription_module.user_roles.user.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDto {
    private Integer id;

    @NotNull(message = "Company ID не может быть пустым")
    private Integer companyId;

    @NotNull(message = "Имя не может быть пустым")
    @Size(max = 50)
    private String fullName;

    @NotNull(message = "Email не может быть пустым")
    @Email(message = "Некорректный email пользователя")
    @Size(max = 50)
    private String email;

    @NotNull(message = "Статус не может быть пустым")
    @Size(max = 50)
    private String status;

    private LocalDateTime dateOfBirth;

    @NotNull(message = "Гендер не может быть пустым")
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "Некорректный гендер")
    private String gender;

    private String profilePicture;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
