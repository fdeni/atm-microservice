package com.project.transaction_service.validator;

import com.project.transaction_service.dto.request.TransactionRequest;
import com.project.transaction_service.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class TransactionValidator {
    public void validateTransactionRequest(TransactionRequest request, TransactionType type) {
        if (request.getAccountFrom() == null || request.getAccountFrom().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AccountFrom is mandatory");
        }

        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }

        if (type == TransactionType.TRANSFER) {
            if (request.getAccountTo() == null || request.getAccountTo().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AccountTo is mandatory for transfer");
            }
        }
    }
}
