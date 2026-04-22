package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.constant.PaymentStatusEnum;
import com.rbms.renbo.constant.TransactionStatusEnum;
import com.rbms.renbo.entity.Item;
import com.rbms.renbo.entity.Transactions;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.mapper.TransactionMapper;
import com.rbms.renbo.model.TransactionRequestDto;
import com.rbms.renbo.model.TransactionResponseDto;
import com.rbms.renbo.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final UserService userService;
    private final ItemService itemService;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionMapper transactionMapper,
                              UserService userService,
                              ItemService itemService) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.userService = userService;
        this.itemService = itemService;
    }

    public TransactionResponseDto createTransaction(TransactionRequestDto requestDto) {
        log.debug("Creating transaction for item {} by renter {}",
                requestDto.getItemId(), requestDto.getRenterId());

        // Get item details first to determine owner if not provided
        Item itemEntity = itemService.getItemEntityById(requestDto.getItemId());

        // Determine ownerId - either from request or from item
        UUID ownerId = requestDto.getOwnerId();
        if (ownerId == null) {
            // Look up owner from item
            ownerId = itemService.getItemOwnerId(requestDto.getItemId());
        }

        // Validate that owner, renter exist
        Optional<User> owner = userService.getUserDetails(ownerId);
        if (owner.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        Optional<User> renter = userService.getUserDetails(requestDto.getRenterId());
        if (renter.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.USER_NOT_FOUND);
        }

        // Get item details to populate rates if not provided
        if (requestDto.getDailyRate() == null) {
            requestDto.setDailyRate(itemEntity.getRate());
        }
        if (requestDto.getDepositAmount() == null) {
            requestDto.setDepositAmount(itemEntity.getDeposit());
        }

        Transactions transaction = transactionMapper.updateEntityFromRequestDto(requestDto);

        // Set the relationships
        transaction.setOwner(owner.get());
        transaction.setRenter(renter.get());
        transaction.setItem(itemEntity);

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }

    public List<TransactionResponseDto> findByOwnerId(UUID ownerId) {
        List<Transactions> transactions = transactionRepository.findByOwnerId(ownerId);
        log.debug("find transactions for ownerId: {}", ownerId);
        return transactions.stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    public List<TransactionResponseDto> findByRenterId(UUID renterId) {
        List<Transactions> transactions = transactionRepository.findByRenterId(renterId);
        return transactions.stream()
                .map(transactionMapper::toDto)
                .toList();
    }


    public TransactionResponseDto approveTransactionWithOwnerValidation(Long transactionId, UUID loggedInUserId, String ownerNote) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();

        // Verify that the logged-in user is the item owner
        if (transaction.getOwner() == null || !transaction.getOwner().getUserID().equals(loggedInUserId)) {
            throw new ApiException(ErrorCodeEnum.UNAUTHORIZED);
        }

        transaction.setTransactionStatus(TransactionStatusEnum.APPROVED);
        transaction.setApprovedDate(LocalDateTime.now());
        transaction.setOwnerNote(ownerNote);

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }

    public List<TransactionResponseDto> getAllTransactions() {
        return transactionRepository.findAll()
                .stream().map(transactionMapper::toDto).toList();
    }

    public TransactionResponseDto activateTransactionWithOwnerValidation(Long transactionId, UUID loggedInUserId) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();

        // Verify that the logged-in user is the item owner
        if (transaction.getOwner() == null || !transaction.getOwner().getUserID().equals(loggedInUserId)) {
            throw new ApiException(ErrorCodeEnum.UNAUTHORIZED);
        }

        // Verify transaction is in APPROVED status
        if (transaction.getTransactionStatus() != TransactionStatusEnum.APPROVED) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST);
        }

        transaction.setTransactionStatus(TransactionStatusEnum.ACTIVE);

        // Update item availability to RENTED
        if (transaction.getItem() != null) {
            itemService.updateItemAvailability(transaction.getItem().getID(), "RENTED");
        }

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }

    public TransactionResponseDto recordPaymentWithOwnerValidation(Long transactionId, UUID loggedInUserId, String paymentRef) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();

        // FIX: payment is recorded by the RENTER (not owner). Allow either party for flexibility.
        boolean isOwner  = transaction.getOwner()  != null && transaction.getOwner().getUserID().equals(loggedInUserId);
        boolean isRenter = transaction.getRenter() != null && transaction.getRenter().getUserID().equals(loggedInUserId);

        if (!isOwner && !isRenter) {
            throw new ApiException(ErrorCodeEnum.UNAUTHORIZED);
        }

        transaction.setPaymentRef(paymentRef);
        transaction.setPaymentStatus(PaymentStatusEnum.PAID);
        transaction.setPaymentDate(LocalDateTime.now());

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }
    public TransactionResponseDto completeTransactionWithOwnerValidation(Long transactionId, UUID loggedInUserId) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();

        // Verify that the logged-in user is the item owner
        if (transaction.getOwner() == null || !transaction.getOwner().getUserID().equals(loggedInUserId)) {
            throw new ApiException(ErrorCodeEnum.UNAUTHORIZED);
        }

        // Verify transaction is in ACTIVE status
        if (transaction.getTransactionStatus() != TransactionStatusEnum.ACTIVE) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST);
        }

        transaction.setTransactionStatus(TransactionStatusEnum.COMPLETED);
        transaction.setReturnedDate(LocalDateTime.now());

        // Update item availability back to AVAILABLE
        if (transaction.getItem() != null) {
            itemService.updateItemAvailability(transaction.getItem().getID(), "AVAILABLE");
        }

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }
  
    public TransactionResponseDto rejectTransactionWithOwnerValidation(Long transactionId, UUID loggedInUserId, String ownerNote) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();

        // Verify that the logged-in user is the item owner
        if (transaction.getOwner() == null || !transaction.getOwner().getUserID().equals(loggedInUserId)) {
            throw new ApiException(ErrorCodeEnum.UNAUTHORIZED);
        }

        // Verify transaction is in PENDING status
        if (transaction.getTransactionStatus() != TransactionStatusEnum.PENDING) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST);
        }

        transaction.setTransactionStatus(TransactionStatusEnum.CANCELLED);
        transaction.setOwnerNote(ownerNote);

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }

    public TransactionResponseDto cancelTransactionWithValidation(Long transactionId, UUID loggedInUserId, String note) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();

        boolean isOwner = transaction.getOwner() != null && transaction.getOwner().getUserID().equals(loggedInUserId);
        boolean isRenter = transaction.getRenter() != null && transaction.getRenter().getUserID().equals(loggedInUserId);

        if (!isOwner && !isRenter) {
            throw new ApiException(ErrorCodeEnum.UNAUTHORIZED);
        }

        TransactionStatusEnum status = transaction.getTransactionStatus();

        if (isRenter && status != TransactionStatusEnum.PENDING) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST);
        }

        if (isOwner && status != TransactionStatusEnum.PENDING && status != TransactionStatusEnum.APPROVED && status != TransactionStatusEnum.ACTIVE) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST);
        }

        // If status was ACTIVE, item was rented, set back to AVAILABLE
        if (status == TransactionStatusEnum.ACTIVE && transaction.getItem() != null) {
            itemService.updateItemAvailability(transaction.getItem().getID(), "AVAILABLE");
        }

        transaction.setTransactionStatus(TransactionStatusEnum.CANCELLED);

        // Set the note based on who is canceling
        if (isRenter) {
            transaction.setRenterNote(note);
        } else {
            transaction.setOwnerNote(note);
        }

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }
}
