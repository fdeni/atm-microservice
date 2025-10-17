package com.project.transaction_service.service;

import com.project.transaction_service.dto.request.TransactionRequest;
import com.project.transaction_service.dto.response.TransactionResponse;
import com.project.transaction_service.entity.Transaction;

import java.util.List;

public interface TransactionService {
    Transaction deposit(TransactionRequest transaction);
    Transaction withdrawal(TransactionRequest transaction);
    Transaction transfer(TransactionRequest transaction);
    List<TransactionResponse> getTransaction(String accountNumber);
}
