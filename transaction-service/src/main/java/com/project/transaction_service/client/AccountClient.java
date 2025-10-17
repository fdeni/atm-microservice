package com.project.transaction_service.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.transaction_service.dto.request.UpdateBalanceRequest;
import com.project.transaction_service.dto.response.UpdateBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Component
@RequiredArgsConstructor
public class AccountClient {
    private final RestTemplate restTemplate;

    @Value("${account.service.url}")
    private String accountServiceUrl;

    public UpdateBalanceResponse updateBalance(UpdateBalanceRequest request) {
        String token = ((ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes())
                .getRequest()
                .getHeader(HttpHeaders.AUTHORIZATION);
        String validationUrl = accountServiceUrl + "/api/accounts/update-balance";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.AUTHORIZATION, token);

            HttpEntity<UpdateBalanceRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<UpdateBalanceResponse> response = restTemplate.exchange(validationUrl, HttpMethod.PUT, entity, UpdateBalanceResponse.class);
            return new UpdateBalanceResponse(response.getStatusCodeValue(), "Balance updated successfully");
        } catch (HttpClientErrorException e) {
            String message;
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(e.getResponseBodyAsString());
                message = node.has("error") ? node.get("error").asText() : e.getStatusText();
            } catch (Exception ex) {
                message = e.getStatusText();
            }
            return new UpdateBalanceResponse(e.getRawStatusCode(), message);
        } catch (Exception e) {
            return new UpdateBalanceResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
        }
    }
}
