package com.example.database.model.crm_module;

import com.example.database.model.crm_module.client.Client;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "client_notes", indexes = {
        @Index(name = "idx_client_notes_client_id", columnList = "client_id"),
        @Index(name = "idx_client_notes_created_at", columnList = "created_at")
})
@Data
public class ClientNotes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
