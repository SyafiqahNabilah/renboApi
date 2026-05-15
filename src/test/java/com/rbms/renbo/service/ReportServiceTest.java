package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.constant.PaymentStatusEnum;
import com.rbms.renbo.constant.TransactionStatusEnum;
import com.rbms.renbo.constant.TransactionTypeEnum;
import com.rbms.renbo.constant.UserRoleEnum;
import com.rbms.renbo.constant.UserStatusEnum;
import com.rbms.renbo.entity.Item;
import com.rbms.renbo.entity.Transactions;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.model.AdminSystemReportDto;
import com.rbms.renbo.model.ItemEarningDto;
import com.rbms.renbo.model.OwnerEarningsReportDto;
import com.rbms.renbo.model.OwnerTransactionSummaryDto;
import com.rbms.renbo.repository.ItemRepository;
import com.rbms.renbo.repository.TransactionRepository;
import com.rbms.renbo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository        userRepository;
    @Mock private ItemRepository        itemRepository;

    @InjectMocks
    private ReportService reportService;

    // ── shared fixtures ───────────────────────────────────────────────────────

    private UUID   ownerId;
    private UUID   renterId;
    private UUID   itemAId;
    private UUID   itemBId;
    private User   owner;
    private User   renter;
    private Item   itemA;
    private Item   itemB;

    private static final LocalDate FROM = LocalDate.of(2025, 1, 1);
    private static final LocalDate TO   = LocalDate.of(2025, 12, 31);

    @BeforeEach
    void setUp() {
        ownerId  = UUID.randomUUID();
        renterId = UUID.randomUUID();
        itemAId  = UUID.randomUUID();
        itemBId  = UUID.randomUUID();

        owner = makeUser(ownerId, "Aisha", "Owner",  UserRoleEnum.OWNER);
        renter= makeUser(renterId,"Ryan",  "Renter", UserRoleEnum.RENTER);

        itemA = makeItem(itemAId, "Excavator", owner, BigDecimal.valueOf(125));
        itemB = makeItem(itemBId, "Ladder",    owner, BigDecimal.valueOf(30));
    }

    // ── factory helpers ───────────────────────────────────────────────────────

    private User makeUser(UUID id, String first, String last, UserRoleEnum role) {
        User u = new User();
        u.setUserID(id);
        u.setFirstName(first);
        u.setLastName(last);
        u.setRole(role);
        u.setStatus(UserStatusEnum.ACTIVE);
        u.setJoinedDate(new Timestamp(System.currentTimeMillis()));
        return u;
    }

    private Item makeItem(UUID id, String name, User itemOwner, BigDecimal rate) {
        Item i = new Item();
        i.setID(id);
        i.setName(name);
        i.setOwner(itemOwner);
        i.setRate(rate);
        return i;
    }

    private Transactions makeTx(Item item,
                                User txOwner,
                                User txRenter,
                                TransactionStatusEnum status,
                                PaymentStatusEnum payStatus,
                                BigDecimal total,
                                int days,
                                LocalDate start) {
        Transactions tx = new Transactions();
        tx.setItem(item);
        tx.setOwner(txOwner);
        tx.setRenter(txRenter);
        tx.setTransactionStatus(status);
        tx.setPaymentStatus(payStatus);
        tx.setTotalAmount(total);
        tx.setTotalDays(days);
        tx.setDailyRate(item.getRate());
        tx.setTransactionType(TransactionTypeEnum.RENT);
        tx.setStartDate(start);
        tx.setEndDate(start.plusDays(days));
        return tx;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ownerEarningsReport
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ownerEarningsReport")
    class OwnerEarningsReport {

        @Test
        @DisplayName("returns correct totals for a single completed+paid transaction")
        void singleTransactionEarnings() {
            Transactions tx = makeTx(itemA, owner, renter,
                    TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,
                    BigDecimal.valueOf(250), 2, FROM);

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findCompletedPaidByOwnerAndDateRange(ownerId, FROM, TO))
                    .thenReturn(List.of(tx));

            OwnerEarningsReportDto report = reportService.ownerEarningsReport(ownerId, FROM, TO);

            assertEquals(ownerId, report.getOwnerId());
            assertEquals("Aisha Owner", report.getOwnerName());
            assertEquals(0, BigDecimal.valueOf(250).compareTo(report.getTotalEarnings()));
            assertEquals(1, report.getCompletedTransactionCount());
            assertEquals(FROM, report.getFromDate());
            assertEquals(TO,   report.getToDate());
            assertNotNull(report.getGeneratedAt());
        }

        @Test
        @DisplayName("sums earnings across multiple transactions on the same item")
        void multipleTransactionsSameItem() {
            Transactions tx1 = makeTx(itemA, owner, renter,
                    TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,
                    BigDecimal.valueOf(250), 2, FROM);
            Transactions tx2 = makeTx(itemA, owner, renter,
                    TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,
                    BigDecimal.valueOf(375), 3, FROM.plusMonths(1));

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findCompletedPaidByOwnerAndDateRange(ownerId, FROM, TO))
                    .thenReturn(List.of(tx1, tx2));

            OwnerEarningsReportDto report = reportService.ownerEarningsReport(ownerId, FROM, TO);

            assertEquals(0, BigDecimal.valueOf(625).compareTo(report.getTotalEarnings()));
            assertEquals(2, report.getCompletedTransactionCount());
            assertEquals(1, report.getItemBreakdown().size()); // same item → 1 breakdown entry
            assertEquals(0, BigDecimal.valueOf(625)
                    .compareTo(report.getItemBreakdown().get(0).getTotalEarnings()));
            assertEquals(5, report.getItemBreakdown().get(0).getTotalDays());
        }

        @Test
        @DisplayName("produces a separate breakdown entry for each distinct item")
        void separateBreakdownPerItem() {
            Transactions txA = makeTx(itemA, owner, renter,
                    TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,
                    BigDecimal.valueOf(500), 4, FROM);
            Transactions txB = makeTx(itemB, owner, renter,
                    TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,
                    BigDecimal.valueOf(90), 3, FROM);

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findCompletedPaidByOwnerAndDateRange(ownerId, FROM, TO))
                    .thenReturn(List.of(txA, txB));

            OwnerEarningsReportDto report = reportService.ownerEarningsReport(ownerId, FROM, TO);

            assertEquals(2, report.getItemBreakdown().size());
            assertEquals(0, BigDecimal.valueOf(590).compareTo(report.getTotalEarnings()));
        }

        @Test
        @DisplayName("item breakdown is sorted by totalEarnings descending")
        void breakdownSortedByEarningsDescending() {
            Transactions txA = makeTx(itemA, owner, renter,
                    TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,
                    BigDecimal.valueOf(500), 4, FROM);
            Transactions txB = makeTx(itemB, owner, renter,
                    TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,
                    BigDecimal.valueOf(90), 3, FROM);

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findCompletedPaidByOwnerAndDateRange(ownerId, FROM, TO))
                    .thenReturn(List.of(txB, txA)); // note: intentionally reversed

            OwnerEarningsReportDto report = reportService.ownerEarningsReport(ownerId, FROM, TO);

            // First entry must be the higher-earning item regardless of input order
            List<ItemEarningDto> bd = report.getItemBreakdown();
            assertTrue(bd.get(0).getTotalEarnings()
                            .compareTo(bd.get(1).getTotalEarnings()) > 0,
                    "First breakdown entry should have higher earnings than second");
        }

        @Test
        @DisplayName("returns zero earnings and empty breakdown when no transactions match")
        void zeroEarningsWhenNoTransactions() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findCompletedPaidByOwnerAndDateRange(ownerId, FROM, TO))
                    .thenReturn(List.of());

            OwnerEarningsReportDto report = reportService.ownerEarningsReport(ownerId, FROM, TO);

            assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalEarnings()));
            assertEquals(0, report.getCompletedTransactionCount());
            assertTrue(report.getItemBreakdown().isEmpty());
        }

        @Test
        @DisplayName("handles a transaction with null totalAmount as zero contribution")
        void handlesNullTotalAmountAsZero() {
            Transactions tx = makeTx(itemA, owner, renter,
                    TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,
                    null, 2, FROM); // null totalAmount

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findCompletedPaidByOwnerAndDateRange(ownerId, FROM, TO))
                    .thenReturn(List.of(tx));

            OwnerEarningsReportDto report = reportService.ownerEarningsReport(ownerId, FROM, TO);

            assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalEarnings()));
            assertDoesNotThrow(() -> report.getTotalEarnings());
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND when owner does not exist")
        void throwsWhenOwnerNotFound() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> reportService.ownerEarningsReport(ownerId, FROM, TO));

            assertEquals(ErrorCodeEnum.USER_NOT_FOUND, ex.getErrorCode());
            verify(transactionRepository, never())
                    .findCompletedPaidByOwnerAndDateRange(any(), any(), any());
        }

        @Test
        @DisplayName("throws BAD_REQUEST when fromDate is after toDate")
        void throwsWhenFromDateAfterToDate() {
            LocalDate badFrom = TO.plusDays(1); // after TO

            ApiException ex = assertThrows(ApiException.class,
                    () -> reportService.ownerEarningsReport(ownerId, badFrom, TO));

            assertEquals(ErrorCodeEnum.BAD_REQUEST, ex.getErrorCode());
            verifyNoInteractions(userRepository, transactionRepository);
        }

        @Test
        @DisplayName("passes correct date range parameters to the repository")
        void passesCorrectDateRangeToRepo() {
            LocalDate from = LocalDate.of(2025, 3, 1);
            LocalDate to   = LocalDate.of(2025, 6, 30);

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findCompletedPaidByOwnerAndDateRange(ownerId, from, to))
                    .thenReturn(List.of());

            reportService.ownerEarningsReport(ownerId, from, to);

            verify(transactionRepository)
                    .findCompletedPaidByOwnerAndDateRange(ownerId, from, to);
        }

        @Test
        @DisplayName("generatedAt timestamp is recent (within 5 seconds of now)")
        void generatedAtIsRecent() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findCompletedPaidByOwnerAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());

            OwnerEarningsReportDto report = reportService.ownerEarningsReport(ownerId, FROM, TO);

            assertTrue(report.getGeneratedAt().isAfter(LocalDateTime.now().minusSeconds(5)));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ownerTransactionSummary
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ownerTransactionSummary")
    class OwnerTransactionSummary {

        @Test
        @DisplayName("counts each status correctly across a mixed transaction list")
        void countsEachStatusCorrectly() {
            List<Transactions> txList = List.of(
                    makeTx(itemA, owner, renter, TransactionStatusEnum.PENDING,   PaymentStatusEnum.UNPAID, BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.PENDING,   PaymentStatusEnum.UNPAID, BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.APPROVED,  PaymentStatusEnum.UNPAID, BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.ACTIVE,    PaymentStatusEnum.UNPAID, BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,   BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,   BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,   BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.CANCELLED, PaymentStatusEnum.UNPAID, BigDecimal.TEN, 1, FROM)
            );

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findAllByOwnerId(ownerId)).thenReturn(txList);

            OwnerTransactionSummaryDto summary = reportService.ownerTransactionSummary(ownerId);

            assertEquals(2, summary.getPending());
            assertEquals(1, summary.getApproved());
            assertEquals(1, summary.getActive());
            assertEquals(3, summary.getCompleted());
            assertEquals(1, summary.getCancelled());
            assertEquals(8, summary.getTotal());
        }

        @Test
        @DisplayName("total equals sum of all status counts")
        void totalEqualsStatusSum() {
            List<Transactions> txList = List.of(
                    makeTx(itemA, owner, renter, TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,   BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.CANCELLED, PaymentStatusEnum.UNPAID, BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.PENDING,   PaymentStatusEnum.UNPAID, BigDecimal.TEN, 1, FROM)
            );

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findAllByOwnerId(ownerId)).thenReturn(txList);

            OwnerTransactionSummaryDto summary = reportService.ownerTransactionSummary(ownerId);

            long statusSum = summary.getPending() + summary.getApproved()
                    + summary.getActive() + summary.getCompleted() + summary.getCancelled();
            assertEquals(statusSum, summary.getTotal());
        }

        @Test
        @DisplayName("returns all-zero counts when owner has no transactions")
        void allZerosWhenNoTransactions() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findAllByOwnerId(ownerId)).thenReturn(List.of());

            OwnerTransactionSummaryDto summary = reportService.ownerTransactionSummary(ownerId);

            assertEquals(0, summary.getPending());
            assertEquals(0, summary.getApproved());
            assertEquals(0, summary.getActive());
            assertEquals(0, summary.getCompleted());
            assertEquals(0, summary.getCancelled());
            assertEquals(0, summary.getTotal());
        }

        @Test
        @DisplayName("includes owner name from entity")
        void includesOwnerName() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findAllByOwnerId(ownerId)).thenReturn(List.of());

            OwnerTransactionSummaryDto summary = reportService.ownerTransactionSummary(ownerId);

            assertEquals("Aisha Owner", summary.getOwnerName());
            assertEquals(ownerId, summary.getOwnerId());
        }

        @Test
        @DisplayName("throws USER_NOT_FOUND when owner does not exist")
        void throwsWhenOwnerNotFound() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> reportService.ownerTransactionSummary(ownerId));

            assertEquals(ErrorCodeEnum.USER_NOT_FOUND, ex.getErrorCode());
            verify(transactionRepository, never()).findAllByOwnerId(any());
        }

        @Test
        @DisplayName("queries with findAllByOwnerId — not the generic findAll")
        void usesOwnerScopedQuery() {
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(transactionRepository.findAllByOwnerId(ownerId)).thenReturn(List.of());

            reportService.ownerTransactionSummary(ownerId);

            verify(transactionRepository).findAllByOwnerId(ownerId);
            verify(transactionRepository, never()).findAll();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // adminSystemReport
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("adminSystemReport")
    class AdminSystemReport {

        @Test
        @DisplayName("returns correct user counts broken down by role")
        void countsUsersByRole() {
            User owner2  = makeUser(UUID.randomUUID(), "Bob", "O", UserRoleEnum.OWNER);
            User renter2 = makeUser(UUID.randomUUID(), "Sam", "R", UserRoleEnum.RENTER);
            User admin   = makeUser(UUID.randomUUID(), "Ada", "A", UserRoleEnum.ADMIN);

            when(userRepository.findAll()).thenReturn(List.of(owner, renter, owner2, renter2, admin));
            when(itemRepository.count()).thenReturn(10L);
            when(transactionRepository.findAll()).thenReturn(List.of());
            when(transactionRepository.findAllCompletedAndPaid()).thenReturn(List.of());

            AdminSystemReportDto report = reportService.adminSystemReport();

            assertEquals(5, report.getTotalUsers());
            assertEquals(2, report.getTotalOwners());
            assertEquals(2, report.getTotalRenters());
            assertEquals(1, report.getTotalAdmins());
        }

        @Test
        @DisplayName("returns correct item count from repository")
        void returnsItemCount() {
            when(userRepository.findAll()).thenReturn(List.of());
            when(itemRepository.count()).thenReturn(42L);
            when(transactionRepository.findAll()).thenReturn(List.of());
            when(transactionRepository.findAllCompletedAndPaid()).thenReturn(List.of());

            AdminSystemReportDto report = reportService.adminSystemReport();

            assertEquals(42L, report.getTotalItems());
        }

        @Test
        @DisplayName("counts all transaction statuses correctly")
        void countsTransactionStatusesCorrectly() {
            List<Transactions> allTx = List.of(
                    makeTx(itemA, owner, renter, TransactionStatusEnum.PENDING,   PaymentStatusEnum.UNPAID, BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.APPROVED,  PaymentStatusEnum.UNPAID, BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.ACTIVE,    PaymentStatusEnum.UNPAID, BigDecimal.TEN, 1, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID,   BigDecimal.valueOf(250), 2, FROM),
                    makeTx(itemA, owner, renter, TransactionStatusEnum.CANCELLED, PaymentStatusEnum.UNPAID, BigDecimal.TEN, 1, FROM)
            );

            when(userRepository.findAll()).thenReturn(List.of());
            when(itemRepository.count()).thenReturn(0L);
            when(transactionRepository.findAll()).thenReturn(allTx);
            when(transactionRepository.findAllCompletedAndPaid()).thenReturn(List.of());

            AdminSystemReportDto report = reportService.adminSystemReport();

            assertEquals(5, report.getTotalTransactions());
            assertEquals(1, report.getPendingTransactions());
            assertEquals(1, report.getApprovedTransactions());
            assertEquals(1, report.getActiveTransactions());
            assertEquals(1, report.getCompletedTransactions());
            assertEquals(1, report.getCancelledTransactions());
        }

        @Test
        @DisplayName("sums total platform earnings from COMPLETED+PAID transactions only")
        void sumsPlatformEarningsFromPaidOnly() {
            List<Transactions> paidTx = List.of(
                    makeTx(itemA, owner, renter, TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID, BigDecimal.valueOf(500), 4, FROM),
                    makeTx(itemB, owner, renter, TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID, BigDecimal.valueOf(300), 10, FROM)
            );

            when(userRepository.findAll()).thenReturn(List.of());
            when(itemRepository.count()).thenReturn(0L);
            when(transactionRepository.findAll()).thenReturn(List.of());
            when(transactionRepository.findAllCompletedAndPaid()).thenReturn(paidTx);

            AdminSystemReportDto report = reportService.adminSystemReport();

            assertEquals(0, BigDecimal.valueOf(800).compareTo(report.getTotalPlatformEarnings()));
        }

        @Test
        @DisplayName("returns zero platform earnings when no paid transactions exist")
        void zeroPlatformEarningsWhenNoPaidTx() {
            when(userRepository.findAll()).thenReturn(List.of());
            when(itemRepository.count()).thenReturn(0L);
            when(transactionRepository.findAll()).thenReturn(List.of());
            when(transactionRepository.findAllCompletedAndPaid()).thenReturn(List.of());

            AdminSystemReportDto report = reportService.adminSystemReport();

            assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalPlatformEarnings()));
        }

        @Test
        @DisplayName("handles null totalAmount in paid transactions as zero contribution")
        void handlesNullTotalAmountInPaidTx() {
            Transactions tx = makeTx(itemA, owner, renter,
                    TransactionStatusEnum.COMPLETED, PaymentStatusEnum.PAID, null, 2, FROM);

            when(userRepository.findAll()).thenReturn(List.of());
            when(itemRepository.count()).thenReturn(0L);
            when(transactionRepository.findAll()).thenReturn(List.of());
            when(transactionRepository.findAllCompletedAndPaid()).thenReturn(List.of(tx));

            AdminSystemReportDto report = reportService.adminSystemReport();

            assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalPlatformEarnings()));
        }

        @Test
        @DisplayName("uses findAllCompletedAndPaid for earnings — not findAll")
        void usesEfficientQueryForEarnings() {
            when(userRepository.findAll()).thenReturn(List.of());
            when(itemRepository.count()).thenReturn(0L);
            when(transactionRepository.findAll()).thenReturn(List.of());
            when(transactionRepository.findAllCompletedAndPaid()).thenReturn(List.of());

            reportService.adminSystemReport();

            verify(transactionRepository).findAllCompletedAndPaid();
            verify(transactionRepository).findAll();
            // findAll is for status counts, findAllCompletedAndPaid for earnings
        }

        @Test
        @DisplayName("returns an empty but non-null report on a fresh platform with no data")
        void returnsNonNullReportOnEmptyPlatform() {
            when(userRepository.findAll()).thenReturn(List.of());
            when(itemRepository.count()).thenReturn(0L);
            when(transactionRepository.findAll()).thenReturn(List.of());
            when(transactionRepository.findAllCompletedAndPaid()).thenReturn(List.of());

            AdminSystemReportDto report = reportService.adminSystemReport();

            assertNotNull(report);
            assertEquals(0, report.getTotalUsers());
            assertEquals(0, report.getTotalItems());
            assertEquals(0, report.getTotalTransactions());
            assertEquals(0, BigDecimal.ZERO.compareTo(report.getTotalPlatformEarnings()));
            assertNotNull(report.getGeneratedAt());
        }

        @Test
        @DisplayName("generatedAt timestamp is recent (within 5 seconds of now)")
        void generatedAtIsRecent() {
            when(userRepository.findAll()).thenReturn(List.of());
            when(itemRepository.count()).thenReturn(0L);
            when(transactionRepository.findAll()).thenReturn(List.of());
            when(transactionRepository.findAllCompletedAndPaid()).thenReturn(List.of());

            AdminSystemReportDto report = reportService.adminSystemReport();

            assertTrue(report.getGeneratedAt().isAfter(LocalDateTime.now().minusSeconds(5)));
        }
    }
}