package com.example.domain.api.authentication_module.mapper;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.api.authentication_module.dto.UserDataDto;
import org.springframework.stereotype.Component;

@Component
public class UserToUserDataMapper {
    public UserDataDto map(User user){
        return UserDataDto.builder()
                .birthday(user.getDateOfBirth())
                .gender(user.getGender())
                .name(user.getFullName())
                .build();
    }
}
