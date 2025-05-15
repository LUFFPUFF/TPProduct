package com.example.domain.api.chat_service_api.integration.vk.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class VkMessage {

    private Long id;
    @JsonProperty("peer_id")
    private Long peerId;
    @JsonProperty("from_id")
    private Long fromId;
    private String text;
    private Integer date;
}
