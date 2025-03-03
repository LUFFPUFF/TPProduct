package com.example.database.model.company_subscription_module.user_roles.role;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles", indexes = {
        @Index(name = "idx_roles_name", columnList = "name")
})
@Data
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name")
    private RoleName name;

    @Column(name = "description")
    private String description;
}
