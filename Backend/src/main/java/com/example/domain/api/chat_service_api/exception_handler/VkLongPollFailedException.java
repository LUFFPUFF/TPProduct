package com.example.domain.api.chat_service_api.exception_handler;

import lombok.Getter;

@Getter
public class VkLongPollFailedException extends VkApiException {
    private final Integer failedCode;
    private final Integer newTs;

    public VkLongPollFailedException(Integer failedCode, Integer newTs) {
        super("VK Long Poll failed with code: " + failedCode);
        this.failedCode = failedCode;
        this.newTs = newTs;
    }

}
