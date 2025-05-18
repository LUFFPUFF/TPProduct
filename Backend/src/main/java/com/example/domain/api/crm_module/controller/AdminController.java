package com.example.domain.api.crm_module.controller;

import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.crm_module.dto.DealDto;
import com.example.domain.dto.EmailDto;
import com.example.ui.dto.chat.ChatUIDetailsDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface AdminController {
    ResponseEntity<DealDto> changeDealPerformer(EmailDto performerEmail);
    ResponseEntity<List<DealDto>> setDealsToArchive(EmailDto email);
}
