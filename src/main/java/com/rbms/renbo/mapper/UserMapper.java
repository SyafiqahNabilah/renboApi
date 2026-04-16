package com.rbms.renbo.mapper;

import com.rbms.renbo.entity.User;
import com.rbms.renbo.model.UserResponseDto;
import com.rbms.renbo.model.userRegistrationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {
    @Mapping(target = "fullName", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    @Mapping(target = "joined", expression = "java(setJoinedDate(user.getJoinedDate()))")
    @Mapping(target = "role", source = "user.role", qualifiedByName = "updateText")
    @Mapping(target = "status", source = "user.status", qualifiedByName = "updateText")
    UserResponseDto toDto(User user);

    @Mapping(target = "userID", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "joinedDate", expression = "java(new java.sql.Timestamp(System.currentTimeMillis()))")
    User updateEntityFromDto(userRegistrationDto dto);

    @Named("extractJoinedDate")
    default String setJoinedDate(Timestamp joinedDate) {
        DateTimeFormatter DD_MM_YYYY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
        return joinedDate.toLocalDateTime().format(DD_MM_YYYY_FORMATTER);
    }

    @Named("updateText")
    default String toCamelCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        //change from ADMIN to Admin
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
