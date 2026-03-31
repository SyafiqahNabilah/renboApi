/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.entity.Item;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.mapper.ItemMapper;
import com.rbms.renbo.model.ItemRequestDto;
import com.rbms.renbo.model.ItemResponseDto;
import com.rbms.renbo.repository.itemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ItemService {
    @Value("${app.upload.dir}") // from application.properties
    private String uploadDir;

    private itemRepository itemRepository;
    private final ItemMapper mapper;
    private final userService userService;

    public ItemService(userService userService, ItemMapper mapper, itemRepository repo) {
        this.userService = userService;
        this.mapper = mapper;
        this.itemRepository = repo;
    }

    public ItemResponseDto saveItem(ItemRequestDto dto,
                                    MultipartFile image1,
                                    MultipartFile image2,
                                    MultipartFile image3) throws IOException {

        log.info("requestDTO:{}", dto);
        Item item = mapper.updateEntityFromRequestDto(dto);
        // Save images and store the filename
        item.setItemImage1(saveFile(image1));
        item.setItemImage2(saveFile(image2));
        item.setItemImage3(saveFile(image3));
        User owner = userService.getUserDetails(dto.getOwnerId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        item.setOwner(owner);
        log.debug("itemEntity before save:{}", item);
        Item saved = itemRepository.save(item);
        return mapper.toDto(saved);
    }

    private String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed.");
        }

        // Generate a unique filename to avoid collisions
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String uniqueName = UUID.randomUUID().toString() + extension;

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Files.copy(file.getInputStream(),
                uploadPath.resolve(uniqueName),
                StandardCopyOption.REPLACE_EXISTING);

        return uniqueName; // only store the filename in DB, not the full path
    }

    public ItemResponseDto findById(UUID id) {
        ItemResponseDto dto = itemRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ApiException(ErrorCodeEnum.ITEM_NOT_FOUND));
        return dto;
    }

    public List<ItemResponseDto> listAllOwnerItem(int ownerId) {
        List<ItemResponseDto> list;
        try {
            list = itemRepository.findByUser(ownerId)
                    .stream()
                    .map(mapper::toDto)
                    .toList();
        } catch (Exception e) {
            throw new ApiException(ErrorCodeEnum.INTERNAL_SERVER_ERROR);
        }
        return list;
    }

    public ItemResponseDto updateItem(UUID id, ItemRequestDto item) {
        Item existing = mapper.updateEntityFromResponseDto(findById(id));
        
        return mapper.toDto(itemRepository.save(existing));
    }

    public List<ItemResponseDto> listOfItem() {
        List<ItemResponseDto> list = itemRepository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
        return list;
    }

    public String deleteItem(UUID id) {
        try {
            itemRepository.deleteById(id);
            return "Success";
        } catch (ApiException e) {
            return "Failed";
        }
    }
}
