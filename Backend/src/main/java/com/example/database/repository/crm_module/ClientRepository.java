package com.example.database.repository.crm_module;

import com.example.database.model.crm_module.client.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    Optional<Client> findByName(@Param("name") String username);

    Optional<Client> findByCompanyId(@Param("companyId") Integer companyId);

    Optional<Client> findByNameAndCompanyId(String name, Integer companyId);
}
