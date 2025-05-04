package com.example.domain.api.authentication_module.security;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.database.repository.company_subscription_module.UserRoleRepository;

import com.example.domain.dto.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email"));

        List<GrantedAuthority> authorities = new ArrayList<>(userRoleRepository.findRolesByEmail(email));

        Integer companyId = (user.getCompany() != null) ? user.getCompany().getId() : null;

        return new AppUserDetails(
                user.getId(),
                companyId,
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
