package com.example.domain.api.authentication_service.controller;


import com.example.database.model.company_subscription_module.user_roles.user.Role;
import com.example.domain.api.authentication_service.cache.AuthCacheService;
import com.example.domain.api.authentication_service.service.interfaces.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;
    private final AuthCacheService authCacheService;
    @PostMapping("/role/user-role-manager")
    public ResponseEntity<Boolean> addUserRoleManager(String userEmail) {
        authCacheService.putExpiredData(userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.addRole(userEmail, Role.MANAGER));
    }
    @PostMapping("/role/user-role-operator")
    public ResponseEntity<Boolean> addUserRoleOperator(String userEmail) {
        authCacheService.putExpiredData(userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.addRole(userEmail, Role.OPERATOR));
    }
}
