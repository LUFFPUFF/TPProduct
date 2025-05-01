package com.example.domain.api.chat_service_api.model.dto.client;

import com.example.database.model.crm_module.client.TypeClient;
import lombok.Data;

@Data
public class ClientInfoDTO {
    private Integer id;
    private String name;
    private String tag;
    private TypeClient typeClient;
}
