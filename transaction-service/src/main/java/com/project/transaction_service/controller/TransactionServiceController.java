package com.project.transaction_service.controller;

import com.project.transaction_service.dto.request.TransactionRequest;
import com.project.transaction_service.dto.response.Base;
import com.project.transaction_service.dto.response.TransactionResponse;
import com.project.transaction_service.entity.Transaction;
import com.project.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transaction")
@RequiredArgsConstructor
public class TransactionServiceController {
    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public Base<Transaction> deposit(@RequestBody TransactionRequest request) {
        Transaction tx = transactionService.deposit(request);
        return Base.<Transaction>builder()
                .data(tx)
                .status(HttpStatus.OK.value())
                .build();
    }

    @PostMapping("/withdraw")
    public Base<Transaction> withdraw(@RequestBody TransactionRequest request) {
        Transaction tx = transactionService.withdrawal(request);
        return Base.<Transaction>builder()
                .data(tx)
                .status(HttpStatus.OK.value())
                .build();
    }

    @PostMapping("/transfer")
    public Base<Transaction> transfer(@RequestBody TransactionRequest request) {
        Transaction tx = transactionService.transfer(request);
        return Base.<Transaction>builder()
                .data(tx)
                .status(HttpStatus.OK.value())
                .build();
    }

    @GetMapping("/{accountNumber}")
    public Base<List<TransactionResponse>> getTransactions(@PathVariable String accountNumber) {
        return Base.<List<TransactionResponse>>builder()
                .data(transactionService.getTransaction(accountNumber))
                .status(HttpStatus.OK.value())
                .build();
    }
}
