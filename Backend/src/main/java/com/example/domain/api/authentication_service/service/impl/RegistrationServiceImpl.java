package com.example.domain.api.authentication_service.service.impl;

import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_service.exception_handler_auth.EmailExistsException;
import com.example.domain.api.authentication_service.security.jwtUtils.JWTUtilsService;
import com.example.domain.api.authentication_service.service.interfaces.RegistrationService;
import com.example.domain.dto.RegistrationDto;
import com.example.domain.dto.TokenDto;
import com.example.domain.dto.mapper.MapperDto;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;
    private final MapperDto mapperDto;
    private final JWTUtilsService jWTUtilsService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    @Override
    @Transactional
    public TokenDto registerUser(RegistrationDto registrationDto) {
        checkEmailIsAvailable(registrationDto.getEmail());
        registrationDto.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        userRepository.save(mapperDto.toEntityUser(registrationDto));
        return jWTUtilsService.generateTokensByUser(userDetailsService.loadUserByUsername(registrationDto.getEmail()));


    }

    @Override
    @Transactional
    public void checkEmailIsAvailable(String email) {
         userRepository.findByEmail(email).orElseThrow(EmailExistsException::new);
    }
}
