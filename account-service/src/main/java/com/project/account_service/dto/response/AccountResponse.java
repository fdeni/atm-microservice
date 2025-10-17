package com.project.account_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountResponse {
    private String accountNumber;
    private String accountName;
    private Double balance;
}
