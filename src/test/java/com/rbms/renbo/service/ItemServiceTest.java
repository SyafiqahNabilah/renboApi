package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.entity.Item;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.mapper.ItemMapper;
import com.rbms.renbo.model.ItemRequestDto;
import com.rbms.renbo.model.ItemResponseDto;
import com.rbms.renbo.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper mapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private ItemService itemService;

    @TempDir
    Path tempDir;

    @Test
    void saveItem_savesImageAndPersistsGeneratedFilename() throws IOException {
        ReflectionTestUtils.setField(itemService, "uploadDir", tempDir.toString());

        UUID ownerId = UUID.randomUUID();
        ItemRequestDto request = new ItemRequestDto();
        request.setOwnerId(ownerId);

        MockMultipartFile image = new MockMultipartFile(
                "itemImage1",
                "chair.png",
                "image/png",
                "png-content".getBytes()
        );

        Item item = new Item();
        User owner = new User();
        owner.setUserID(ownerId);
        ItemResponseDto responseDto = new ItemResponseDto();

        when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
        when(mapper.updateEntityFromRequestDto(request)).thenReturn(item);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(Item.class))).thenReturn(responseDto);

        ItemResponseDto result = itemService.saveItem(request, image, null, null);

        assertNotNull(result);
        assertEquals(owner, item.getOwner());
        assertNotNull(item.getItemImage1());
        assertTrue(item.getItemImage1().endsWith(".png"));
        assertTrue(Files.exists(tempDir.resolve(item.getItemImage1())));
        verify(itemRepository).save(item);
    }

    @Test
    void saveItem_rejectsNonImageUpload() {
        ReflectionTestUtils.setField(itemService, "uploadDir", tempDir.toString());

        UUID ownerId = UUID.randomUUID();
        ItemRequestDto request = new ItemRequestDto();
        request.setOwnerId(ownerId);

        MockMultipartFile file = new MockMultipartFile(
                "itemImage1",
                "notes.txt",
                "text/plain",
                "hello".getBytes()
        );

        User owner = new User();
        owner.setUserID(ownerId);

        when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
        when(mapper.updateEntityFromRequestDto(request)).thenReturn(new Item());

        ApiException exception = assertThrows(ApiException.class, () -> itemService.saveItem(request, file, null, null));

        assertEquals(ErrorCodeEnum.BAD_REQUEST, exception.getErrorCode());
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void saveItem_acceptsImageWithoutFilenameExtension() throws IOException {
        ReflectionTestUtils.setField(itemService, "uploadDir", tempDir.toString());

        UUID ownerId = UUID.randomUUID();
        ItemRequestDto request = new ItemRequestDto();
        request.setOwnerId(ownerId);

        MockMultipartFile image = new MockMultipartFile(
                "itemImage1",
                "upload",
                "image/jpeg",
                "jpeg-content".getBytes()
        );

        Item item = new Item();
        User owner = new User();
        owner.setUserID(ownerId);

        when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
        when(mapper.updateEntityFromRequestDto(request)).thenReturn(item);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(Item.class))).thenReturn(new ItemResponseDto());

        itemService.saveItem(request, image, null, null);

        assertNotNull(item.getItemImage1());
        assertTrue(Files.exists(tempDir.resolve(item.getItemImage1())));
    }

    @Test
    void updateItem_updatesExistingEntityInsteadOfCreatingNewOne() {
        UUID itemId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        ItemRequestDto request = new ItemRequestDto();
        request.setName("Updated chair");
        request.setOwnerId(ownerId);

        Item existing = new Item();
        existing.setID(itemId);
        existing.setName("Old chair");


        User owner = new User();
        owner.setUserID(ownerId);
        existing.setOwner(owner);

        ItemResponseDto response = new ItemResponseDto();
        response.setId(itemId);
        response.setName("Updated chair");

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(existing));
        when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
        doAnswer(invocation -> {
            ItemRequestDto dto = invocation.getArgument(0);
            Item entity = invocation.getArgument(1);
            entity.setName(dto.getName());
            return null;
        }).when(mapper).updateEntityFromRequestDto(eq(request), eq(existing));
        when(itemRepository.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenReturn(response);

        ItemResponseDto result = itemService.updateItem(itemId, request, ownerId);

        assertEquals(response, result);
        assertEquals(itemId, existing.getID());
        assertEquals("Updated chair", existing.getName());
        assertEquals(owner, existing.getOwner());
        verify(itemRepository).findById(itemId);
        verify(mapper).updateEntityFromRequestDto(request, existing);
        verify(itemRepository).save(existing);
    }
}
