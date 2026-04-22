package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.entity.Item;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.mapper.ItemMapper;
import com.rbms.renbo.model.ItemRequestDto;
import com.rbms.renbo.model.ItemResponseDto;
import com.rbms.renbo.repository.ItemRepository;
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

    @Value("${app.public.base-url:http://localhost:8080}")
    private String publicBaseUrl;

    private final ItemRepository itemRepository;
    private final ItemMapper mapper;
    private final UserService userService;

    public ItemService(UserService userService, ItemMapper mapper, ItemRepository repo) {
        this.userService = userService;
        this.mapper = mapper;
        this.itemRepository = repo;
    }

    public ItemResponseDto saveItem(ItemRequestDto dto,
                                    MultipartFile image1,
                                    MultipartFile image2,
                                    MultipartFile image3) throws IOException {

        log.info("requestDTO:{}", dto);
        if (dto.getOwnerId() == null) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST, "ownerId is required");
        }

        User owner = userService.getUserDetails(dto.getOwnerId())
                .orElseThrow(() -> new ApiException(ErrorCodeEnum.USER_NOT_FOUND));

        Item item = mapper.updateEntityFromRequestDto(dto);
        // Save images and store the filename
        item.setItemImage1(saveFile(image1));
        item.setItemImage2(saveFile(image2));
        item.setItemImage3(saveFile(image3));
        item.setOwner(owner);
        log.debug("itemEntity before save:{}", item);
        Item saved = itemRepository.save(item);
        return withImageUrls(mapper.toDto(saved));
    }

    private String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ApiException(ErrorCodeEnum.BAD_REQUEST, "Only image files are allowed");
        }

        // Generate a unique filename to avoid collisions
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = "";
        int extensionIndex = originalName.lastIndexOf(".");
        if (extensionIndex >= 0 && extensionIndex < originalName.length() - 1) {
            extension = originalName.substring(extensionIndex);
        }
        String uniqueName = UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
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
        return withImageUrls(dto);
    }

    public Item getItemEntityById(UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCodeEnum.ITEM_NOT_FOUND));
    }

    public ItemResponseDto updateItem(UUID id, ItemRequestDto item, UUID requestingUserId) {
        Item existing = itemRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCodeEnum.ITEM_NOT_FOUND));

        // ✅ Authorization check - verify user owns this item
        if (!existing.getOwner().getUserID().equals(requestingUserId)) {
            throw new ApiException(ErrorCodeEnum.UNAUTHORIZED);
        }

        mapper.updateEntityFromRequestDto(item, existing);

        if (item.getOwnerId() != null) {
            // Prevent user from reassigning item to another owner (optional)
            if (!item.getOwnerId().equals(requestingUserId)) {
                throw new ApiException(ErrorCodeEnum.FORBIDDEN_OWNER_CHANGE);
            }

            User owner = userService.getUserDetails(item.getOwnerId())
                    .orElseThrow(() -> new ApiException(ErrorCodeEnum.USER_NOT_FOUND));
            existing.setOwner(owner);
        }

        return withImageUrls(mapper.toDto(itemRepository.save(existing)));
    }

    public List<ItemResponseDto> listOfItem() {
        List<Item> items = itemRepository.findAll();
        return items.stream()
                    .map(mapper::toDto)
                    .map(this::withImageUrls)
                    .toList();
    }

    public List<ItemResponseDto> listOfItemByOwner(UUID ownerId) {
        List<Item> items = itemRepository.findByUser(ownerId);
            return items.stream()
                    .map(mapper::toDto)
                    .map(this::withImageUrls)
                    .toList();
    }

    public void deleteItem(UUID id, UUID requestingUserId) {
        Item existing = itemRepository.findById(id)
                    .orElseThrow(() -> new ApiException(ErrorCodeEnum.ITEM_NOT_FOUND));

            // ✅ Authorization check - verify user owns this item
            if (!existing.getOwner().getUserID().equals(requestingUserId)) {
                throw new ApiException(ErrorCodeEnum.UNAUTHORIZED);
            }

            itemRepository.deleteById(id);
    }

    public void updateItemAvailability(UUID itemId, String availability) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(ErrorCodeEnum.ITEM_NOT_FOUND));

        item.setAvailability(availability);
        itemRepository.save(item);
    }

    public UUID getItemOwnerId(UUID itemId) {
        return getItemEntityById(itemId).getOwner().getUserID();
    }

    private ItemResponseDto withImageUrls(ItemResponseDto dto) {
        dto.setItemImage1(toPublicImageUrl(dto.getItemImage1()));
        dto.setItemImage2(toPublicImageUrl(dto.getItemImage2()));
        dto.setItemImage3(toPublicImageUrl(dto.getItemImage3()));
        return dto;
    }

    private String toPublicImageUrl(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return fileName;
        }

        if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
            return fileName;
        }

        return publicBaseUrl.replaceAll("/+$", "") + "/img/products/" + fileName;
    }
}
