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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionMapper transactionMapper;
    @Mock private UserService userService;
    @Mock private ItemService itemService;

    @InjectMocks
    private TransactionService transactionService;

    // ── shared fixtures ───────────────────────────────────────────────────────

    private UUID ownerId;
    private UUID renterId;
    private UUID outsiderId;
    private UUID itemId;
    private User owner;
    private User renter;
    private Item testItem;
    private ItemResponseDto itemResponse;

    @BeforeEach
    void setUp() {
        ownerId    = UUID.randomUUID();
        renterId   = UUID.randomUUID();
        outsiderId = UUID.randomUUID();
        itemId     = UUID.randomUUID();

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

        testItem = new Item();
        testItem.setID(itemId);
        testItem.setName("Excavator");
        testItem.setOwner(owner);
        testItem.setRate(new BigDecimal("125.50"));
        testItem.setDeposit(new BigDecimal("400.00"));

        itemResponse = new ItemResponseDto();
        itemResponse.setId(itemId);
        itemResponse.setName("Excavator");
        itemResponse.setDescription("Heavy-duty excavator");
        itemResponse.setRate(new BigDecimal("125.50"));
        itemResponse.setDeposit(new BigDecimal("400.00"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Builds a transaction wired to owner + renter with the given status. */
    private Transactions buildTx(TransactionStatusEnum status) {
        Transactions tx = new Transactions();
        tx.setOwner(owner);
        tx.setRenter(renter);
        tx.setItem(testItem);
        tx.setTransactionStatus(status);
        return tx;
    }

    /** Configures save + toDto to echo the transaction's status/paymentStatus back. */
    private void mockSaveAndMap() {
        when(transactionRepository.save(any(Transactions.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toDto(any(Transactions.class)))
                .thenAnswer(inv -> {
                    Transactions tx = inv.getArgument(0);
                    TransactionResponseDto dto = new TransactionResponseDto();
                    dto.setTransactionStatus(tx.getTransactionStatus());
                    dto.setPaymentStatus(tx.getPaymentStatus());
                    dto.setPaymentRef(tx.getPaymentRef());
                    dto.setOwnerNote(tx.getOwnerNote());
                    dto.setRenterNote(tx.getRenterNote());
                    return dto;
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createTransaction  (existing tests — kept untouched)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTransaction")
    class CreateTransaction {

        @Test
        @DisplayName("snapshots item's daily rate and deposit when not provided in request")
        void snapshotsItemRateAndDeposit() {
            TransactionRequestDto request = buildCreateRequest();
            request.setOwnerId(null);
            request.setDailyRate(null);
            request.setDepositAmount(null);

            when(itemService.getItemEntityById(itemId)).thenReturn(testItem);
            when(itemService.getItemOwnerId(itemId)).thenReturn(ownerId);
            when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
            when(userService.getUserDetails(renterId)).thenReturn(Optional.of(renter));
            when(transactionMapper.updateEntityFromRequestDto(any(TransactionRequestDto.class)))
                    .thenAnswer(inv -> {
                        TransactionRequestDto dto = inv.getArgument(0);
                        Transactions tx = new Transactions();
                        tx.setDailyRate(dto.getDailyRate());
                        tx.setDepositAmount(dto.getDepositAmount());
                        return tx;
                    });
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionMapper.toDto(any())).thenAnswer(inv -> {
                Transactions tx = inv.getArgument(0);
                TransactionResponseDto dto = new TransactionResponseDto();
                dto.setDailyRate(tx.getDailyRate());
                dto.setDepositAmount(tx.getDepositAmount());
                return dto;
            });

            TransactionResponseDto response = transactionService.createTransaction(request);

            assertEquals(itemResponse.getRate(), response.getDailyRate());
            assertEquals(itemResponse.getDeposit(), response.getDepositAmount());
        }

        @Test
        @DisplayName("wires real item entity onto saved transaction (not a detached reference)")
        void loadsRealItemNotDetachedEntity() {
            TransactionRequestDto request = buildCreateRequest();

            when(itemService.getItemEntityById(itemId)).thenReturn(testItem);
            when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
            when(userService.getUserDetails(renterId)).thenReturn(Optional.of(renter));
            when(transactionMapper.updateEntityFromRequestDto(any())).thenReturn(new Transactions());
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<Transactions> captor = ArgumentCaptor.forClass(Transactions.class);
            when(transactionMapper.toDto(any())).thenAnswer(inv -> {
                Transactions tx = inv.getArgument(0);
                TransactionResponseDto dto = new TransactionResponseDto();
                dto.setItemName(tx.getItem() != null ? tx.getItem().getName() : null);
                return dto;
            });

            TransactionResponseDto response = transactionService.createTransaction(request);

            verify(transactionRepository).save(captor.capture());
            assertEquals("Excavator", captor.getValue().getItem().getName());
            assertEquals("Excavator", response.getItemName());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByOwnerId  (NEW)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByOwnerId")
    class FindByOwnerId {

        @Test
        @DisplayName("returns mapped DTOs for all of the owner's transactions")
        void returnsMappedTransactions() {
            Transactions tx1 = buildTx(TransactionStatusEnum.PENDING);
            Transactions tx2 = buildTx(TransactionStatusEnum.ACTIVE);

            TransactionResponseDto dto1 = new TransactionResponseDto();
            dto1.setTransactionStatus(TransactionStatusEnum.PENDING);
            TransactionResponseDto dto2 = new TransactionResponseDto();
            dto2.setTransactionStatus(TransactionStatusEnum.ACTIVE);

            when(transactionRepository.findByOwnerId(ownerId)).thenReturn(List.of(tx1, tx2));
            when(transactionMapper.toDto(tx1)).thenReturn(dto1);
            when(transactionMapper.toDto(tx2)).thenReturn(dto2);

            List<TransactionResponseDto> result = transactionService.findByOwnerId(ownerId);

            assertEquals(2, result.size());
            assertEquals(TransactionStatusEnum.PENDING, result.get(0).getTransactionStatus());
            assertEquals(TransactionStatusEnum.ACTIVE,  result.get(1).getTransactionStatus());
            verify(transactionRepository).findByOwnerId(ownerId);
        }

        @Test
        @DisplayName("returns empty list when owner has no transactions")
        void returnsEmptyList() {
            when(transactionRepository.findByOwnerId(ownerId)).thenReturn(List.of());

            List<TransactionResponseDto> result = transactionService.findByOwnerId(ownerId);

            assertTrue(result.isEmpty());
            verifyNoInteractions(transactionMapper);
        }

        @Test
        @DisplayName("queries only by ownerId — never calls findAll")
        void queriesOnlyByOwnerId() {
            when(transactionRepository.findByOwnerId(ownerId)).thenReturn(List.of());

            transactionService.findByOwnerId(ownerId);

            verify(transactionRepository).findByOwnerId(ownerId);
            verify(transactionRepository, never()).findAll();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByRenterId  (NEW)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByRenterId")
    class FindByRenterId {

        @Test
        @DisplayName("returns mapped DTOs for all of the renter's transactions")
        void returnsMappedTransactions() {
            Transactions tx = buildTx(TransactionStatusEnum.COMPLETED);

            TransactionResponseDto dto = new TransactionResponseDto();
            dto.setTransactionStatus(TransactionStatusEnum.COMPLETED);

            when(transactionRepository.findByRenterId(renterId)).thenReturn(List.of(tx));
            when(transactionMapper.toDto(tx)).thenReturn(dto);

            List<TransactionResponseDto> result = transactionService.findByRenterId(renterId);

            assertEquals(1, result.size());
            assertEquals(TransactionStatusEnum.COMPLETED, result.get(0).getTransactionStatus());
            verify(transactionRepository).findByRenterId(renterId);
        }

        @Test
        @DisplayName("returns empty list when renter has no transactions")
        void returnsEmptyList() {
            when(transactionRepository.findByRenterId(renterId)).thenReturn(List.of());

            List<TransactionResponseDto> result = transactionService.findByRenterId(renterId);

            assertTrue(result.isEmpty());
            verifyNoInteractions(transactionMapper);
        }

        @Test
        @DisplayName("queries only by renterId — never calls findAll")
        void queriesOnlyByRenterId() {
            when(transactionRepository.findByRenterId(renterId)).thenReturn(List.of());

            transactionService.findByRenterId(renterId);

            verify(transactionRepository).findByRenterId(renterId);
            verify(transactionRepository, never()).findAll();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllTransactions  (NEW)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllTransactions")
    class GetAllTransactions {

        @Test
        @DisplayName("returns all transactions across all users")
        void returnsAllTransactions() {
            Transactions tx1 = buildTx(TransactionStatusEnum.PENDING);
            Transactions tx2 = buildTx(TransactionStatusEnum.COMPLETED);

            TransactionResponseDto dto1 = new TransactionResponseDto();
            TransactionResponseDto dto2 = new TransactionResponseDto();

            when(transactionRepository.findAll()).thenReturn(List.of(tx1, tx2));
            when(transactionMapper.toDto(tx1)).thenReturn(dto1);
            when(transactionMapper.toDto(tx2)).thenReturn(dto2);

            List<TransactionResponseDto> result = transactionService.getAllTransactions();

            assertEquals(2, result.size());
            verify(transactionMapper, times(2)).toDto(any());
        }

        @Test
        @DisplayName("returns empty list when no transactions exist in the system")
        void returnsEmptyList() {
            when(transactionRepository.findAll()).thenReturn(List.of());

            List<TransactionResponseDto> result = transactionService.getAllTransactions();

            assertTrue(result.isEmpty());
            verifyNoInteractions(transactionMapper);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // approveTransactionWithOwnerValidation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approveTransaction")
    class ApproveTransaction {

        @Test
        @DisplayName("sets status to APPROVED, stores ownerNote, and saves")
        void approvesSuccessfully() {
            Transactions tx = buildTx(TransactionStatusEnum.PENDING);
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
            mockSaveAndMap();

            TransactionResponseDto result =
                    transactionService.approveTransactionWithOwnerValidation(1L, ownerId, "Looks good!");

            assertEquals(TransactionStatusEnum.APPROVED, tx.getTransactionStatus());
            assertEquals("Looks good!", tx.getOwnerNote());
            assertNotNull(tx.getApprovedDate());
            assertEquals(TransactionStatusEnum.APPROVED, result.getTransactionStatus());
            verify(transactionRepository).save(tx);
        }

        @Test
        @DisplayName("throws RENTAL_NOT_FOUND when transaction does not exist")
        void throwsWhenTransactionNotFound() {
            when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.approveTransactionWithOwnerValidation(99L, ownerId, "note"));

            assertEquals(ErrorCodeEnum.RENTAL_NOT_FOUND, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UNAUTHORIZED when caller is not the item owner")
        void throwsWhenCallerIsNotOwner() {
            Transactions tx = buildTx(TransactionStatusEnum.PENDING);
            when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.approveTransactionWithOwnerValidation(1L, outsiderId, "note"));

            assertEquals(ErrorCodeEnum.UNAUTHORIZED, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // activateTransactionWithOwnerValidation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("activateTransaction")
    class ActivateTransaction {

        @Test
        @DisplayName("sets status to ACTIVE and marks item as RENTED")
        void activatesAndSetsItemRented() {
            Transactions tx = buildTx(TransactionStatusEnum.APPROVED);
            when(transactionRepository.findById(2L)).thenReturn(Optional.of(tx));
            mockSaveAndMap();

            TransactionResponseDto result =
                    transactionService.activateTransactionWithOwnerValidation(2L, ownerId);

            assertEquals(TransactionStatusEnum.ACTIVE, result.getTransactionStatus());
            verify(itemService).updateItemAvailability(itemId, "RENTED");
            verify(transactionRepository).save(tx);
        }

        @Test
        @DisplayName("throws RENTAL_NOT_FOUND when transaction does not exist")
        void throwsWhenTransactionNotFound() {
            when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.activateTransactionWithOwnerValidation(99L, ownerId));

            assertEquals(ErrorCodeEnum.RENTAL_NOT_FOUND, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UNAUTHORIZED when caller is not the item owner")
        void throwsWhenCallerIsNotOwner() {
            Transactions tx = buildTx(TransactionStatusEnum.APPROVED);
            when(transactionRepository.findById(2L)).thenReturn(Optional.of(tx));

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.activateTransactionWithOwnerValidation(2L, outsiderId));

            assertEquals(ErrorCodeEnum.UNAUTHORIZED, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BAD_REQUEST when transaction is not in APPROVED status")
        void throwsWhenStatusIsNotApproved() {
            // PENDING cannot be activated — must be APPROVED first
            Transactions tx = buildTx(TransactionStatusEnum.PENDING);
            when(transactionRepository.findById(2L)).thenReturn(Optional.of(tx));

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.activateTransactionWithOwnerValidation(2L, ownerId));

            assertEquals(ErrorCodeEnum.BAD_REQUEST, ex.getErrorCode());
            verify(itemService, never()).updateItemAvailability(any(), any());
            verify(transactionRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // completeTransactionWithOwnerValidation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("completeTransaction")
    class CompleteTransaction {

        @Test
        @DisplayName("sets status to COMPLETED and marks item as AVAILABLE")
        void completesAndSetsItemAvailable() {
            Transactions tx = buildTx(TransactionStatusEnum.ACTIVE);
            when(transactionRepository.findById(3L)).thenReturn(Optional.of(tx));
            mockSaveAndMap();

            TransactionResponseDto result =
                    transactionService.completeTransactionWithOwnerValidation(3L, ownerId);

            assertEquals(TransactionStatusEnum.COMPLETED, result.getTransactionStatus());
            assertNotNull(tx.getReturnedDate());
            verify(itemService).updateItemAvailability(itemId, "AVAILABLE");
            verify(transactionRepository).save(tx);
        }

        @Test
        @DisplayName("throws RENTAL_NOT_FOUND when transaction does not exist")
        void throwsWhenTransactionNotFound() {
            when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.completeTransactionWithOwnerValidation(99L, ownerId));

            assertEquals(ErrorCodeEnum.RENTAL_NOT_FOUND, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UNAUTHORIZED when caller is not the item owner")
        void throwsWhenCallerIsNotOwner() {
            Transactions tx = buildTx(TransactionStatusEnum.ACTIVE);
            when(transactionRepository.findById(3L)).thenReturn(Optional.of(tx));

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.completeTransactionWithOwnerValidation(3L, outsiderId));

            assertEquals(ErrorCodeEnum.UNAUTHORIZED, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BAD_REQUEST when transaction is not in ACTIVE status")
        void throwsWhenStatusIsNotActive() {
            // APPROVED cannot be completed — must be ACTIVE first
            Transactions tx = buildTx(TransactionStatusEnum.APPROVED);
            when(transactionRepository.findById(3L)).thenReturn(Optional.of(tx));

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.completeTransactionWithOwnerValidation(3L, ownerId));

            assertEquals(ErrorCodeEnum.BAD_REQUEST, ex.getErrorCode());
            verify(itemService, never()).updateItemAvailability(any(), any());
            verify(transactionRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // recordPaymentWithOwnerValidation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordPayment")
    class RecordPayment {

        @Test
        @DisplayName("renter can record their own payment")
        void renterCanRecordPayment() {
            Transactions tx = buildTx(TransactionStatusEnum.ACTIVE);
            tx.setPaymentStatus(PaymentStatusEnum.UNPAID);
            when(transactionRepository.findById(4L)).thenReturn(Optional.of(tx));
            mockSaveAndMap();

            TransactionResponseDto result =
                    transactionService.recordPaymentWithOwnerValidation(4L, renterId, "TNG-123");

            assertEquals(PaymentStatusEnum.PAID, result.getPaymentStatus());
            assertEquals("TNG-123", result.getPaymentRef());
            assertNotNull(tx.getPaymentDate());
        }

        @Test
        @DisplayName("owner can also record payment on behalf of transaction")
        void ownerCanAlsoRecordPayment() {
            Transactions tx = buildTx(TransactionStatusEnum.ACTIVE);
            tx.setPaymentStatus(PaymentStatusEnum.UNPAID);
            when(transactionRepository.findById(4L)).thenReturn(Optional.of(tx));
            mockSaveAndMap();

            TransactionResponseDto result =
                    transactionService.recordPaymentWithOwnerValidation(4L, ownerId, "BANK-456");

            assertEquals(PaymentStatusEnum.PAID, result.getPaymentStatus());
            assertEquals("BANK-456", result.getPaymentRef());
        }

        @Test
        @DisplayName("throws UNAUTHORIZED when caller is neither owner nor renter")
        void throwsWhenCallerIsOutsider() {
            Transactions tx = buildTx(TransactionStatusEnum.ACTIVE);
            when(transactionRepository.findById(4L)).thenReturn(Optional.of(tx));

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.recordPaymentWithOwnerValidation(4L, outsiderId, "REF"));

            assertEquals(ErrorCodeEnum.UNAUTHORIZED, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws RENTAL_NOT_FOUND when transaction does not exist")
        void throwsWhenTransactionNotFound() {
            when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.recordPaymentWithOwnerValidation(99L, renterId, "REF"));

            assertEquals(ErrorCodeEnum.RENTAL_NOT_FOUND, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // rejectTransactionWithOwnerValidation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectTransaction")
    class RejectTransaction {

        @Test
        @DisplayName("sets status to CANCELLED and stores ownerNote when request is PENDING")
        void rejectsSuccessfully() {
            Transactions tx = buildTx(TransactionStatusEnum.PENDING);
            when(transactionRepository.findById(8L)).thenReturn(Optional.of(tx));
            mockSaveAndMap();

            TransactionResponseDto result =
                    transactionService.rejectTransactionWithOwnerValidation(8L, ownerId, "Not available");

            assertEquals(TransactionStatusEnum.CANCELLED, tx.getTransactionStatus());
            assertEquals("Not available", tx.getOwnerNote());
            assertEquals(TransactionStatusEnum.CANCELLED, result.getTransactionStatus());
            verify(transactionRepository).save(tx);
        }

        @Test
        @DisplayName("throws BAD_REQUEST when transaction is not in PENDING status")
        void throwsWhenStatusIsNotPending() {
            Transactions tx = buildTx(TransactionStatusEnum.APPROVED);
            when(transactionRepository.findById(8L)).thenReturn(Optional.of(tx));

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.rejectTransactionWithOwnerValidation(8L, ownerId, "too late"));

            assertEquals(ErrorCodeEnum.BAD_REQUEST, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UNAUTHORIZED when caller is not the item owner")
        void throwsWhenCallerIsNotOwner() {
            Transactions tx = buildTx(TransactionStatusEnum.PENDING);
            when(transactionRepository.findById(8L)).thenReturn(Optional.of(tx));

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.rejectTransactionWithOwnerValidation(8L, outsiderId, "note"));

            assertEquals(ErrorCodeEnum.UNAUTHORIZED, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws RENTAL_NOT_FOUND when transaction does not exist")
        void throwsWhenTransactionNotFound() {
            when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.rejectTransactionWithOwnerValidation(99L, ownerId, "note"));

            assertEquals(ErrorCodeEnum.RENTAL_NOT_FOUND, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // cancelTransactionWithValidation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelTransaction")
    class CancelTransaction {

        @Test
        @DisplayName("renter can cancel a PENDING transaction")
        void renterCancelsPending() {
            Transactions tx = buildTx(TransactionStatusEnum.PENDING);
            when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));
            mockSaveAndMap();

            TransactionResponseDto result =
                    transactionService.cancelTransactionWithValidation(5L, renterId, "changed plans");

            assertEquals(TransactionStatusEnum.CANCELLED, result.getTransactionStatus());
            assertEquals("changed plans", tx.getRenterNote());
            verify(itemService, never()).updateItemAvailability(any(), any());
        }

        @Test
        @DisplayName("renter cannot cancel a non-PENDING transaction")
        void renterCannotCancelApproved() {
            Transactions tx = buildTx(TransactionStatusEnum.APPROVED);
            when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.cancelTransactionWithValidation(5L, renterId, "changed plans"));

            assertEquals(ErrorCodeEnum.BAD_REQUEST, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("owner can cancel a PENDING transaction")
        void ownerCancelsPending() {
            Transactions tx = buildTx(TransactionStatusEnum.PENDING);
            when(transactionRepository.findById(6L)).thenReturn(Optional.of(tx));
            mockSaveAndMap();

            TransactionResponseDto result =
                    transactionService.cancelTransactionWithValidation(6L, ownerId, "owner cancels");

            assertEquals(TransactionStatusEnum.CANCELLED, result.getTransactionStatus());
            assertEquals("owner cancels", tx.getOwnerNote());
        }

        @Test
        @DisplayName("owner can cancel an APPROVED transaction")
        void ownerCancelsApproved() {
            Transactions tx = buildTx(TransactionStatusEnum.APPROVED);
            when(transactionRepository.findById(7L)).thenReturn(Optional.of(tx));
            mockSaveAndMap();

            TransactionResponseDto result =
                    transactionService.cancelTransactionWithValidation(7L, ownerId, "owner cancels approved");

            assertEquals(TransactionStatusEnum.CANCELLED, result.getTransactionStatus());
        }

        @Test
        @DisplayName("owner cancelling an ACTIVE transaction resets item availability to AVAILABLE")
        void ownerCancelsActiveResetsItemAvailability() {
            Transactions tx = buildTx(TransactionStatusEnum.ACTIVE);
            when(transactionRepository.findById(9L)).thenReturn(Optional.of(tx));
            mockSaveAndMap();

            TransactionResponseDto result =
                    transactionService.cancelTransactionWithValidation(9L, ownerId, "emergency cancel");

            assertEquals(TransactionStatusEnum.CANCELLED, result.getTransactionStatus());
            verify(itemService).updateItemAvailability(itemId, "AVAILABLE");
        }

        @Test
        @DisplayName("throws UNAUTHORIZED when caller is neither owner nor renter")
        void throwsWhenCallerIsOutsider() {
            Transactions tx = buildTx(TransactionStatusEnum.PENDING);
            when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.cancelTransactionWithValidation(5L, outsiderId, "hack"));

            assertEquals(ErrorCodeEnum.UNAUTHORIZED, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws RENTAL_NOT_FOUND when transaction does not exist")
        void throwsWhenTransactionNotFound() {
            when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> transactionService.cancelTransactionWithValidation(99L, ownerId, "note"));

            assertEquals(ErrorCodeEnum.RENTAL_NOT_FOUND, ex.getErrorCode());
            verify(transactionRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helpers
    // ─────────────────────────────────────────────────────────────────────────

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