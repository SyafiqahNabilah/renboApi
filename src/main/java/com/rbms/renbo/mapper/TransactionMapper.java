package com.rbms.renbo.mapper;

import com.rbms.renbo.entity.Transactions;
import com.rbms.renbo.model.TransactionRequestDto;
import com.rbms.renbo.model.TransactionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TransactionMapper {

    @Mapping(target = "ownerName", expression = "java(transaction.getOwner() != null ? transaction.getOwner().getFirstName() + \" \" + transaction.getOwner().getLastName() : null)")
    @Mapping(target = "ownerEmail", expression = "java(transaction.getOwner() != null ? transaction.getOwner().getEmail() : null)")
    @Mapping(target = "renterName", expression = "java(transaction.getRenter() != null ? transaction.getRenter().getFirstName() + \" \" + transaction.getRenter().getLastName() : null)")
    @Mapping(target = "renterEmail", expression = "java(transaction.getRenter() != null ? transaction.getRenter().getEmail() : null)")
    @Mapping(target = "itemName", source = "transaction.item.name")
    @Mapping(target = "itemDescription", expression = "java(transaction.getItem() != null ? transaction.getItem().getDescription() : null)")
    TransactionResponseDto toDto(Transactions transaction);

    @Mapping(target = "transactionID", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "renter", ignore = true)
    @Mapping(target = "item", ignore = true)
    @Mapping(target = "approvedDate", ignore = true)
    @Mapping(target = "returnedDate", ignore = true)
    @Mapping(target = "paymentDate", ignore = true)
    @Mapping(target = "ownerNote", ignore = true)
    @Mapping(target = "transactionStatus", expression = "java(com.rbms.renbo.constant.TransactionStatusEnum.PENDING)")
    @Mapping(target = "paymentStatus", expression = "java(com.rbms.renbo.constant.PaymentStatusEnum.UNPAID)")
    @Mapping(target = "requestedDate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "totalDays", source = "dto", qualifiedByName = "calculateTotalDays")
    @Mapping(target = "totalAmount", source = "dto", qualifiedByName = "calculateTotalAmount")
    Transactions updateEntityFromRequestDto(TransactionRequestDto dto);

    @Named("calculateTotalDays")
    default int calculateTotalDays(TransactionRequestDto dto) {
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            return (int) java.time.temporal.ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate());
        }
        return 0;
    }

    @Named("calculateTotalAmount")
    default BigDecimal calculateTotalAmount(TransactionRequestDto dto) {
        if (dto.getDailyRate() != null) {
            int totalDays = (int) java.time.temporal.ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate());
            return dto.getDailyRate().multiply(new BigDecimal(totalDays));
        }
        return java.math.BigDecimal.ZERO;
    }

}
