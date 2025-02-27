package com.example.repository.integration_module;

import com.example.model.integration_module.Integration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntegrationRepository extends JpaRepository<Integration, Integration> {
}
