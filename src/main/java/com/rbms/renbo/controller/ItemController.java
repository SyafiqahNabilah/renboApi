package com.rbms.renbo.controller;

import com.rbms.renbo.model.ItemRequestDto;
import com.rbms.renbo.model.ItemResponseDto;
import com.rbms.renbo.service.ItemService;
import com.rbms.renbo.util.JwtUtil;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Syafiqah Nabilah
 */
@Slf4j
@RestController
@RequestMapping("/item")
public class ItemController {

    private final ItemService itemService;
    private final JwtUtil jwtUtil;

    public ItemController(ItemService itemService, JwtUtil jwtUtil) {
        this.itemService = itemService;
        this.jwtUtil = jwtUtil;
    }

    // save item in db
    @PostMapping(path = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponseDto> addItem(
            @Valid
            @ModelAttribute ItemRequestDto item) throws IOException {
        log.info("request:{}", item);
        ItemResponseDto response = itemService.saveItem(item, item.getItemImage1(), item.getItemImage2(), item.getItemImage3());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public List<ItemResponseDto> showList() {
        return itemService.listOfItem();
    }

    @GetMapping("all/{ownerId}")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public List<ItemResponseDto> showListByOwner(@PathVariable UUID ownerId) {
        return itemService.listOfItemByOwner(ownerId);
    }

    @GetMapping("/details/{id}")
    public ItemResponseDto showItemDetails(@PathVariable UUID id) {
        return itemService.findById(id);
    }

    @PutMapping("/update/{id}")
    public ItemResponseDto updateItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id,
            @Valid ItemRequestDto item) {
        UUID ownerId = jwtUtil.validateUserExisting(authHeader);
        //need to get authentication from bearer token, then get user id from token and pass to service to check if the user is the owner of the item
        return itemService.updateItem(id, item, ownerId);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteItem(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id) {

        UUID ownerId = jwtUtil.validateUserExisting(authHeader);
        itemService.deleteItem(id, ownerId);
    }
}
