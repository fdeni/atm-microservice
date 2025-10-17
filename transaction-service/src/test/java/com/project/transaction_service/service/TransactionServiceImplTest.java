package com.project.transaction_service.service;

import com.project.transaction_service.client.AccountClient;
import com.project.transaction_service.dto.request.TransactionRequest;
import com.project.transaction_service.dto.request.UpdateBalanceRequest;
import com.project.transaction_service.dto.response.TransactionResponse;
import com.project.transaction_service.dto.response.UpdateBalanceResponse;
import com.project.transaction_service.entity.Transaction;
import com.project.transaction_service.enums.TransactionStatus;
import com.project.transaction_service.enums.TransactionType;
import com.project.transaction_service.repository.TransactionRepository;
import com.project.transaction_service.validator.TransactionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private TransactionValidator transactionValidator;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private TransactionRequest transactionRequest;
    private Transaction transaction;
    private UpdateBalanceResponse successResponse;
    private UpdateBalanceResponse failedResponse;

    @BeforeEach
    void setUp() {
        transactionRequest = new TransactionRequest();
        transactionRequest.setAccountFrom("1234567890");
        transactionRequest.setAmount(1000.0);

        transaction = Transaction.builder()
                .id(1L)
                .transactionId("TX-123")
                .accountFrom("1234567890")
                .amount(1000.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .transactionDate(LocalDateTime.now())
                .build();

        successResponse = UpdateBalanceResponse.builder()
                .status(200)
                .message("Success")
                .build();

        failedResponse = UpdateBalanceResponse.builder()
                .status(400)
                .message("Failed")
                .build();
    }

    @Test
    void deposit_Success() {
        // Given
        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.DEPOSIT));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class))).thenReturn(successResponse);

        // When
        Transaction result = transactionService.deposit(transactionRequest);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        verify(transactionValidator, times(1))
                .validateTransactionRequest(transactionRequest, TransactionType.DEPOSIT);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountClient, times(1)).updateBalance(any(UpdateBalanceRequest.class));
    }

    @Test
    void deposit_FailedDueToAccountClientError() {
        // Given
        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.DEPOSIT));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class))).thenReturn(failedResponse);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.deposit(transactionRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(transactionRepository, times(3)).save(transactionCaptor.capture());
        // First save: PENDING, Second save: FAILED (in handleBalanceUpdate), Third save: FAILED (in catch)
        List<Transaction> savedTransactions = transactionCaptor.getAllValues();
        assertEquals(TransactionStatus.FAILED, savedTransactions.get(2).getStatus());
    }

    @Test
    void deposit_FailedDueToResponseStatusException() {
        // Given
        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.DEPOSIT));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.deposit(transactionRequest)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.FAILED, transactionCaptor.getValue().getStatus());
    }

    @Test
    void deposit_FailedDueToGenericException() {
        // Given
        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.DEPOSIT));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.deposit(transactionRequest)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.FAILED, transactionCaptor.getValue().getStatus());
    }

    @Test
    void deposit_ValidationFails() {
        // Given
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid amount"))
                .when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.DEPOSIT));

        // When & Then
        assertThrows(ResponseStatusException.class, () -> transactionService.deposit(transactionRequest));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountClient, never()).updateBalance(any(UpdateBalanceRequest.class));
    }

    @Test
    void withdrawal_Success() {
        // Given
        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.WITHDRAW));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class))).thenReturn(successResponse);

        // When
        Transaction result = transactionService.withdrawal(transactionRequest);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        verify(transactionValidator, times(1))
                .validateTransactionRequest(transactionRequest, TransactionType.WITHDRAW);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountClient, times(1)).updateBalance(any(UpdateBalanceRequest.class));
    }

    @Test
    void withdrawal_FailedDueToInsufficientBalance() {
        // Given
        UpdateBalanceResponse insufficientResponse = UpdateBalanceResponse.builder()
                .status(400)
                .message("Insufficient balance")
                .build();

        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.WITHDRAW));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class))).thenReturn(insufficientResponse);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.withdrawal(transactionRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(transactionRepository, times(3)).save(transactionCaptor.capture());
        // First save: PENDING, Second save: FAILED (in handleBalanceUpdate), Third save: FAILED (in catch)
        List<Transaction> savedTransactions = transactionCaptor.getAllValues();
        assertEquals(TransactionStatus.FAILED, savedTransactions.get(2).getStatus());
    }

    @Test
    void withdrawal_FailedDueToResponseStatusException() {
        // Given
        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.WITHDRAW));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.withdrawal(transactionRequest)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.FAILED, transactionCaptor.getValue().getStatus());
    }

    @Test
    void withdrawal_FailedDueToGenericException() {
        // Given
        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.WITHDRAW));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class)))
                .thenThrow(new RuntimeException("Network error"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.withdrawal(transactionRequest)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transfer_Success() {
        // Given
        transactionRequest.setAccountTo("9876543210");

        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.TRANSFER));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class))).thenReturn(successResponse);

        // When
        Transaction result = transactionService.transfer(transactionRequest);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        verify(transactionValidator, times(1))
                .validateTransactionRequest(transactionRequest, TransactionType.TRANSFER);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountClient, times(2)).updateBalance(any(UpdateBalanceRequest.class)); // withdraw + deposit
    }

    @Test
    void transfer_FailedDuringWithdraw() {
        // Given
        transactionRequest.setAccountTo("9876543210");

        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.TRANSFER));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class)))
                .thenReturn(failedResponse);

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.transfer(transactionRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(transactionRepository, times(3)).save(transactionCaptor.capture());
        // First save: PENDING, Second save: FAILED (in handleBalanceUpdate), Third save: FAILED (in catch)
        List<Transaction> savedTransactions = transactionCaptor.getAllValues();
        assertEquals(TransactionStatus.FAILED, savedTransactions.get(2).getStatus());
        verify(accountClient, times(1)).updateBalance(any(UpdateBalanceRequest.class)); // Only withdraw attempted
    }

    @Test
    void transfer_FailedDuringDeposit() {
        // Given
        transactionRequest.setAccountTo("9876543210");

        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.TRANSFER));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class)))
                .thenReturn(successResponse)  // withdraw success
                .thenReturn(failedResponse);   // deposit failed

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.transfer(transactionRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(transactionRepository, times(3)).save(transactionCaptor.capture());
        // First save: PENDING, Second save: FAILED (in handleBalanceUpdate), Third save: FAILED (in catch)
        List<Transaction> savedTransactions = transactionCaptor.getAllValues();
        assertEquals(TransactionStatus.FAILED, savedTransactions.get(2).getStatus());
        verify(accountClient, times(2)).updateBalance(any(UpdateBalanceRequest.class));
    }

    @Test
    void transfer_FailedDueToResponseStatusException() {
        // Given
        transactionRequest.setAccountTo("9876543210");

        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.TRANSFER));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.transfer(transactionRequest)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transfer_FailedDueToGenericException() {
        // Given
        transactionRequest.setAccountTo("9876543210");

        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.TRANSFER));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class)))
                .thenThrow(new RuntimeException("System error"));

        // When & Then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> transactionService.transfer(transactionRequest)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transfer_ValidationFails() {
        // Given
        transactionRequest.setAccountTo("9876543210");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transfer"))
                .when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.TRANSFER));

        // When & Then
        assertThrows(ResponseStatusException.class, () -> transactionService.transfer(transactionRequest));
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(accountClient, never()).updateBalance(any(UpdateBalanceRequest.class));
    }

    @Test
    void getTransaction_Success() {
        // Given
        String accountNumber = "1234567890";
        Transaction tx1 = Transaction.builder()
                .id(1L)
                .transactionId("TX-001")
                .accountFrom(accountNumber)
                .amount(1000.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction tx2 = Transaction.builder()
                .id(2L)
                .transactionId("TX-002")
                .accountFrom(accountNumber)
                .accountTo("9876543210")
                .amount(500.0)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        List<Transaction> transactions = Arrays.asList(tx1, tx2);
        when(transactionRepository.findByAccountFrom(accountNumber)).thenReturn(transactions);

        // When
        List<TransactionResponse> result = transactionService.getTransaction(accountNumber);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("TX-001", result.get(0).getTransactionId());
        assertEquals("TX-002", result.get(1).getTransactionId());
        assertEquals(TransactionType.DEPOSIT, result.get(0).getType());
        assertEquals(TransactionType.TRANSFER, result.get(1).getType());
        verify(transactionRepository, times(1)).findByAccountFrom(accountNumber);
    }

    @Test
    void getTransaction_EmptyList() {
        // Given
        String accountNumber = "1234567890";
        when(transactionRepository.findByAccountFrom(accountNumber)).thenReturn(Arrays.asList());

        // When
        List<TransactionResponse> result = transactionService.getTransaction(accountNumber);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(transactionRepository, times(1)).findByAccountFrom(accountNumber);
    }

    @Test
    void getTransaction_WithDifferentStatuses() {
        // Given
        String accountNumber = "1234567890";
        Transaction tx1 = Transaction.builder()
                .id(1L)
                .transactionId("TX-001")
                .accountFrom(accountNumber)
                .amount(1000.0)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        Transaction tx2 = Transaction.builder()
                .id(2L)
                .transactionId("TX-002")
                .accountFrom(accountNumber)
                .amount(500.0)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.FAILED)
                .createdAt(LocalDateTime.now())
                .build();

        List<Transaction> transactions = Arrays.asList(tx1, tx2);
        when(transactionRepository.findByAccountFrom(accountNumber)).thenReturn(transactions);

        // When
        List<TransactionResponse> result = transactionService.getTransaction(accountNumber);

        // Then
        assertEquals(2, result.size());
        assertEquals(TransactionStatus.SUCCESS, result.get(0).getStatus());
        assertEquals(TransactionStatus.FAILED, result.get(1).getStatus());
    }

    @Test
    void deposit_TransactionStatusFlow() {
        // Given
        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.DEPOSIT));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class))).thenReturn(successResponse);

        // When
        Transaction result = transactionService.deposit(transactionRequest);

        // Then
        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(transactionRequest.getAccountFrom(), result.getAccountFrom());
        assertEquals(transactionRequest.getAmount(), result.getAmount());
        assertEquals(TransactionType.DEPOSIT, result.getType());
        assertNotNull(result.getTransactionId());
        assertNotNull(result.getCreatedAt());

        // Verify repository save was called twice (initial save + final save)
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountClient, times(1)).updateBalance(any(UpdateBalanceRequest.class));
    }

    @Test
    void withdrawal_TransactionIdAndFieldsAreSet() {
        // Given
        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.WITHDRAW));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class))).thenReturn(successResponse);

        // When
        Transaction result = transactionService.withdrawal(transactionRequest);

        // Then
        assertNotNull(result);
        assertNotNull(result.getTransactionId());
        assertFalse(result.getTransactionId().isEmpty());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertEquals(TransactionType.WITHDRAW, result.getType());
        assertEquals(transactionRequest.getAccountFrom(), result.getAccountFrom());
        assertEquals(transactionRequest.getAmount(), result.getAmount());

        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transfer_AllFieldsAreSetCorrectly() {
        // Given
        String accountTo = "9876543210";
        transactionRequest.setAccountTo(accountTo);

        doNothing().when(transactionValidator)
                .validateTransactionRequest(any(TransactionRequest.class), eq(TransactionType.TRANSFER));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountClient.updateBalance(any(UpdateBalanceRequest.class))).thenReturn(successResponse);

        // When
        Transaction result = transactionService.transfer(transactionRequest);

        // Then
        assertNotNull(result);
        assertEquals(accountTo, result.getAccountTo());
        assertEquals(transactionRequest.getAccountFrom(), result.getAccountFrom());
        assertEquals(transactionRequest.getAmount(), result.getAmount());
        assertEquals(TransactionType.TRANSFER, result.getType());
        assertEquals(TransactionStatus.SUCCESS, result.getStatus());
        assertNotNull(result.getTransactionId());
        assertNotNull(result.getCreatedAt());

        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(accountClient, times(2)).updateBalance(any(UpdateBalanceRequest.class));
    }
}
