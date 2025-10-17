package com.project.account_service.controller;

import com.project.account_service.dto.response.AccountResponse;
import com.project.account_service.dto.response.Base;
import com.project.account_service.dto.request.UpdateBalanceRequest;
import com.project.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountServiceController {
    private final AccountService accountService;

    @GetMapping
    private Base<List<AccountResponse>> getAllAccounts() {
        return Base.<List<AccountResponse>>builder()
                .data(accountService.getAllAccounts())
                .status(HttpStatus.OK.value())
                .build();
    }

    @GetMapping("/{accountNumber}")
    public Base<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return Base.<AccountResponse>builder()
                .data(accountService.getAccountByNumber(accountNumber))
                .status(HttpStatus.OK.value())
                .build();
    }

    @PutMapping("/update-balance")
    public Base<AccountResponse> updateBalance(@RequestBody UpdateBalanceRequest request) {
        return Base.<AccountResponse>builder()
                .data(accountService.updateBalance(request))
                .status(HttpStatus.OK.value())
                .build();
    }
}
