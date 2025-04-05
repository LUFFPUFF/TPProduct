package com.example.domain.api.authentication_service.security.jwtUtils;

import com.example.database.model.company_subscription_module.user_roles.user.User;
import com.example.domain.dto.TokenDto;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface JWTUtilsService {

   String generateAccessToken(UserDetails user);
   String generateRefreshToken(UserDetails user);
   TokenDto generateTokensByUser(UserDetails user);
   Claims parseToken(String token);
   List<String> getRoles(String token);
   boolean isTokenExpired(String token);
}
