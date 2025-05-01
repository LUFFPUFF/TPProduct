package com.example.domain.api.chat_service_api.mapper;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.api.chat_service_api.model.dto.user.UserDTO;
import com.example.domain.api.chat_service_api.model.dto.user.UserInfoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(User user);

    UserInfoDTO toInfoDTO(User user);

     @Mapping(target = "id", ignore = true)
     @Mapping(target = "createdAt", ignore = true)
     @Mapping(target = "updatedAt", ignore = true)
     User toEntity(UserDTO userDTO);

     User fromInfoDTO(UserInfoDTO userInfoDTO);
}
