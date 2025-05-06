package com.example.domain.api.chat_service_api.mapper;

import com.example.database.model.company_subscription_module.user_roles.user.Gender;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.company_subscription_module.user_roles.user.UserStatus;
import com.example.domain.api.chat_service_api.model.dto.user.UserInfoDTO;
import com.example.domain.dto.UserDto;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-06T03:20:43+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 21.0.7 (Microsoft)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDto toDto(User user) {
        if ( user == null ) {
            return null;
        }

        UserDto userDto = new UserDto();

        userDto.setId( user.getId() );
        userDto.setFullName( user.getFullName() );
        userDto.setEmail( user.getEmail() );
        if ( user.getStatus() != null ) {
            userDto.setStatus( user.getStatus().name() );
        }
        userDto.setDateOfBirth( user.getDateOfBirth() );
        if ( user.getGender() != null ) {
            userDto.setGender( user.getGender().name() );
        }
        userDto.setProfilePicture( user.getProfilePicture() );
        userDto.setCreatedAt( user.getCreatedAt() );
        userDto.setUpdatedAt( user.getUpdatedAt() );

        return userDto;
    }

    @Override
    public UserInfoDTO toInfoDTO(User user) {
        if ( user == null ) {
            return null;
        }

        UserInfoDTO userInfoDTO = new UserInfoDTO();

        userInfoDTO.setId( user.getId() );
        userInfoDTO.setFullName( user.getFullName() );
        userInfoDTO.setProfilePicture( user.getProfilePicture() );
        userInfoDTO.setStatus( user.getStatus() );

        return userInfoDTO;
    }

    @Override
    public User toEntity(UserDto userDTO) {
        if ( userDTO == null ) {
            return null;
        }

        User user = new User();

        user.setFullName( userDTO.getFullName() );
        user.setEmail( userDTO.getEmail() );
        if ( userDTO.getStatus() != null ) {
            user.setStatus( Enum.valueOf( UserStatus.class, userDTO.getStatus() ) );
        }
        user.setDateOfBirth( userDTO.getDateOfBirth() );
        if ( userDTO.getGender() != null ) {
            user.setGender( Enum.valueOf( Gender.class, userDTO.getGender() ) );
        }
        user.setProfilePicture( userDTO.getProfilePicture() );

        return user;
    }

    @Override
    public User fromInfoDTO(UserInfoDTO userInfoDTO) {
        if ( userInfoDTO == null ) {
            return null;
        }

        User user = new User();

        user.setId( userInfoDTO.getId() );
        user.setFullName( userInfoDTO.getFullName() );
        user.setStatus( userInfoDTO.getStatus() );
        user.setProfilePicture( userInfoDTO.getProfilePicture() );

        return user;
    }
}
