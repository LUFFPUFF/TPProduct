package com.example.domain.api.crm_module.controller;

import com.example.domain.api.crm_module.dto.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface DealController {
    ResponseEntity<DealDto> createDeal(CreateDealDtoReq dealDto);
    ResponseEntity<DealDto> getDeal(Integer id);
    ResponseEntity<List<DealDto>> getDeals(FilterDealsDto filterDealsDto);
    ResponseEntity<DealDto> changeDealStage(ChangeDealStageReq changeDealStageReq);
    ResponseEntity<DealDto> changeDealData(ChangeDealReqDto changeDealReqDto);
    ResponseEntity<List<DealDto>> setDealsToArchive();
    ResponseEntity<List<DealArchiveDto>> getDealsArchive(FilterArchiveDto archiveDto);
    ResponseEntity<DealDto> getDealByChat(Integer chatId);
}
