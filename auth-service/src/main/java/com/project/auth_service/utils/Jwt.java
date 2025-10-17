package com.project.auth_service.utils;

import com.project.auth_service.dto.data.AuthenticationData;
import com.project.auth_service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class Jwt {
    @Value("${jwt.secret-key}")
    private String secretKey;

    public String generateToken(User user) {
        long expiredMillis = System.currentTimeMillis() + (1000 * 60 * 60 * 2);
        Date expiredDate = new Date(expiredMillis);

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("name", user.getName());

        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(expiredDate)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public AuthenticationData validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            Claims claimsBody = claims.getBody();

            return AuthenticationData.builder()
                    .id(claimsBody.get("id", Long.class))
                    .username(claimsBody.get("username", String.class))
                    .name(claimsBody.get("name", String.class))
                    .token(token)
                    .expiredAt(claimsBody.getExpiration())
                    .build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed");
        }
    }
}
