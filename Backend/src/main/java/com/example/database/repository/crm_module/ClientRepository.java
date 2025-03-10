package com.example.database.repository.crm_module;

import com.example.database.model.crm_module.client.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    Optional<Client> findByName(String username);

    Integer findByNameClientId(String username);
}
