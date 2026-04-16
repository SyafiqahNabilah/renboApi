package com.rbms.renbo.controller;

import com.rbms.renbo.model.RentalRequestDto;
import com.rbms.renbo.model.TransactionRequestDto;
import com.rbms.renbo.model.ItemResponseDto;
import com.rbms.renbo.model.TransactionResponseDto;
import com.rbms.renbo.service.TransactionService;
import com.rbms.renbo.service.ItemService;
import com.rbms.renbo.util.JwtUtil;
import com.rbms.renbo.util.PaymentUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/transaction", produces = "application/json")
public class TransactionController {

    private final TransactionService transactionService;
    private final ItemService itemService;
    private final JwtUtil jwtUtil;
    private final PaymentUtil paymentUtil;

    public TransactionController(TransactionService transactionService, ItemService itemService, JwtUtil jwtUtil, PaymentUtil paymentUtil) {
        this.transactionService = transactionService;
        this.itemService = itemService;
        this.jwtUtil = jwtUtil;
        this.paymentUtil = paymentUtil;
    }

    @GetMapping("/all")
    public List<TransactionResponseDto> getAllTransactions(
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

        return transactionService.getAllTransactions();
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

    @GetMapping("/renter")
    public List<TransactionResponseDto> getMyRentalRequests(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Extract userId from JWT token
        String token = authHeader != null && authHeader.startsWith("Bearer ") ?
                      authHeader.substring(7) : null;

        if (token == null) {
            throw new RuntimeException("Authorization token required");
        }

        // Extract userId from token
        String userIdStr = jwtUtil.extractUserId(token);
        if (userIdStr == null) {
            throw new RuntimeException("Invalid token");
        }

        UUID renterId = UUID.fromString(userIdStr);

        return transactionService.findByRenterId(renterId);
    }

    @PostMapping("/request")
    public TransactionResponseDto submitRentalRequest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody RentalRequestDto rentalRequest) {

        // Extract renterId from JWT token
        String token = authHeader != null && authHeader.startsWith("Bearer ") ?
                      authHeader.substring(7) : null;

        if (token == null) {
            throw new RuntimeException("Authorization token required");
        }

        String renterIdStr = jwtUtil.extractUserId(token);
        if (renterIdStr == null) {
            throw new RuntimeException("Invalid token");
        }

        UUID renterId = UUID.fromString(renterIdStr);

        // Look up item to get owner and pricing information
        ItemResponseDto item = itemService.findById(rentalRequest.getItemId());

        // Create transaction request with all required data
        TransactionRequestDto transactionRequest = new TransactionRequestDto();
        transactionRequest.setItemId(rentalRequest.getItemId());
        transactionRequest.setRenterId(renterId);
        transactionRequest.setOwnerId(null); // Will be set by service based on item
        transactionRequest.setStartDate(rentalRequest.getStartDate());
        transactionRequest.setEndDate(rentalRequest.getEndDate());
        transactionRequest.setTransactionType(rentalRequest.getTransactionType());
        transactionRequest.setRenterNote(rentalRequest.getRenterNote());

        // Snapshot rates from item at time of request
        transactionRequest.setDailyRate(item.getRate());
        transactionRequest.setDepositAmount(item.getDeposit());

        return transactionService.createTransaction(transactionRequest);
    }
  
