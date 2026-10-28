package com.securevault.securevault.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securevault.securevault.common.MessageKeys;
import com.securevault.securevault.dto.ApiResponse;
import com.securevault.securevault.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final MessageSource messageSource;
  private final ObjectMapper objectMapper;

  public JwtAuthenticationFilter(
      JwtService jwtService, MessageSource messageSource, ObjectMapper objectMapper) {
    this.jwtService = jwtService;
    this.messageSource = messageSource;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authorizationHeader = request.getHeader("Authorization");

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authorizationHeader.substring(7);

    try {
      Claims claims = jwtService.extractClaims(token);
      String email = claims.getSubject();
      String role = claims.get("role", String.class);

      if (email != null
          && role != null
          && SecurityContextHolder.getContext().getAuthentication() == null) {

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        SecurityContextHolder.getContext().setAuthentication(authentication);
      } else if (email == null || role == null) {
        writeUnauthorizedResponse(request, response);
        return;
      }

    } catch (JwtException | IllegalArgumentException ex) {
      SecurityContextHolder.clearContext();
      writeUnauthorizedResponse(request, response);
      return;
    }

    filterChain.doFilter(request, response);
  }

  private void writeUnauthorizedResponse(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    String message =
        messageSource.getMessage(MessageKeys.AUTH_TOKEN_INVALID, null, request.getLocale());

    ApiResponse<Void> apiResponse = new ApiResponse<>(false, message, null);

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), apiResponse);
  }
}
