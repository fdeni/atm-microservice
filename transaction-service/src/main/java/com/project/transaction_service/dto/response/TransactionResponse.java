package com.project.transaction_service.dto.response;

import com.project.transaction_service.enums.TransactionStatus;
import com.project.transaction_service.enums.TransactionType;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
public class TransactionResponse {
    private Long id;
    private String transactionId;
    private String accountFrom;
    private String accountTo;
    private Double amount;
    private TransactionType type;
    private TransactionStatus status;
    private LocalDateTime createdAt;
}
