package com.example.database.model.crm_module;

import com.example.database.model.crm_module.client.Client;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "client_contacts", indexes = {
        @Index(name = "idx_client_contacts_client_id", columnList = "client_id"),
        @Index(name = "idx_client_contacts_type", columnList = "type")
})
@Data
public class ClientContacts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @Column(name = "type")
    private String type;

    @Column(name = "value")
    private String value;


}
