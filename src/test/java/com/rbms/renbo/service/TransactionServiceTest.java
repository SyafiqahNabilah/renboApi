package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.constant.PaymentStatusEnum;
import com.rbms.renbo.constant.TransactionStatusEnum;
import com.rbms.renbo.constant.TransactionTypeEnum;
import com.rbms.renbo.entity.Item;
import com.rbms.renbo.entity.Transactions;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.mapper.TransactionMapper;
import com.rbms.renbo.model.ItemResponseDto;
import com.rbms.renbo.model.TransactionRequestDto;
import com.rbms.renbo.model.TransactionResponseDto;
import com.rbms.renbo.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private UserService userService;

    @Mock
    private ItemService itemService;

    @InjectMocks
    private TransactionService transactionService;

    private UUID ownerId;
    private UUID renterId;
    private UUID outsiderId;
    private UUID itemId;
    private User owner;
    private User renter;
    private ItemResponseDto itemResponse;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        renterId = UUID.randomUUID();
        outsiderId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        owner = new User();
        owner.setUserID(ownerId);
        owner.setFirstName("Olivia");
        owner.setLastName("Owner");
        owner.setEmail("owner@example.com");

        renter = new User();
        renter.setUserID(renterId);
        renter.setFirstName("Ryan");
        renter.setLastName("Renter");
        renter.setEmail("renter@example.com");

        itemResponse = new ItemResponseDto();
        itemResponse.setId(itemId);
        itemResponse.setName("Excavator");
        itemResponse.setDescription("Heavy-duty excavator");
        itemResponse.setRate(new BigDecimal("125.50"));
        itemResponse.setDeposit(new BigDecimal("400.00"));
    }

    @Test
    void createTransaction_snapshotsItemRateAndDeposit() {
        TransactionRequestDto request = buildCreateRequest();
        request.setOwnerId(null);
        request.setDailyRate(null);
        request.setDepositAmount(null);
        Item itemEntity = new Item();
        itemEntity.setID(itemId);
        itemEntity.setName(itemResponse.getName());
        itemEntity.setDescription(itemResponse.getDescription());
        itemEntity.setOwner(owner);
        itemEntity.setRate(itemResponse.getRate());
        itemEntity.setDeposit(itemResponse.getDeposit());

        when(itemService.getItemEntityById(itemId)).thenReturn(itemEntity);
        when(itemService.getItemOwnerId(itemId)).thenReturn(ownerId);
        when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
        when(userService.getUserDetails(renterId)).thenReturn(Optional.of(renter));
        when(transactionMapper.updateEntityFromRequestDto(any(TransactionRequestDto.class))).thenAnswer(invocation -> {
            TransactionRequestDto dto = invocation.getArgument(0);
            Transactions transaction = new Transactions();
            transaction.setDailyRate(dto.getDailyRate());
            transaction.setDepositAmount(dto.getDepositAmount());
            transaction.setTransactionType(dto.getTransactionType());
            transaction.setStartDate(dto.getStartDate());
            transaction.setEndDate(dto.getEndDate());
            return transaction;
        });
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toDto(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions transaction = invocation.getArgument(0);
            TransactionResponseDto response = new TransactionResponseDto();
            response.setDailyRate(transaction.getDailyRate());
            response.setDepositAmount(transaction.getDepositAmount());
            return response;
        });

        TransactionResponseDto response = transactionService.createTransaction(request);

        assertEquals(itemResponse.getRate(), response.getDailyRate());
        assertEquals(itemResponse.getDeposit(), response.getDepositAmount());
        assertEquals(itemResponse.getRate(), request.getDailyRate());
        assertEquals(itemResponse.getDeposit(), request.getDepositAmount());
    }

    @Test
    void approveTransaction_rejectsWhenNotOwner() {
        Transactions transaction = new Transactions();
        transaction.setOwner(owner);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        ApiException exception = assertThrows(ApiException.class, () ->
                transactionService.approveTransactionWithOwnerValidation(1L, outsiderId, "approve")
        );

        assertEquals(ErrorCodeEnum.UNAUTHORIZED, exception.getErrorCode());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void activateTransaction_setsItemToRented() {
        Transactions transaction = new Transactions();
        transaction.setOwner(owner);
        transaction.setTransactionStatus(TransactionStatusEnum.APPROVED);
        Item item = new Item();
        item.setID(itemId);
        transaction.setItem(item);

        when(transactionRepository.findById(2L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toDto(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions saved = invocation.getArgument(0);
            TransactionResponseDto response = new TransactionResponseDto();
            response.setTransactionStatus(saved.getTransactionStatus());
            return response;
        });

        TransactionResponseDto response = transactionService.activateTransactionWithOwnerValidation(2L, ownerId);

        assertEquals(TransactionStatusEnum.ACTIVE, response.getTransactionStatus());
        verify(itemService).updateItemAvailability(itemId, "RENTED");
    }

    @Test
    void completeTransaction_setsItemBackToAvailable() {
        Transactions transaction = new Transactions();
        transaction.setOwner(owner);
        transaction.setTransactionStatus(TransactionStatusEnum.ACTIVE);
        Item item = new Item();
        item.setID(itemId);
        transaction.setItem(item);

        when(transactionRepository.findById(3L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toDto(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions saved = invocation.getArgument(0);
            TransactionResponseDto response = new TransactionResponseDto();
            response.setTransactionStatus(saved.getTransactionStatus());
            return response;
        });

        TransactionResponseDto response = transactionService.completeTransactionWithOwnerValidation(3L, ownerId);

        assertEquals(TransactionStatusEnum.COMPLETED, response.getTransactionStatus());
        verify(itemService).updateItemAvailability(itemId, "AVAILABLE");
    }

    @Test
    void recordPayment_allowsRenterToPayNotJustOwner() {
        Transactions transaction = new Transactions();
        transaction.setOwner(owner);
        transaction.setRenter(renter);
        transaction.setPaymentStatus(PaymentStatusEnum.UNPAID);

        when(transactionRepository.findById(4L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toDto(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions saved = invocation.getArgument(0);
            TransactionResponseDto response = new TransactionResponseDto();
            response.setPaymentStatus(saved.getPaymentStatus());
            response.setPaymentRef(saved.getPaymentRef());
            return response;
        });

        TransactionResponseDto response =
                transactionService.recordPaymentWithOwnerValidation(4L, renterId, "TNG-123");

        assertEquals(PaymentStatusEnum.PAID, response.getPaymentStatus());
        assertEquals("TNG-123", response.getPaymentRef());
    }

    @Test
    void cancelTransaction_renterCanOnlyCancelPending() {
        Transactions transaction = new Transactions();
        transaction.setOwner(owner);
        transaction.setRenter(renter);
        transaction.setTransactionStatus(TransactionStatusEnum.APPROVED);

        when(transactionRepository.findById(5L)).thenReturn(Optional.of(transaction));

        ApiException exception = assertThrows(ApiException.class, () ->
                transactionService.cancelTransactionWithValidation(5L, renterId, "changed plans")
        );

        assertEquals(ErrorCodeEnum.BAD_REQUEST, exception.getErrorCode());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_loadsRealItemNotDetachedEntity() {
        TransactionRequestDto request = buildCreateRequest();
        Item itemEntity = new Item();
        itemEntity.setID(itemId);
        itemEntity.setName(itemResponse.getName());
        itemEntity.setDescription(itemResponse.getDescription());
        itemEntity.setOwner(owner);

        when(itemService.getItemEntityById(itemId)).thenReturn(itemEntity);
        when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
        when(userService.getUserDetails(renterId)).thenReturn(Optional.of(renter));
        when(transactionMapper.updateEntityFromRequestDto(any(TransactionRequestDto.class))).thenReturn(new Transactions());
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Transactions> transactionCaptor = ArgumentCaptor.forClass(Transactions.class);
        when(transactionMapper.toDto(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions saved = invocation.getArgument(0);
            TransactionResponseDto response = new TransactionResponseDto();
            response.setItemName(saved.getItem() != null ? saved.getItem().getName() : null);
            return response;
        });

        TransactionResponseDto response = transactionService.createTransaction(request);

        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals(itemResponse.getName(), transactionCaptor.getValue().getItem().getName());
        assertEquals(itemResponse.getName(), response.getItemName());
    }

    private TransactionRequestDto buildCreateRequest() {
        TransactionRequestDto request = new TransactionRequestDto();
        request.setOwnerId(ownerId);
        request.setRenterId(renterId);
        request.setItemId(itemId);
        request.setTransactionType(TransactionTypeEnum.RENT);
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setRenterNote("Need it for a project");
        return request;
    }
}
