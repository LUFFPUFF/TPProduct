package com.example.database.repository.integration_module;

import com.example.database.model.chats_messages_module.ChatAttachment;
import com.example.database.model.integration_module.Integration;
import com.example.database.model.integration_module.IntegrationStatus;
import com.example.database.model.integration_module.IntegrationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationRepository extends JpaRepository<Integration, Integration> {

    List<Integration> findByCompanyIdAndTypeAndStatus(Integer companyId, IntegrationType type, IntegrationStatus status);
}
