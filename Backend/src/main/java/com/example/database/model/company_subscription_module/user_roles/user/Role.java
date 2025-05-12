package com.example.database.model.company_subscription_module.user_roles.user;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    MANAGER,
    OPERATOR,
    LOCAL;

    @Override
    public String getAuthority() {
        return name();
    }
}
