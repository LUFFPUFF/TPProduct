package com.example.domain.api.crm_module.controller.impl;

import com.example.domain.api.chat_service_api.model.dto.ChatDetailsDTO;
import com.example.domain.api.crm_module.controller.AdminController;
import com.example.domain.api.crm_module.dto.DealDto;
import com.example.domain.api.crm_module.service.DealService;
import com.example.domain.dto.EmailDto;
import com.example.ui.dto.chat.ChatUIDetailsDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/crm/admin")
@RequiredArgsConstructor
public class AdminCrmController implements AdminController {
    private final DealService dealService;
    @Override
    @PostMapping("/change-performer")
    public ResponseEntity<DealDto> changeDealPerformer(@Valid EmailDto performerEmail) {
        return ResponseEntity.ok(dealService.changeDealPerformer(performerEmail.getEmail()));
    }

    @Override
    @PostMapping("/put-in-archive")
    public ResponseEntity<List<DealDto>> setDealsToArchive(@Valid EmailDto emailDto) {
        return ResponseEntity.ok(dealService.setAdminDealsToArchive(emailDto.getEmail()));
    }


}
