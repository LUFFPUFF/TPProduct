package com.example.domain.api.crm_module.mapper;


import com.example.database.model.crm_module.deal.Deal;

import com.example.database.model.crm_module.deal.DealStatus;
import com.example.domain.api.crm_module.dto.CreateDealDtoReq;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component

public class DealReqDtoToDealMapper {
    public Deal map(CreateDealDtoReq dealDto) {
        Deal deal = new Deal();
        deal.setStage(dealDto.getDealStage());
        deal.setAmount(dealDto.getAmount());
        deal.setContent(dealDto.getContent());
        deal.setClient(dealDto.getClient());
        deal.setUser(dealDto.getUser());
        deal.setStatus(DealStatus.OPENED);
        deal.setCreatedAt(LocalDateTime.now());
        return deal;
    }
}
