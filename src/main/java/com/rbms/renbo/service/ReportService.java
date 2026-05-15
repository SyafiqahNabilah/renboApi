package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.constant.TransactionStatusEnum;
import com.rbms.renbo.constant.UserRoleEnum;
import com.rbms.renbo.entity.Transactions;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.model.*;
import com.rbms.renbo.repository.ItemRepository;
import com.rbms.renbo.repository.TransactionRepository;
import com.rbms.renbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reporting service — all methods are read-only aggregations over existing data.
 * Three reports:
 *  1. ownerEarningsReport  — income breakdown by item for a given date range
 *  2. ownerTransactionSummary — lifecycle status counts (lifetime totals)
 *  3. adminSystemReport    — platform-wide snapshot for admins
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository        userRepository;
    private final ItemRepository        itemRepository;

    // ─── 1. Owner Earnings Report ─────────────────────────────────────────────

    /**
     * Calculates an owner's total earnings and per-item breakdown for a date range.
     * Only COMPLETED + PAID transactions are included.
     *
     * @param ownerId  the owner whose report to generate
     * @param fromDate inclusive start of the date range (matched against transaction startDate)
     * @param toDate   inclusive end of the date range
     * @throws ApiException USER_NOT_FOUND if the owner does not exist
     * @throws ApiException BAD_REQUEST    if fromDate is after toDate
     */
    public OwnerEarningsReportDto ownerEarningsReport(UUID ownerId,
                                                      LocalDate fromDate,
                                                      LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST,
                    "fromDate must not be after toDate");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCodeEnum.USER_NOT_FOUND));

        List<Transactions> txList = transactionRepository
                .findCompletedPaidByOwnerAndDateRange(ownerId, fromDate, toDate);

        // Group by item → aggregate per-item totals
        Map<UUID, List<Transactions>> byItem = txList.stream()
                .collect(Collectors.groupingBy(t -> t.getItem().getID()));

        List<ItemEarningDto> breakdown = byItem.values().stream()
                .map(itemTxList -> {
                    Transactions first = itemTxList.get(0);

                    BigDecimal itemEarnings = itemTxList.stream()
                            .map(t -> t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    int itemDays = itemTxList.stream()
                            .mapToInt(Transactions::getTotalDays)
                            .sum();

                    return new ItemEarningDto(
                            first.getItem().getID(),
                            first.getItem().getName(),
                            itemEarnings,
                            itemTxList.size(),
                            itemDays
                    );
                })
                // Sort by totalEarnings descending — best-performing item first
                .sorted(Comparator.comparing(ItemEarningDto::getTotalEarnings).reversed())
                .collect(Collectors.toList());

        BigDecimal totalEarnings = breakdown.stream()
                .map(ItemEarningDto::getTotalEarnings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OwnerEarningsReportDto report = new OwnerEarningsReportDto();
        report.setOwnerId(ownerId);
        report.setOwnerName(owner.getFirstName() + " " + owner.getLastName());
        report.setFromDate(fromDate);
        report.setToDate(toDate);
        report.setTotalEarnings(totalEarnings);
        report.setCompletedTransactionCount(txList.size());
        report.setItemBreakdown(breakdown);
        report.setGeneratedAt(LocalDateTime.now());

        log.info("Earnings report generated for owner {} — {} transactions, total {}",
                ownerId, txList.size(), totalEarnings);
        return report;
    }

    // ─── 2. Owner Transaction Summary ────────────────────────────────────────

    /**
     * Returns lifetime counts of each transaction status for an owner.
     * Useful as a dashboard summary card.
     *
     * @param ownerId the owner whose summary to generate
     * @throws ApiException USER_NOT_FOUND if the owner does not exist
     */
    public OwnerTransactionSummaryDto ownerTransactionSummary(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCodeEnum.USER_NOT_FOUND));

        List<Transactions> all = transactionRepository.findAllByOwnerId(ownerId);

        long pending   = countByStatus(all, TransactionStatusEnum.PENDING);
        long approved  = countByStatus(all, TransactionStatusEnum.APPROVED);
        long active    = countByStatus(all, TransactionStatusEnum.ACTIVE);
        long completed = countByStatus(all, TransactionStatusEnum.COMPLETED);
        long cancelled = countByStatus(all, TransactionStatusEnum.CANCELLED);

        OwnerTransactionSummaryDto summary = new OwnerTransactionSummaryDto();
        summary.setOwnerId(ownerId);
        summary.setOwnerName(owner.getFirstName() + " " + owner.getLastName());
        summary.setPending(pending);
        summary.setApproved(approved);
        summary.setActive(active);
        summary.setCompleted(completed);
        summary.setCancelled(cancelled);
        summary.setTotal(all.size());
        summary.setGeneratedAt(LocalDateTime.now());

        log.info("Transaction summary generated for owner {} — {} total transactions", ownerId, all.size());
        return summary;
    }

    // ─── 3. Admin System Report ───────────────────────────────────────────────

    /**
     * Platform-wide snapshot for admins — user counts, item counts,
     * transaction volume by status, and total platform earnings.
     */
    public AdminSystemReportDto adminSystemReport() {
        List<User> allUsers = userRepository.findAll();
        long totalItems    = itemRepository.count();
        List<Transactions> allTx = transactionRepository.findAll();
        List<Transactions> paidTx = transactionRepository.findAllCompletedAndPaid();

        BigDecimal platformEarnings = paidTx.stream()
                .map(t -> t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AdminSystemReportDto report = new AdminSystemReportDto();

        // User counts
        report.setTotalUsers(allUsers.size());
        report.setTotalOwners(countByRole(allUsers, UserRoleEnum.OWNER));
        report.setTotalRenters(countByRole(allUsers, UserRoleEnum.RENTER));
        report.setTotalAdmins(countByRole(allUsers, UserRoleEnum.ADMIN));

        // Item count
        report.setTotalItems(totalItems);

        // Transaction counts
        report.setTotalTransactions(allTx.size());
        report.setPendingTransactions(countByStatus(allTx,   TransactionStatusEnum.PENDING));
        report.setApprovedTransactions(countByStatus(allTx,  TransactionStatusEnum.APPROVED));
        report.setActiveTransactions(countByStatus(allTx,    TransactionStatusEnum.ACTIVE));
        report.setCompletedTransactions(countByStatus(allTx, TransactionStatusEnum.COMPLETED));
        report.setCancelledTransactions(countByStatus(allTx, TransactionStatusEnum.CANCELLED));

        // Earnings
        report.setTotalPlatformEarnings(platformEarnings);
        report.setGeneratedAt(LocalDateTime.now());

        log.info("Admin system report generated — {} users, {} items, {} transactions, earnings {}",
                allUsers.size(), totalItems, allTx.size(), platformEarnings);
        return report;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private long countByStatus(List<Transactions> list, TransactionStatusEnum status) {
        return list.stream()
                .filter(t -> status.equals(t.getTransactionStatus()))
                .count();
    }

    private long countByRole(List<User> users, UserRoleEnum role) {
        return users.stream()
                .filter(u -> role.equals(u.getRole()))
                .count();
    }
}