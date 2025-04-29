package com.example.domain.dto.mapper;

import com.example.database.model.chats_messages_module.ChatAttachment;
import com.example.database.model.chats_messages_module.ChatMessage;
import com.example.database.model.chats_messages_module.chat.Chat;
import com.example.database.model.company_subscription_module.company.Company;
import com.example.database.model.company_subscription_module.user_roles.UserRole;
import com.example.database.model.company_subscription_module.user_roles.role.Role;
import com.example.database.model.company_subscription_module.user_roles.role.RoleName;
import com.example.database.model.company_subscription_module.user_roles.user.Gender;
import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.model.company_subscription_module.user_roles.user.UserStatus;
import com.example.database.model.crm_module.client.Client;
import com.example.database.model.crm_module.client.TypeClient;
import com.example.domain.dto.chat_module.ChatAttachmentDto;
import com.example.domain.dto.chat_module.ChatDto;
import com.example.domain.dto.chat_module.MessageDto;
import com.example.domain.dto.company_module.ClientDto;
import com.example.domain.dto.company_module.CompanyDto;
import com.example.domain.dto.company_module.RoleDto;
import com.example.domain.dto.company_module.UserDto;
import com.example.domain.dto.company_module.UserRoleDto;
import java.util.Date;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-04-29T20:56:25+0300",
    comments = "version: 1.6.0.Beta1, compiler: javac, environment: Java 22.0.2 (Amazon.com Inc.)"
)
@Component
public class MapperDtoImpl implements MapperDto {

    @Override
    public Chat toEntityChat(ChatDto chatDto) {
        if ( chatDto == null ) {
            return null;
        }

        Chat chat = new Chat();

        chat.setUser( toEntityUser( chatDto.getUserDto() ) );
        chat.setClient( toEntityClient( chatDto.getClientDto() ) );
        chat.setId( chatDto.getId() );
        chat.setChatChannel( chatDto.getChatChannel() );
        chat.setStatus( chatDto.getStatus() );
        chat.setCreatedAt( chatDto.getCreatedAt() );

        return chat;
    }

    @Override
    public ChatDto toDtoChat(Chat chat) {
        if ( chat == null ) {
            return null;
        }

        ChatDto chatDto = new ChatDto();

        chatDto.setUserDto( toDtoUser( chat.getUser() ) );
        chatDto.setClientDto( toDtoClient( chat.getClient() ) );
        chatDto.setId( chat.getId() );
        chatDto.setChatChannel( chat.getChatChannel() );
        chatDto.setStatus( chat.getStatus() );
        chatDto.setCreatedAt( chat.getCreatedAt() );

        return chatDto;
    }

    @Override
    public ChatMessage toEntityChatMessage(MessageDto messageDto) {
        if ( messageDto == null ) {
            return null;
        }

        ChatMessage chatMessage = new ChatMessage();

        chatMessage.setChat( toEntityChat( messageDto.getChatDto() ) );
        chatMessage.setContent( messageDto.getContent() );
        chatMessage.setSentAt( messageDto.getSentAt() );

        return chatMessage;
    }

    @Override
    public MessageDto toDtoChatMessage(ChatMessage chatMessage) {
        if ( chatMessage == null ) {
            return null;
        }

        MessageDto messageDto = new MessageDto();

        messageDto.setChatDto( toDtoChat( chatMessage.getChat() ) );
        messageDto.setContent( chatMessage.getContent() );
        messageDto.setSentAt( chatMessage.getSentAt() );

        return messageDto;
    }

    @Override
    public ChatAttachment toEntityChatAttachment(ChatAttachmentDto chatAttachmentDto) {
        if ( chatAttachmentDto == null ) {
            return null;
        }

        ChatAttachment chatAttachment = new ChatAttachment();

        chatAttachment.setFileUrl( chatAttachmentDto.getFileUrl() );
        chatAttachment.setFileType( chatAttachmentDto.getFileType() );

        return chatAttachment;
    }

    @Override
    public ChatAttachmentDto toDtoChatAttachment(ChatAttachment chatAttachment) {
        if ( chatAttachment == null ) {
            return null;
        }

        ChatAttachmentDto chatAttachmentDto = new ChatAttachmentDto();

        chatAttachmentDto.setFileUrl( chatAttachment.getFileUrl() );
        chatAttachmentDto.setFileType( chatAttachment.getFileType() );

        return chatAttachmentDto;
    }

