package com.example.domain.api.authentication_module.dto;

import com.example.database.model.company_subscription_module.user_roles.user.Gender;
import lombok.Builder;
import lombok.Data;


import java.time.LocalDateTime;

@Data
@Builder
public class UserDataDto {
    private String name;
    private LocalDateTime birthday;
    private Gender gender;
}
