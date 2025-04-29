package com.example.database.repository.crm_module;

import com.example.database.model.crm_module.ClientContacts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientContactsRepository extends JpaRepository<ClientContacts, Integer> {
}
