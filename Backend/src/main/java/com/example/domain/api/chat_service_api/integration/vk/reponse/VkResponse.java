package com.example.domain.api.chat_service_api.integration.vk.reponse;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class VkResponse {

    private Integer companyId;
    private Long communityId;
    private Long peerId;
    private Long fromId;
    private String text;
    private Instant timestamp;
}
