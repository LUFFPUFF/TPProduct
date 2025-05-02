package com.example.ui.mapper.chat;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.dto.UserDto;
import com.example.ui.dto.user.UiUserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UiUserMapper {

    UiUserDto toUiDto(UserDto userDto);

    UiUserDto toUiDto(User user);

    UserDto toUserDto(UiUserDto uiUserDto);
}