    @Override
    public Client toEntityClient(ClientDto clientDto) {
        if ( clientDto == null ) {
            return null;
        }

        Client client = new Client();

        client.setUser( toEntityUser( clientDto.getUserDto() ) );
        client.setId( clientDto.getId() );
        client.setName( clientDto.getName() );
        if ( clientDto.getTypeClient() != null ) {
            client.setTypeClient( Enum.valueOf( TypeClient.class, clientDto.getTypeClient() ) );
        }
        client.setTag( clientDto.getTag() );
        client.setCreatedAt( clientDto.getCreatedAt() );
        client.setUpdatedAt( clientDto.getUpdatedAt() );

        return client;
    }

    @Override
    public ClientDto toDtoClient(Client client) {
        if ( client == null ) {
            return null;
        }

        ClientDto clientDto = new ClientDto();

        clientDto.setUserDto( toDtoUser( client.getUser() ) );
        clientDto.setId( client.getId() );
        clientDto.setName( client.getName() );
        if ( client.getTypeClient() != null ) {
            clientDto.setTypeClient( client.getTypeClient().name() );
        }
        clientDto.setTag( client.getTag() );
        clientDto.setCreatedAt( client.getCreatedAt() );
        clientDto.setUpdatedAt( client.getUpdatedAt() );

        return clientDto;
    }

    @Override
    public Company toEntityCompany(CompanyDto companyDto) {
        if ( companyDto == null ) {
            return null;
        }

        Company company = new Company();

        company.setId( companyDto.getId() );
        company.setName( companyDto.getName() );
        company.setContactEmail( companyDto.getContactEmail() );
        company.setCreatedAt( companyDto.getCreatedAt() );
        company.setUpdatedAt( companyDto.getUpdatedAt() );

        return company;
    }

    @Override
    public CompanyDto toDtoCompany(Company company) {
        if ( company == null ) {
            return null;
        }

        CompanyDto.CompanyDtoBuilder companyDto = CompanyDto.builder();

        companyDto.id( company.getId() );
        companyDto.name( company.getName() );
        companyDto.contactEmail( company.getContactEmail() );
        companyDto.createdAt( company.getCreatedAt() );
        companyDto.updatedAt( company.getUpdatedAt() );

        return companyDto.build();
    }

    @Override
    public User toEntityUser(UserDto userDto) {
        if ( userDto == null ) {
            return null;
        }

        User user = new User();

        user.setCompany( toEntityCompany( userDto.getCompanyDto() ) );
        user.setId( userDto.getId() );
        user.setFullName( userDto.getFullName() );
        user.setEmail( userDto.getEmail() );
        if ( userDto.getStatus() != null ) {
            user.setStatus( Enum.valueOf( UserStatus.class, userDto.getStatus() ) );
        }
        user.setDateOfBirth( userDto.getDateOfBirth() );
        if ( userDto.getGender() != null ) {
            user.setGender( Enum.valueOf( Gender.class, userDto.getGender() ) );
        }
        user.setProfilePicture( userDto.getProfilePicture() );
        user.setCreatedAt( userDto.getCreatedAt() );
        user.setUpdatedAt( userDto.getUpdatedAt() );

        return user;
    }

    @Override
    public UserDto toDtoUser(User user) {
        if ( user == null ) {
            return null;
        }

        UserDto userDto = new UserDto();

        userDto.setCompanyDto( toDtoCompany( user.getCompany() ) );
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
    public Role toEntityRole(RoleDto roleDto) {
        if ( roleDto == null ) {
            return null;
        }

        Role role = new Role();

        role.setId( roleDto.getId() );
        if ( roleDto.getName() != null ) {
            role.setName( Enum.valueOf( RoleName.class, roleDto.getName() ) );
        }
        role.setDescription( roleDto.getDescription() );

        return role;
    }

    @Override
    public RoleDto toDtoRole(Role role) {
        if ( role == null ) {
            return null;
        }

        RoleDto roleDto = new RoleDto();

        roleDto.setId( role.getId() );
        if ( role.getName() != null ) {
            roleDto.setName( role.getName().name() );
        }
        roleDto.setDescription( role.getDescription() );

        return roleDto;
    }

    @Override
    public UserRole toEntityUserRole(UserRoleDto userRoleDto) {
        if ( userRoleDto == null ) {
            return null;
        }

        UserRole userRole = new UserRole();

        return userRole;
    }

    @Override
    public UserRoleDto toDtoUserRole(UserRole userRole) {
        if ( userRole == null ) {
            return null;
        }

        UserRoleDto userRoleDto = new UserRoleDto();

        return userRoleDto;
    }
}
