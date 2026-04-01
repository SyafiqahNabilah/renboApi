package com.rbms.renbo.controller;

import com.rbms.renbo.model.TransactionResponseDto;
import com.rbms.renbo.service.TransactionService;
import com.rbms.renbo.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/transaction", produces = "application/json")
public class TransactionController {

    private final TransactionService transactionService;
    private final JwtUtil jwtUtil;

    public TransactionController(TransactionService transactionService, JwtUtil jwtUtil) {
        this.transactionService = transactionService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/my-transactions")
    public List<TransactionResponseDto> getMyTransactions(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String status) {

        // Extract userId from JWT token
        // For now, this is a placeholder - in production you'd validate the token
        String token = authHeader != null && authHeader.startsWith("Bearer ") ?
                      authHeader.substring(7) : null;

        if (token == null) {
            throw new RuntimeException("Authorization token required");
        }

        // Extract userId from token (you'd validate the token first in production)
        String userIdStr = jwtUtil.extractUserId(token);
        if (userIdStr == null) {
            throw new RuntimeException("Invalid token");
        }

        UUID ownerId = UUID.fromString(userIdStr);

        if (status != null && !status.isEmpty()) {
            // Filter by status - for now, get all and filter client-side
            // In production, you'd want a repository method that filters by both ownerId and status
            return transactionService.findByOwnerId(ownerId).stream()
                    .filter(t -> t.getTransactionStatus().name().equalsIgnoreCase(status))
                    .toList();
        } else {
            return transactionService.findByOwnerId(ownerId);
        }
    }
}
