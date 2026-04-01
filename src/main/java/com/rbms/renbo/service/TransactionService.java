package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.constant.PaymentStatusEnum;
import com.rbms.renbo.constant.TransactionStatusEnum;
import com.rbms.renbo.entity.Item;
import com.rbms.renbo.entity.Transactions;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.mapper.TransactionMapper;
import com.rbms.renbo.model.ItemResponseDto;
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
    private final userService userService;
    private final ItemService itemService;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionMapper transactionMapper,
                              userService userService,
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
        ItemResponseDto item = itemService.findById(requestDto.getItemId());
        
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
            requestDto.setDailyRate(item.getRate());
        }
        if (requestDto.getDepositAmount() == null) {
            requestDto.setDepositAmount(item.getDeposit());
        }

        Transactions transaction = transactionMapper.updateEntityFromRequestDto(requestDto);

        // Set the relationships
        transaction.setOwner(owner.get());
        transaction.setRenter(renter.get());
        Item itemEntity = new Item();
        itemEntity.setID(requestDto.getItemId());
        transaction.setItem(itemEntity);

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }

    public TransactionResponseDto findById(Long transactionId) {
        Optional<Transactions> transaction = transactionRepository.findById(transactionId);
        if (transaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }
        return transactionMapper.toDto(transaction.get());
    }

    public List<TransactionResponseDto> findByOwnerId(UUID ownerId) {
        List<Transactions> transactions = transactionRepository.findByOwnerId(ownerId);
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

    public List<TransactionResponseDto> findByItemId(UUID itemId) {
        List<Transactions> transactions = transactionRepository.findByItemId(itemId);
        return transactions.stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    public List<TransactionResponseDto> findByStatus(String status) {
        List<Transactions> transactions = transactionRepository.findByTransactionStatus(status);
        return transactions.stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    public TransactionResponseDto approveTransaction(Long transactionId, String ownerNote) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();
        transaction.setTransactionStatus(TransactionStatusEnum.APPROVED);
        transaction.setApprovedDate(LocalDateTime.now());
        transaction.setOwnerNote(ownerNote);

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
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

    public TransactionResponseDto cancelTransaction(Long transactionId, String reason) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();
        transaction.setTransactionStatus(TransactionStatusEnum.CANCELLED);
        transaction.setOwnerNote(reason);

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }

    public TransactionResponseDto markAsActive(Long transactionId) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();
        transaction.setTransactionStatus(TransactionStatusEnum.ACTIVE);

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }

    public TransactionResponseDto markAsCompleted(Long transactionId) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();
        transaction.setTransactionStatus(TransactionStatusEnum.COMPLETED);
        transaction.setReturnedDate(LocalDateTime.now());

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }

    public List<TransactionResponseDto> getAllTransactions() {
        List<Transactions> transactions = transactionRepository.findAll();
        return transactions.stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    public TransactionResponseDto completeTransaction(Long transactionId) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();
        
        // Validate that transaction is in ACTIVE status before completing
        if (transaction.getTransactionStatus() != TransactionStatusEnum.ACTIVE) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST);
        }

        transaction.setTransactionStatus(TransactionStatusEnum.COMPLETED);
        transaction.setReturnedDate(LocalDateTime.now());

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
    }

    public TransactionResponseDto recordPayment(Long transactionId, String paymentRef, PaymentStatusEnum paymentStatus) {
        Optional<Transactions> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            throw new ApiException(ErrorCodeEnum.RENTAL_NOT_FOUND);
        }

        Transactions transaction = optionalTransaction.get();
        transaction.setPaymentRef(paymentRef);
        transaction.setPaymentStatus(paymentStatus);
        
        // Set payment date if status is PAID
        if (paymentStatus == PaymentStatusEnum.PAID) {
            transaction.setPaymentDate(LocalDateTime.now());
        }

        Transactions savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDto(savedTransaction);
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
}
