package com.example.database.model.company_subscription_module.user_roles;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "user_roles", indexes = {
        @Index(name = "idx_user_roles_user_id_role_id", columnList = "user_id,role_id")
})
@Data
@ToString
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

//    @ManyToOne
//    @JoinColumn(name = "role_id", referencedColumnName = "id")
//    private Role role;
//
//    @ManyToOne
//    @JoinColumn(name = "user_id", referencedColumnName = "id")
//    private User user;
}
