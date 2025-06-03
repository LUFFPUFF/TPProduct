package com.example.domain.api.crm_module.controller.impl;

import com.example.domain.api.crm_module.controller.DealController;
import com.example.domain.api.crm_module.dto.*;
import com.example.domain.api.crm_module.service.DealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crm")
@RequiredArgsConstructor
public class CrmController implements DealController {
    private final DealService dealService;
    @PostMapping("/create")
    public ResponseEntity<DealDto> createDeal(@Validated @RequestBody CreateDealDtoReq dealDto) {
        return ResponseEntity.ok(dealService.createDeal(dealDto));
    }

    @Override
    @GetMapping("/get/{id}")
    public ResponseEntity<DealDto> getDeal(@PathVariable("id") Integer id ) {
        return ResponseEntity.ok(dealService.getDeal(id));
    }
    @Override
    @GetMapping("/get")
    public ResponseEntity<List<DealDto>> getDeals(@ModelAttribute FilterDealsDto filterDealsDto) {
        return ResponseEntity.ok(dealService.getDeals(filterDealsDto,false));
    }

    @Override
    @PostMapping("/change-stage")
    public ResponseEntity<DealDto> changeDealStage(@Validated @RequestBody ChangeDealStageReq changeDealStageReq) {
        return ResponseEntity.ok(dealService.changeDealStage(changeDealStageReq));
    }

    @Override
    public ResponseEntity<DealDto> changeDealData(ChangeDealReqDto changeDealReqDto) {
        return ResponseEntity.ok(dealService.changeDealData(changeDealReqDto));
    }

    @Override
    @PostMapping("/put-in-archive")
    public ResponseEntity<List<DealDto>> setDealsToArchive() {
        return ResponseEntity.ok(dealService.setDealsToArchive());
    }

    @Override
    @GetMapping("/get-archive")
    public ResponseEntity<List<DealArchiveDto>> getDealsArchive(@ModelAttribute FilterArchiveDto archiveDto) {
        return ResponseEntity.ok(dealService.getDealsArchive(archiveDto));
    }



    @Override
    @GetMapping("/get-by-chat/{chatId}")
    public ResponseEntity<DealDto> getDealByChat(@PathVariable("chatId") Integer chatId) {
        return ResponseEntity.ok(dealService.getDealByChat(chatId));
    }
}
