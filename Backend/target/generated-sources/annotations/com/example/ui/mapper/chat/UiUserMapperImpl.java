package com.example.ui.mapper.chat;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.dto.UserDto;
import com.example.ui.dto.user.UiUserDto;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-22T17:00:26+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class UiUserMapperImpl implements UiUserMapper {

    @Override
    public UiUserDto toUiDto(UserDto userDto) {
        if ( userDto == null ) {
            return null;
        }

        UiUserDto.UiUserDtoBuilder uiUserDto = UiUserDto.builder();

        uiUserDto.id( userDto.getId() );
        uiUserDto.fullName( userDto.getFullName() );
        uiUserDto.status( userDto.getStatus() );
        uiUserDto.gender( userDto.getGender() );
        uiUserDto.profilePicture( userDto.getProfilePicture() );

        return uiUserDto.build();
    }

    @Override
    public UiUserDto toUiDto(User user) {
        if ( user == null ) {
            return null;
        }

        UiUserDto.UiUserDtoBuilder uiUserDto = UiUserDto.builder();

        uiUserDto.id( user.getId() );
        uiUserDto.fullName( user.getFullName() );
        if ( user.getStatus() != null ) {
            uiUserDto.status( user.getStatus().name() );
        }
        if ( user.getGender() != null ) {
            uiUserDto.gender( user.getGender().name() );
        }
        uiUserDto.profilePicture( user.getProfilePicture() );

        return uiUserDto.build();
    }

    @Override
    public UserDto toUserDto(UiUserDto uiUserDto) {
        if ( uiUserDto == null ) {
            return null;
        }

        UserDto userDto = new UserDto();

        userDto.setId( uiUserDto.getId() );
        userDto.setFullName( uiUserDto.getFullName() );
        userDto.setStatus( uiUserDto.getStatus() );
        userDto.setGender( uiUserDto.getGender() );
        userDto.setProfilePicture( uiUserDto.getProfilePicture() );

        return userDto;
    }
}
