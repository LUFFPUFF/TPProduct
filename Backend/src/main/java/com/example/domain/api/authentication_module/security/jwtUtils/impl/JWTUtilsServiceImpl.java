package com.example.domain.api.authentication_module.security.jwtUtils.impl;

import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.InvalidTokenSignException;
import com.example.domain.api.authentication_module.security.config.JWTConfig;

import com.example.domain.api.authentication_module.security.jwtUtils.JWTUtilsService;
import com.example.domain.dto.TokenDto;
import com.example.domain.dto.mapper.MapperDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.security.Keys;
import lombok.Data;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Data
public class JWTUtilsServiceImpl implements JWTUtilsService {
    private final JWTConfig jwtConfig;
    private final MapperDto mapperDto;
    private final UserRepository userRepository;
    private final AuthCacheService authCacheService;

    @Override
    public String generateAccessToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email",user.getUsername());
        claims.put("roles",user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return Jwts.builder()
                .setSubject(user.getUsername())
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessExpiration()))
                .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()))
                .compact();
    }

    public List<String> getRoles(String token) {
        return parseToken(token).get("roles", List.class);
    }
    @Override
    public String generateRefreshToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email",user.getUsername());
        claims.put("roles",user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return Jwts.builder()
                .setSubject(user.getUsername())
                .setClaims(claims)
                .setIssuedAt(new Date())
                .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshExpiration()))
                .compact();
    }

    @Override
    public TokenDto generateTokensByUser(UserDetails user) {
        String ref_token = generateRefreshToken(user);
        String acc_token = generateAccessToken(user);
        authCacheService.putRefreshToken(ref_token, user.getUsername());
        return TokenDto.builder()
                .access_token(acc_token)
                .refresh_token(ref_token)
                .build();
    }
    @Override
    public Claims parseToken(String token){
        try {


            return Jwts.parser()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()))
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenSignException();
        }
    }
    @Override
    public boolean isTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (RuntimeException e) {
            return true;
        }
    }
}
