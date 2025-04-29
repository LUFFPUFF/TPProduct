package com.example.database.repository.crm_module;

import com.example.database.model.crm_module.ClientNotes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientNotesRepository extends JpaRepository<ClientNotes, Integer> {
}
