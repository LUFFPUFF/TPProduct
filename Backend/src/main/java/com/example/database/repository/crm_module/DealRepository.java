package com.example.database.repository.crm_module;

import com.example.database.model.crm_module.deal.Deal;
import com.example.database.model.crm_module.deal.DealStage;
import com.example.database.model.crm_module.deal.DealStatus;
import com.example.database.model.crm_module.task.TaskPriority;
import com.example.domain.api.crm_module.dto.DealArchiveDto;
import com.example.domain.api.crm_module.dto.DealDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DealRepository extends JpaRepository<Deal, Integer> {
    @Query("SELECT new com.example.domain.api.crm_module.dto.DealDto(d.id,d.user.fullName,d.user.email,d.stage.id," +
            "d.content,d.amount,d.status,d.createdAt,d.client.id,t.title,t.priority) " +
            "FROM Deal d JOIN Task t ON t.deal = d WHERE d.id = :dealId")
    Optional<DealDto> findDealDataByDealId(@Param("dealId") Integer dealId);

    @Query("SELECT new com.example.domain.api.crm_module.dto.DealDto(d.id,d.user.fullName,d.user.email,d.stage.id," +
            "d.content,d.amount,d.status,d.createdAt,d.client.id,t.title,t.priority) " +
            "FROM Deal d JOIN Task t ON t.deal = d WHERE d.user.email = :email " +
            "AND d.status = com.example.database.model.crm_module.deal.DealStatus.OPENED")
    List<DealDto> findDealDataByUserEmail(@Param("email") String userEmail);

    @Query("SELECT new com.example.domain.api.crm_module.dto.DealDto(d.id,d.user.fullName,d.user.email,d.stage.id," +
            "d.content,d.amount,d.status,d.createdAt,d.client.id,t.title,t.priority) " +
            "FROM Deal d JOIN Task t ON t.deal = d WHERE d.user.company.id = :companyId " +
            "AND d.status = com.example.database.model.crm_module.deal.DealStatus.OPENED")
    List<DealDto> findByCompany(@Param("companyId") Integer companyId);
    @Query("SELECT new com.example.domain.api.crm_module.dto.DealArchiveDto(d.id,d.user.fullName,d.user.email," +
            "d.content,d.amount,d.status,d.createdAt,d.client.id,t.title,t.priority,t.dueDate) " +
            "FROM Deal d JOIN Task t ON t.deal = d WHERE d.user.email = :email " +
            "AND d.status = com.example.database.model.crm_module.deal.DealStatus.CLOSED")
    List<DealArchiveDto> findArchiveDataByUserEmail(@Param("email") String userEmail);

    @Query("SELECT new com.example.domain.api.crm_module.dto.DealArchiveDto(d.id,d.user.fullName,d.user.email," +
            "d.content,d.amount,d.status,d.createdAt,d.client.id,t.title,t.priority,t.dueDate) " +
            "FROM Deal d JOIN Task t ON t.deal = d WHERE d.user.company.id = :companyId " +
            "AND d.status = com.example.database.model.crm_module.deal.DealStatus.CLOSED")
    List<DealArchiveDto> findArchiveDataByCompany(@Param("companyId") Integer companyId);


    @Modifying
    @Query("UPDATE Deal d Set d.status = com.example.database.model.crm_module.deal.DealStatus.CLOSED WHERE d.user.email = :email " +
            "AND (d.stage.id = 4 OR d.stage.id = 3)")
    void setDealsToArchive(@Param("email") String email);
}
