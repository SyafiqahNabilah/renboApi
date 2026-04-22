package com.rbms.renbo.mapper;

import com.rbms.renbo.entity.Item;
import com.rbms.renbo.model.ItemRequestDto;
import com.rbms.renbo.model.ItemResponseDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ItemMapper {

    @Mapping(target = "itemImage1", expression = "java(item.getItemImage1() != null ? item.getItemImage1() : null)")
    @Mapping(target = "itemImage2", expression = "java(item.getItemImage2() != null ? item.getItemImage2() : null)")
    @Mapping(target = "itemImage3", expression = "java(item.getItemImage3() != null ? item.getItemImage3() : null)")
    @Mapping(target = "id", source = "ID")
    ItemResponseDto toDto(Item item);


    @Mapping(target = "ID", ignore = true)
    @Mapping(target = "itemImage1", ignore = true)
    @Mapping(target = "itemImage2", ignore = true)
    @Mapping(target = "itemImage3", ignore = true)
    @Mapping(target = "availability", source = "availability")
    Item updateEntityFromRequestDto(ItemRequestDto dto);//

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "ID", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "itemImage1", ignore = true)
    @Mapping(target = "itemImage2", ignore = true)
    @Mapping(target = "itemImage3", ignore = true)
    void updateEntityFromRequestDto(ItemRequestDto dto, @MappingTarget Item item);

}
