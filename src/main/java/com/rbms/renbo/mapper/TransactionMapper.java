package com.rbms.renbo.mapper;

import com.rbms.renbo.entity.Transactions;
import com.rbms.renbo.model.TransactionRequestDto;
import com.rbms.renbo.model.TransactionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TransactionMapper {

    @Mapping(target = "ownerName", expression = "java(transaction.getOwner() != null ? transaction.getOwner().getFirstName() + \" \" + transaction.getOwner().getLastName() : null)")
    @Mapping(target = "ownerEmail", expression = "java(transaction.getOwner() != null ? transaction.getOwner().getEmail() : null)")
    @Mapping(target = "renterName", expression = "java(transaction.getRenter() != null ? transaction.getRenter().getFirstName() + \" \" + transaction.getRenter().getLastName() : null)")
    @Mapping(target = "renterEmail", expression = "java(transaction.getRenter() != null ? transaction.getRenter().getEmail() : null)")
    @Mapping(target = "itemName", expression = "java(transaction.getItem() != null ? transaction.getItem().getName() : null)")
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
    @Mapping(target = "totalDays", expression = "java((int) java.time.temporal.ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()))")
    @Mapping(target = "totalAmount", expression = "java(dto.getDailyRate() != null && dto.getEndDate() != null && dto.getStartDate() != null ? dto.getDailyRate() * java.time.temporal.ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) : 0.0f)")
    Transactions updateEntityFromRequestDto(TransactionRequestDto dto);
}
