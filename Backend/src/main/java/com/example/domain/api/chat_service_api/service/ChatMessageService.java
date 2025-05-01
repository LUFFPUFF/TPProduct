package com.example.domain.api.chat_service_api.service;

import com.example.domain.api.chat_service_api.model.dto.MessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    public void createMessage(MessageDto messageDto) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public MessageDto getMessageById(Integer id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<MessageDto> getAllMessages() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
