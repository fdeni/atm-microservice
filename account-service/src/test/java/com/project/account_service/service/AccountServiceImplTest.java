package com.project.account_service.service;

import com.project.account_service.dto.request.UpdateBalanceRequest;
import com.project.account_service.dto.response.AccountResponse;
import com.project.account_service.entity.Account;
import com.project.account_service.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account account;
    private String accountNumber;

    @BeforeEach
    void setUp() {
        accountNumber = "1234567890";
        account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountName("John Doe");
        account.setBalance(1000.0);
    }

    @Test
    void getAccountByNumber_Success() {
        // Given
        when(accountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));

        // When
        AccountResponse response = accountService.getAccountByNumber(accountNumber);

        // Then
        assertNotNull(response);
        assertEquals(accountNumber, response.getAccountNumber());
        assertEquals("John Doe", response.getAccountName());
        assertEquals(1000.0, response.getBalance());
        verify(accountRepository, times(1)).findByAccountNumber(accountNumber);
    }

    @Test
    void getAccountByNumber_AccountNotFound_ThrowsException() {
        // Given
        when(accountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accountService.getAccountByNumber(accountNumber)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Account not found", exception.getReason());
        verify(accountRepository, times(1)).findByAccountNumber(accountNumber);
    }

    @Test
    void getAllAccounts_Success() {
        // Given
        Account account2 = new Account();
        account2.setAccountNumber("9876543210");
        account2.setAccountName("Jane Smith");
        account2.setBalance(2000.0);

        List<Account> accounts = Arrays.asList(account, account2);
        when(accountRepository.findAll()).thenReturn(accounts);

        // When
        List<AccountResponse> responses = accountService.getAllAccounts();

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());

        assertEquals(accountNumber, responses.get(0).getAccountNumber());
        assertEquals("John Doe", responses.get(0).getAccountName());
        assertEquals(1000.0, responses.get(0).getBalance());

        assertEquals("9876543210", responses.get(1).getAccountNumber());
        assertEquals("Jane Smith", responses.get(1).getAccountName());
        assertEquals(2000.0, responses.get(1).getBalance());

        verify(accountRepository, times(1)).findAll();
    }

    @Test
    void getAllAccounts_EmptyList() {
        // Given
        when(accountRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<AccountResponse> responses = accountService.getAllAccounts();

        // Then
        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verify(accountRepository, times(1)).findAll();
    }

    @Test
    void updateBalance_Deposit_Success() {
        // Given
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAccountNumber(accountNumber);
        request.setAmount(500.0);
        request.setType("DEPOSIT");

        when(accountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When
        AccountResponse response = accountService.updateBalance(request);

        // Then
        assertNotNull(response);
        assertEquals(accountNumber, response.getAccountNumber());
        assertEquals(1500.0, response.getBalance());
        verify(accountRepository, times(1)).findByAccountNumber(accountNumber);
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void updateBalance_Deposit_CaseInsensitive() {
        // Given
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAccountNumber(accountNumber);
        request.setType("deposit"); // lowercase
        request.setAmount(300.0);

        when(accountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When
        AccountResponse response = accountService.updateBalance(request);

        // Then
        assertEquals(1300.0, response.getBalance());
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void updateBalance_Withdraw_Success() {
        // Given
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAccountNumber(accountNumber);
        request.setType("WITHDRAW");
        request.setAmount(300.0);

        when(accountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When
        AccountResponse response = accountService.updateBalance(request);

        // Then
        assertNotNull(response);
        assertEquals(accountNumber, response.getAccountNumber());
        assertEquals(700.0, response.getBalance());
        verify(accountRepository, times(1)).findByAccountNumber(accountNumber);
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void updateBalance_Withdraw_InsufficientBalance_ThrowsException() {
        // Given
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAccountNumber(accountNumber);
        request.setType("WITHDRAW");
        request.setAmount(1500.0); // More than balance

        when(accountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accountService.updateBalance(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Insufficient balance", exception.getReason());
        verify(accountRepository, times(1)).findByAccountNumber(accountNumber);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void updateBalance_Withdraw_ExactBalance_Success() {
        // Given
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAccountNumber(accountNumber);
        request.setType("WITHDRAW");
        request.setAmount(1000.0); // Exact balance

        when(accountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When
        AccountResponse response = accountService.updateBalance(request);

        // Then
        assertEquals(0.0, response.getBalance());
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void updateBalance_AccountNotFound_ThrowsException() {
        // Given
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAccountNumber(accountNumber);
        request.setType("DEPOSIT");
        request.setAmount(500.0);

        when(accountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.empty());

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accountService.updateBalance(request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Account not found", exception.getReason());
        verify(accountRepository, times(1)).findByAccountNumber(accountNumber);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void updateBalance_InvalidTransactionType_ThrowsException() {
        // Given
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setAccountNumber(accountNumber);
        request.setType("TRANSFER"); // Invalid type
        request.setAmount(500.0);

        when(accountRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(account));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accountService.updateBalance(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid transaction type", exception.getReason());
        verify(accountRepository, times(1)).findByAccountNumber(accountNumber);
        verify(accountRepository, never()).save(any(Account.class));
    }
}
