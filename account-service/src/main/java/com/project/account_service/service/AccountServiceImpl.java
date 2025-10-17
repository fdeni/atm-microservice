package com.project.account_service.service;

import com.project.account_service.dto.response.AccountResponse;
import com.project.account_service.dto.request.UpdateBalanceRequest;
import com.project.account_service.entity.Account;
import com.project.account_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;

    @Override
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        return AccountResponse.builder().
                accountNumber(account.getAccountNumber()).
                accountName(account.getAccountName()).
                balance(account.getBalance()).
                build();
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        return accounts.stream().map(it ->
                AccountResponse.builder().
                        accountNumber(it.getAccountNumber()).
                        accountName(it.getAccountName()).
                        balance(it.getBalance()).build()).
                collect(Collectors.toList());
    }

    @Override
    public AccountResponse updateBalance(UpdateBalanceRequest request) {
        Account account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        if ("DEPOSIT".equalsIgnoreCase(request.getType())) {
            account.setBalance(account.getBalance() + request.getAmount());
        } else if ("WITHDRAW".equalsIgnoreCase(request.getType())) {
            if (account.getBalance() < request.getAmount()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
            }
            account.setBalance(account.getBalance() - request.getAmount());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transaction type");
        }

        accountRepository.save(account);

        return AccountResponse.builder().
                accountNumber(account.getAccountNumber()).
                accountName(account.getAccountName()).
                balance(account.getBalance()).build();
    }
}
