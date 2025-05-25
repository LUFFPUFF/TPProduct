package com.example.domain.api.crm_module.service.impl;

import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.database.model.crm_module.client.Client;
import com.example.database.model.crm_module.deal.Deal;
import com.example.database.model.crm_module.deal.DealStatus;
import com.example.database.model.crm_module.task.Task;
import com.example.database.repository.chats_messages_module.ChatRepository;
import com.example.database.repository.crm_module.ClientRepository;
import com.example.database.repository.crm_module.DealRepository;
import com.example.database.repository.crm_module.DealStageRepository;
import com.example.database.repository.crm_module.TaskRepository;
import com.example.domain.api.authentication_module.service.interfaces.CurrentUserDataService;
import com.example.domain.api.crm_module.dto.*;
import com.example.domain.api.crm_module.exception_handler_crm.*;
import com.example.domain.api.crm_module.mapper.DealReqDtoToDealMapper;
import com.example.domain.api.crm_module.mapper.DealReqDtoToTaskMapper;
import com.example.domain.api.crm_module.mapper.DealToDealDtoMapper;
import com.example.domain.api.crm_module.service.DealService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DealServiceImpl implements DealService {
    private final DealRepository dealRepository;
    private final ClientRepository clientRepository;
    private final DealReqDtoToDealMapper dealReqDtoToDealMapper;
    private final DealReqDtoToTaskMapper dealReqDtoToTaskMapper;
    private final DealToDealDtoMapper dealToDealDtoMapper;
    private final TaskRepository taskRepository;
    private final DealStageRepository dealStageRepository;
    private final CurrentUserDataService currentUserDataService;
    private final ChatRepository chatRepository;

    @Override
    @Transactional
    public DealDto createDeal(CreateDealDtoReq dealDto) {
        dealDto.setDealStatus(DealStatus.OPENED);
        dealDto.setDealStage(dealStageRepository.findById(0).orElseThrow(NotFoundDealStageException::new));
        dealDto.setClient(clientRepository.findById(dealDto.getClient_id()).orElseThrow(NotFoundClientForDealException::new));
        dealDto.setUser(currentUserDataService.getUser());
        Deal deal = dealRepository.save(dealReqDtoToDealMapper.map(dealDto));
        Task task = taskRepository.save(dealReqDtoToTaskMapper.map(dealDto, deal));

        return dealToDealDtoMapper.map(deal, task);
    }

    @Override
    @Transactional
    public DealDto getDeal(Integer id) {
        return dealRepository.findDealDataByDealId(id).orElseThrow(NotFoundDealDataException::new);
    }
    @Override
    @Transactional
    public List<DealDto> getDeals(FilterDealsDto filterDealsDto) {
        boolean isManager = currentUserDataService.hasRole(Role.MANAGER);
        List<DealDto> deals;
        if(filterDealsDto.getEmail() == null && !isManager) {
            filterDealsDto.setEmail(currentUserDataService.getUserEmail());
            deals = dealRepository.findDealDataByUserEmail(filterDealsDto.getEmail());

        }else if(filterDealsDto.getEmail() != null && !isManager) {
            throw new AccessDeniedFilterDeals();
        }else if(filterDealsDto.getEmail() == null){
            deals = dealRepository.findByCompany(currentUserDataService.getUser().getCompany().getId());
        } else {
            deals = dealRepository.findDealDataByUserEmail(filterDealsDto.getEmail());
        }
        return deals.stream()
                .filter(dealDto -> dealDto.getEmail().equals(filterDealsDto.getEmail()))
                .filter(dealDto -> filterDealsDto.getMinAmount() == null||dealDto.getAmount() >= filterDealsDto.getMinAmount())
                .filter(dealDto -> filterDealsDto.getMaxAmount() == null||dealDto.getAmount() <= filterDealsDto.getMaxAmount())
                .filter(dealDto -> filterDealsDto.getStage() == null || dealDto.getStageId().equals(filterDealsDto.getStage()))
                .filter(dealDto -> filterDealsDto.getPriority() == null || (filterDealsDto.getPriority().contains(dealDto.getPriority())))
                .toList();
    }

    @Override
    @Transactional
    public DealDto changeDealStage(ChangeDealStageReq dealDto) {
        Deal deal = dealRepository.findById(dealDto.getDealId()).orElseThrow(NotFoundDealDataException::new);
        checkDealChangeAccess(deal);
        dealRepository.updateDealStageById(dealDto.getDealId(), dealDto.getStageId());
        return getDeal(dealDto.getDealId());
    }

    private void checkDealChangeAccess(Deal deal) {
        if (deal.getStatus().equals(DealStatus.CLOSED)) {
            throw new UpdateDealException("Сделка закрыта и изменить ее данные нельзя", HttpStatus.BAD_REQUEST);
        }
        if (!Objects.equals(deal.getUser().getEmail(), currentUserDataService.getUserEmail()) && !currentUserDataService.hasRole(Role.MANAGER)) {
            throw new UpdateDealException("Нельзя изменить данные чужой сделки", HttpStatus.FORBIDDEN);
        }
    }

    @Override
    @Transactional
    public List<DealDto> setDealsToArchive() {
        dealRepository.setDealsToArchive(currentUserDataService.getUserEmail());
        return getDeals(FilterDealsDto.builder().build());
    }
    @Transactional
    @Override
    public List<DealDto> setAdminDealsToArchive(String email) {
        dealRepository.setDealsToArchive(email);
        return getDeals(FilterDealsDto.builder().email(email).build());
    }

    @Override
    public List<DealArchiveDto> getDealsArchive(FilterArchiveDto archiveDto) {
        boolean isManager = currentUserDataService.hasRole(Role.MANAGER);
        List<DealArchiveDto> deals;
        if(archiveDto.getEmail() == null && !isManager) {
            archiveDto.setEmail(currentUserDataService.getUserEmail());
            deals = dealRepository.findArchiveDataByUserEmail(archiveDto.getEmail());

        }else if(archiveDto.getEmail() != null && !isManager) {
            throw new AccessDeniedFilterDeals();
        }else if(archiveDto.getEmail() == null){
            deals = dealRepository.findArchiveDataByCompany(currentUserDataService.getUser().getCompany().getId());
        } else {
            deals = dealRepository.findArchiveDataByUserEmail(archiveDto.getEmail());
        }

        return deals.stream()
                .filter(dealArchiveDto -> dealArchiveDto.getEmail().equals(archiveDto.getEmail()))
                .filter(dealArchiveDto -> archiveDto.getFromEndDateTime() == null || dealArchiveDto.getDueDate().isAfter(archiveDto.getFromEndDateTime()))
                .filter(dealArchiveDto -> archiveDto.getToEndDateTime() == null || dealArchiveDto.getDueDate().isBefore(archiveDto.getToEndDateTime()))
                .toList();
    }

    @Override
    public DealDto getDealByChat(Integer cliendId) {
      //TODO: не знаю какие данные хранятся в чате надо уточнить
        return null;
    }

    @Override
    public DealDto changeDealPerformer(String performerEmail) {
        //TODO:Метод из чата для перенаправления чата другому человеку
        return null;

    }

    @Override
    @Transactional
    public DealDto changeDealData(ChangeDealReqDto changeDealReqDto) {
        Deal deal = dealRepository.findById(changeDealReqDto.getDealId()).orElseThrow((NotFoundDealDataException::new));
        Task task = taskRepository.findTaskByDealId(changeDealReqDto.getDealId());
        if (changeDealReqDto.getTitle() != null) {
            task.setTitle(changeDealReqDto.getTitle());
        }
        if (changeDealReqDto.getContent() != null) {
            deal.setContent(changeDealReqDto.getContent());
        }
        if (changeDealReqDto.getPriority() != null) {
            task.setPriority(changeDealReqDto.getPriority());
        }
        dealRepository.save(deal);
        taskRepository.save(task);

        return getDeal(changeDealReqDto.getDealId());

    }
}
