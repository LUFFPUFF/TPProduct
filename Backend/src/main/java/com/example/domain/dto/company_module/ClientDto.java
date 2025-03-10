package com.example.domain.dto.company_module;

import com.example.database.model.crm_module.client.TypeClient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientDto {

    @NotNull(message = "User ID не может быть пустым")
    private Integer userId;

    @NotNull(message = "Имя не может быть пустым")
    @Size(max = 50)
    private String name;

    @Size(max = 50)
    private String typeClient;

    @Size(max = 50)
    private String tag;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
