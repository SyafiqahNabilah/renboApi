package com.rbms.renbo.controller;

import com.rbms.renbo.model.AdminSystemReportDto;
import com.rbms.renbo.model.OwnerEarningsReportDto;
import com.rbms.renbo.model.OwnerTransactionSummaryDto;
import com.rbms.renbo.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reporting endpoints — all require authentication.
 * Endpoint summary:
 *   GET /report/owner/{ownerId}/earnings?from=&to=   → owner earnings by date range
 *   GET /report/owner/{ownerId}/summary              → owner lifetime transaction counts
 *   GET /report/admin/system                         → platform-wide admin snapshot
 */
@RestController
@RequestMapping(value = "/report", produces = "application/json")
@Tag(name = "Reporting", description = "Platform reporting and analytics APIs")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ─── 1. Owner Earnings Report ─────────────────────────────────────────────

    @GetMapping("/owner/{ownerId}/earnings")
    @Operation(
            summary = "Owner earnings report",
            description = "Returns total earnings and a per-item breakdown for a given date range. " +
                    "Only COMPLETED + PAID transactions are included. " +
                    "Date range is matched against the transaction startDate.\n" +
                    "Example: `GET /report/owner/{id}/earnings?from=2025-01-01&to=2025-12-31`"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "fromDate is after toDate"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Owner not found")
    })
    public OwnerEarningsReportDto ownerEarningsReport(
            @PathVariable
            @Parameter(description = "Owner's user ID") UUID ownerId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Start date inclusive (ISO-8601, e.g. 2025-01-01)") LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "End date inclusive (ISO-8601, e.g. 2025-12-31)") LocalDate to) {

        return reportService.ownerEarningsReport(ownerId, from, to);
    }

    // ─── 2. Owner Transaction Summary ────────────────────────────────────────

    @GetMapping("/owner/{ownerId}/summary")
    @Operation(
            summary = "Owner transaction summary",
            description = "Returns lifetime counts of each transaction status (PENDING, APPROVED, " +
                    "ACTIVE, COMPLETED, CANCELLED) for the given owner. " +
                    "Useful as a dashboard summary card."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary generated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Owner not found")
    })
    public OwnerTransactionSummaryDto ownerTransactionSummary(
            @PathVariable
            @Parameter(description = "Owner's user ID") UUID ownerId) {

        return reportService.ownerTransactionSummary(ownerId);
    }

    // ─── 3. Admin System Report ───────────────────────────────────────────────

    @GetMapping("/admin/system")
    @Operation(
            summary = "Admin system report",
            description = "Platform-wide snapshot for admins: user counts by role, total item listings, " +
                    "transaction volume by status, and total platform earnings " +
                    "(sum of all COMPLETED + PAID transaction amounts)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden — admin only")
    })
    public AdminSystemReportDto adminSystemReport() {
        return reportService.adminSystemReport();
    }
}