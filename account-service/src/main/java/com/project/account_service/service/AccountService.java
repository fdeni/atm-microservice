package com.project.account_service.service;

import com.project.account_service.dto.response.AccountResponse;
import com.project.account_service.dto.request.UpdateBalanceRequest;

import java.util.List;

public interface AccountService {
    AccountResponse getAccountByNumber(String accountNumber);
    List<AccountResponse> getAllAccounts();
    AccountResponse updateBalance(UpdateBalanceRequest request);
}
