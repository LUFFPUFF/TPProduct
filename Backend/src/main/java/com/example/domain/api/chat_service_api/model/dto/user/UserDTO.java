package com.example.domain.api.chat_service_api.model.dto.user;

import com.example.database.model.company_subscription_module.user_roles.user.Gender;
import com.example.database.model.company_subscription_module.user_roles.user.UserStatus;
import com.example.domain.dto.company_module.CompanyDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Integer id;
    private CompanyDto company;
    private String fullName;
    private String email;
    private UserStatus status;
    private LocalDateTime dateOfBirth;
    private Gender gender;
    private String profilePicture;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
