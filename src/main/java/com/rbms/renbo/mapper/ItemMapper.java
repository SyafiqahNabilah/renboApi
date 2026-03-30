package com.rbms.renbo.mapper;

import com.rbms.renbo.entity.Item;
import com.rbms.renbo.model.ItemRequestDto;
import com.rbms.renbo.model.ItemResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ItemMapper {

    ItemResponseDto toDto(Item item);


    @Mapping(target = "ID", ignore = true)
    @Mapping(target = "itemImage1", ignore = true)
    @Mapping(target = "itemImage2", ignore = true)
    @Mapping(target = "itemImage3", ignore = true)
    @Mapping(target = "availability", constant = "AVAILABLE")
    Item updateEntityFromRequestDto(ItemRequestDto dto);

    Item updateEntityFromResponseDto(ItemResponseDto dto);

}
