package com.example.repository.crm_module;

import com.example.model.crm_module.ClientNotes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientNotesRepository extends JpaRepository<ClientNotes, Integer> {
}
