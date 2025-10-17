package com.project.account_service.utils;

import com.project.account_service.dto.data.UserData;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthValidator {
    @Value("${auth.service.url}")
    private String authServiceUrl;

    public UserData validateToken(String token)  {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, token);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        String validationUrl = authServiceUrl + "/api/auth/validate";

        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    validationUrl,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                Map<String, Object> responseData = (Map<String, Object>) responseEntity.getBody().get("data");

                Number id = (Number) responseData.get("id");
                String username = (String) responseData.get("username");
                return UserData.builder()
                        .id(id.longValue())
                        .username(username)
                        .build();
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication failed");
        }
    }
}
