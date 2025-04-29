package com.example.domain.api.chat_service_api.integration.vk;

import lombok.Data;

@Data
public class VkResponse {

    private Long userId;
    private String firstName;
    private String lastName;
    private String birthDate;
    private String city;
    private String photoUrl;
}
