package com.rbms.renbo.mapper;

import com.rbms.renbo.entity.User;
import com.rbms.renbo.model.UserResponseDto;
import com.rbms.renbo.model.userRegistrationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {
    @Mapping(target = "fullName", expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    UserResponseDto toDto(User user);

    @Mapping(target = "userID", ignore = true)
    @Mapping(target = "subsPlan", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    User updateEntityFromDto(userRegistrationDto dto);

}
