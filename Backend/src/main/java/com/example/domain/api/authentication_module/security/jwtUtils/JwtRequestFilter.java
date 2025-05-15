package com.example.domain.api.authentication_module.security.jwtUtils;

import com.example.database.repository.company_subscription_module.UserRepository;
import com.example.domain.api.authentication_module.cache.AuthCacheService;
import com.example.domain.api.authentication_module.exception_handler_auth.NotFoundUserException;
import com.example.domain.dto.TokenDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
    private final JWTUtilsService jwtUtilsService;
    private final AuthCookieService authCookieService;
    private final AuthCacheService authCacheService;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        TokenDto tokens = authCookieService.getTokensCookie(request);

        if (!tokens.getRefresh_token().isEmpty()) {
            if (!jwtUtilsService.isTokenExpired(tokens.getRefresh_token())) {

                if (tokens.getAccess_token().isEmpty() ||
                        jwtUtilsService.isTokenExpired(tokens.getAccess_token()) ||
                        authCacheService.checkExpireToken(tokens.getRefresh_token()).isPresent()) {
                    tokens = refreshTokens(response, tokens.getRefresh_token());
                }

                putContext(tokens.getAccess_token());
            }
            authCookieService.ExpireTokenCookie(response);
        }

        filterChain.doFilter(request, response);
    }

    private void putContext(String access_token){
        String email = jwtUtilsService.parseToken(access_token).get("email").toString();
        Authentication authentication = new UsernamePasswordAuthenticationToken(email, null,
                jwtUtilsService.getRoles(access_token)
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }


    public TokenDto refreshTokens(HttpServletResponse response,String refreshToken) {
        authCacheService.removeRefreshToken(refreshToken);
        TokenDto tokenDto = userRepository.findByEmail(jwtUtilsService.parseToken(refreshToken).get("email",String.class)).map(user1 ->
                jwtUtilsService.generateTokensByUser(userDetailsService.loadUserByUsername(user1.getEmail())))
                .orElseThrow(NotFoundUserException::new);

        authCookieService.setTokenCookies(response, tokenDto);
        return tokenDto;
    }


}
