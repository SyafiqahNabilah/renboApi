/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rbms.renbo.controller;

import com.rbms.renbo.model.ItemRequestDto;
import com.rbms.renbo.model.ItemResponseDto;
import com.rbms.renbo.service.ItemService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    ItemService itemService;

    String systemDir = System.getProperty("user.dir");

    // save item in db
    @PostMapping(path = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponseDto> addItem(
            @ModelAttribute ItemRequestDto item) throws IOException {
        log.info("request:{}", item);
        ItemResponseDto response = itemService.saveItem(item, item.getItemImage1(), item.getItemImage2(), item.getItemImage3());

//        if (response.equals()) {
//            return ResponseEntity.badRequest().build();
//        }

        return ResponseEntity.ok(response);
    }
    // get upload image form
    // @GetMapping("/Add-img/{id}")
    // public String addItemImg(@PathVariable("id") UUID id, Model model) {
    // ItemResponseDto item = itemService.findById(id);
    // model.addAttribute("item", item);
    // return "owner/item-addImg";
    // }

    // save uploaded image in database
//    @PostMapping("/save/image/{id}")
//    public ItemResponseDto saveItemImage(@PathVariable("id") UUID id, ItemRequestDto item) {
//        return itemService.saveItem(item);
//    }

    @GetMapping("/list")
    public List<ItemResponseDto> showList() {
        return itemService.listOfItem();
    }

    @GetMapping("/details/{id}")
    public ItemResponseDto showItemDetails(@PathVariable("id") UUID id) {
        return itemService.findById(id);
    }

    // @GetMapping("/edit/{id}")
    // public String editItem(@PathVariable("id") UUID id, Model model) {
    // ItemResponseDto item = itemService.findById(id);
    // // Owners owner = ownerRepository.findByOwnerEmail(auth.getName());
    // model.addAttribute("item", item);
    // return "owner/item-edit";
    // }

    @PostMapping("/update/{id}")
    public ItemResponseDto updateStudent(@PathVariable UUID id, @Valid ItemRequestDto item) {
        return itemService.updateItem(id, item);
    }

    @DeleteMapping("Item/delete/{id}")
    public String deleteStudent(@PathVariable("id") UUID id) {
        return itemService.deleteItem(id);
    }

    // @GetMapping("/item-details/{id}")
    // public String showItemIndex(@PathVariable("id") int id, Model model) {
    // Item item = itemRepository.findById(id)
    // .orElseThrow(() -> new IllegalArgumentException("Invalid item Id:" + id));
    // model.addAttribute("item", item);
    // return "item-view";
    // }

}