      @PutMapping("/{id}/approve")
    public TransactionResponseDto approveTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String ownerNote) {

        // Extract userId from JWT token
        String token = authHeader != null && authHeader.startsWith("Bearer ") ?
                      authHeader.substring(7) : null;

        if (token == null) {
            throw new RuntimeException("Authorization token required");
        }

        // Extract userId from token
        String userIdStr = jwtUtil.extractUserId(token);
        if (userIdStr == null) {
            throw new RuntimeException("Invalid token");
        }

        UUID loggedInUserId = UUID.fromString(userIdStr);

        // Verify that the logged-in user is the item owner and approve the transaction
        return transactionService.approveTransactionWithOwnerValidation(id, loggedInUserId, ownerNote);
    }

    @PutMapping("/{id}/activate")
    public TransactionResponseDto activateTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Extract userId from JWT token
        String token = authHeader != null && authHeader.startsWith("Bearer ") ?
                      authHeader.substring(7) : null;

        if (token == null) {
            throw new RuntimeException("Authorization token required");
        }

        // Extract userId from token
        String userIdStr = jwtUtil.extractUserId(token);
        if (userIdStr == null) {
            throw new RuntimeException("Invalid token");
        }

        UUID loggedInUserId = UUID.fromString(userIdStr);

        // Verify that the logged-in user is the item owner and activate the transaction
        return transactionService.activateTransactionWithOwnerValidation(id, loggedInUserId);
    }

    @PutMapping("/{id}/payment")
    public TransactionResponseDto recordPayment(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String paymentRef) {

        // Extract userId from JWT token
        String token = authHeader != null && authHeader.startsWith("Bearer ") ?
                      authHeader.substring(7) : null;

        if (token == null) {
            throw new RuntimeException("Authorization token required");
        }

        // Extract userId from token
        String userIdStr = jwtUtil.extractUserId(token);
        if (userIdStr == null) {
            throw new RuntimeException("Invalid token");
        }

        UUID loggedInUserId = UUID.fromString(userIdStr);

        // Generate payment reference if not provided
        if (paymentRef == null || paymentRef.trim().isEmpty()) {
            paymentRef = paymentUtil.generatePaymentReference();
        }

        // Verify that the logged-in user is the item owner and record payment
        return transactionService.recordPaymentWithOwnerValidation(id, loggedInUserId, paymentRef);
    }
    @PutMapping("/{id}/complete")
    public TransactionResponseDto completeTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Extract userId from JWT token
        String token = authHeader != null && authHeader.startsWith("Bearer ") ?
                      authHeader.substring(7) : null;

        if (token == null) {
            throw new RuntimeException("Authorization token required");
        }

        // Extract userId from token
        String userIdStr = jwtUtil.extractUserId(token);
        if (userIdStr == null) {
            throw new RuntimeException("Invalid token");
        }

        UUID loggedInUserId = UUID.fromString(userIdStr);

        // Verify that the logged-in user is the item owner and complete the transaction
        return transactionService.completeTransactionWithOwnerValidation(id, loggedInUserId);
    }
    @PutMapping("/{id}/reject")
    public TransactionResponseDto rejectTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String ownerNote) {

        // Extract userId from JWT token
        String token = authHeader != null && authHeader.startsWith("Bearer ") ?
                      authHeader.substring(7) : null;

        if (token == null) {
            throw new RuntimeException("Authorization token required");
        }

        // Extract userId from token
        String userIdStr = jwtUtil.extractUserId(token);
        if (userIdStr == null) {
            throw new RuntimeException("Invalid token");
        }

        UUID loggedInUserId = UUID.fromString(userIdStr);

        // Verify that the logged-in user is the item owner and reject the transaction
        return transactionService.rejectTransactionWithOwnerValidation(id, loggedInUserId, ownerNote);
    }

    @PutMapping("/{id}/cancel")
    public TransactionResponseDto cancelTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String note) {

        // Extract userId from JWT token
        String token = authHeader != null && authHeader.startsWith("Bearer ") ?
                      authHeader.substring(7) : null;

        if (token == null) {
            throw new RuntimeException("Authorization token required");
        }

        // Extract userId from token
        String userIdStr = jwtUtil.extractUserId(token);
        if (userIdStr == null) {
            throw new RuntimeException("Invalid token");
        }

        UUID loggedInUserId = UUID.fromString(userIdStr);

        // Verify that the logged-in user is the renter or owner and cancel the transaction
        return transactionService.cancelTransactionWithValidation(id, loggedInUserId, note);
    }
}
