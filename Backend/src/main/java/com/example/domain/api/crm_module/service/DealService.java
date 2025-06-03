package com.example.domain.api.crm_module.service;


import com.example.domain.api.crm_module.dto.*;
import com.example.ui.dto.chat.ChatUIDetailsDTO;
import org.springframework.http.ResponseEntity;


import java.util.List;

public interface DealService {
    DealDto createDeal(CreateDealDtoReq dealDto);
    DealDto getDeal(Integer id);
    List<DealDto> getDeals(FilterDealsDto filterDealsDto);
    void changeDealUser(String email);
    DealDto changeDealStage(ChangeDealStageReq dealDto);
    List<DealDto> setDealsToArchive();
    List<DealDto> setAdminDealsToArchive(String email);
    List<DealArchiveDto> getDealsArchive(FilterArchiveDto archiveDto);
    DealDto getDealByChat(Integer chatId);
    DealDto changeDealPerformer(String performerEmail);
    DealDto changeDealData(ChangeDealReqDto changeDealReqDto);;
}
