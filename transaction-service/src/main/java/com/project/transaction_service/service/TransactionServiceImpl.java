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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final TransactionValidator transactionValidator;

    @Transactional
    @Override
    public Transaction deposit(TransactionRequest request) {
        log.info("Processing deposit for account {}", request.getAccountFrom());
        transactionValidator.validateTransactionRequest(request, TransactionType.DEPOSIT);

        Transaction tx = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountFrom(request.getAccountFrom())
                .amount(request.getAmount())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);

        try {
            handleBalanceUpdate(request, TransactionType.DEPOSIT, tx);
            tx.setStatus(TransactionStatus.SUCCESS);
        } catch (ResponseStatusException e) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw e;
        } catch (Exception e) {
            log.error("Deposit failed: {}", e.getMessage());
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return transactionRepository.save(tx);
    }

    @Transactional
    @Override
    public Transaction withdrawal(TransactionRequest request) {
        log.info("Processing withdrawal for account {}", request.getAccountFrom());
        transactionValidator.validateTransactionRequest(request, TransactionType.WITHDRAW);

        Transaction tx = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountFrom(request.getAccountFrom())
                .amount(request.getAmount())
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);

        try {
            handleBalanceUpdate(request, TransactionType.WITHDRAW, tx);
            tx.setStatus(TransactionStatus.SUCCESS);
        } catch (ResponseStatusException e) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw e;
        } catch (Exception e) {
            log.error("Withdrawal failed: {}", e.getMessage());
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return transactionRepository.save(tx);
    }

    @Transactional
    @Override
    public Transaction transfer(TransactionRequest request) {
        log.info("Processing transfer from {} to {}", request.getAccountFrom(), request.getAccountTo());
        transactionValidator.validateTransactionRequest(request, TransactionType.TRANSFER);

        Transaction tx = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .accountFrom(request.getAccountFrom())
                .accountTo(request.getAccountTo())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .transactionDate(LocalDateTime.now())
                .build();

        transactionRepository.save(tx);

        try {
            // Kurangi saldo pengirim
            handleBalanceUpdate(request, TransactionType.WITHDRAW, tx);

            // Tambah saldo penerima
            handleBalanceUpdate(request, TransactionType.DEPOSIT, tx);

            tx.setStatus(TransactionStatus.SUCCESS);
        } catch (ResponseStatusException e) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw e;
        } catch (Exception e) {
            log.error("Transfer failed: {}", e.getMessage());
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

        return transactionRepository.save(tx);
    }

    @Override
    public List<TransactionResponse> getTransaction(String accountNumber) {
        List<Transaction> transactions = transactionRepository.findByAccountFrom(accountNumber);

        return transactions.stream().map(transaction ->
                TransactionResponse.builder()
                        .id(transaction.getId())
                        .transactionId(transaction.getTransactionId())
                        .accountFrom(transaction.getAccountFrom())
                        .accountTo(transaction.getAccountTo())
                        .amount(transaction.getAmount())
                        .type(transaction.getType())
                        .status(transaction.getStatus())
                        .createdAt(transaction.getCreatedAt())
                        .build()
        ).collect(Collectors.toList());
    }

    private void handleBalanceUpdate(TransactionRequest request, TransactionType type, Transaction tx) {
        UpdateBalanceResponse response = updateBalance(request, type.name());
        if (response.getStatus() >= 400) {
            tx.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(tx);
            throw new ResponseStatusException(HttpStatus.valueOf(response.getStatus()), response.getMessage());
        }
    }

    private UpdateBalanceResponse updateBalance(TransactionRequest request, String type) {
        return accountClient.updateBalance(new UpdateBalanceRequest(
                request.getAccountFrom(),
                request.getAmount(),
                type
        ));
    }
}
