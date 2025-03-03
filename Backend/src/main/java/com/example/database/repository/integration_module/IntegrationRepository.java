package com.example.database.repository.integration_module;

import com.example.database.model.integration_module.Integration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntegrationRepository extends JpaRepository<Integration, Integration> {
}
