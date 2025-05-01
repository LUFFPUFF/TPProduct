package com.example.domain.api.chat_service_api.model.dto.user;

import com.example.database.model.company_subscription_module.user_roles.user.UserStatus;
import lombok.Data;

@Data
public class UserInfoDTO {
    private Integer id;
    private String fullName;
    private String profilePicture;
    private UserStatus status;
}
