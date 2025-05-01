package com.example.domain.api.chat_service_api.model.dto.client;

import com.example.database.model.crm_module.client.TypeClient;
import com.example.domain.api.chat_service_api.model.dto.user.UserInfoDTO;
import com.example.domain.dto.CompanyDto;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientDTO {
    private Integer id;
    private UserInfoDTO user;
    private CompanyDto company;
    private String name;
    private TypeClient typeClient;
    private String tag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
