package com.rbms.renbo.service;

import com.rbms.renbo.config.exception.ApiException;
import com.rbms.renbo.constant.ErrorCodeEnum;
import com.rbms.renbo.entity.Item;
import com.rbms.renbo.entity.User;
import com.rbms.renbo.mapper.ItemMapper;
import com.rbms.renbo.model.ItemRequestDto;
import com.rbms.renbo.model.ItemResponseDto;
import com.rbms.renbo.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private ItemMapper mapper;
    @Mock private UserService userService;

    @InjectMocks
    private ItemService itemService;

    @TempDir
    Path tempDir;

    // ── shared fixtures ───────────────────────────────────────────────────────

    private UUID ownerId;
    private UUID otherUserId;
    private UUID itemId;
    private User owner;
    private Item testItem;
    private ItemResponseDto testDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(itemService, "uploadDir",    tempDir.toString());
        ReflectionTestUtils.setField(itemService, "publicBaseUrl","http://localhost:8080");

        ownerId      = UUID.randomUUID();
        otherUserId  = UUID.randomUUID();
        itemId       = UUID.randomUUID();

        owner = new User();
        owner.setUserID(ownerId);

        testItem = new Item();
        testItem.setID(itemId);
        testItem.setOwner(owner);
        testItem.setName("Test Chair");
        testItem.setRate(BigDecimal.valueOf(50));

        testDto = new ItemResponseDto();
        testDto.setId(itemId);
        testDto.setName("Test Chair");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // saveItem  (existing tests – kept untouched)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("saveItem")
    class SaveItem {

        @Test
        @DisplayName("saves image to disk and persists generated filename")
        void savesImageAndPersistsGeneratedFilename() throws IOException {
            ItemRequestDto request = new ItemRequestDto();
            request.setOwnerId(ownerId);

            MockMultipartFile image = new MockMultipartFile(
                    "itemImage1", "chair.png", "image/png", "png-content".getBytes());

            Item item = new Item();
            when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
            when(mapper.updateEntityFromRequestDto(request)).thenReturn(item);
            when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDto(any(Item.class))).thenReturn(new ItemResponseDto());

            ItemResponseDto result = itemService.saveItem(request, image, null, null);

            assertNotNull(result);
            assertEquals(owner, item.getOwner());
            assertNotNull(item.getItemImage1());
            assertTrue(item.getItemImage1().endsWith(".png"));
            assertTrue(Files.exists(tempDir.resolve(item.getItemImage1())));
            verify(itemRepository).save(item);
        }

        @Test
        @DisplayName("rejects non-image upload with BAD_REQUEST")
        void rejectsNonImageUpload() {
            ItemRequestDto request = new ItemRequestDto();
            request.setOwnerId(ownerId);

            MockMultipartFile file = new MockMultipartFile(
                    "itemImage1", "notes.txt", "text/plain", "hello".getBytes());

            when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
            when(mapper.updateEntityFromRequestDto(request)).thenReturn(new Item());

            ApiException ex = assertThrows(ApiException.class,
                    () -> itemService.saveItem(request, file, null, null));

            assertEquals(ErrorCodeEnum.BAD_REQUEST, ex.getErrorCode());
            verify(itemRepository, never()).save(any());
        }

        @Test
        @DisplayName("accepts image upload with no filename extension")
        void acceptsImageWithoutFilenameExtension() throws IOException {
            ItemRequestDto request = new ItemRequestDto();
            request.setOwnerId(ownerId);

            MockMultipartFile image = new MockMultipartFile(
                    "itemImage1", "upload", "image/jpeg", "jpeg-content".getBytes());

            Item item = new Item();
            when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
            when(mapper.updateEntityFromRequestDto(request)).thenReturn(item);
            when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDto(any(Item.class))).thenReturn(new ItemResponseDto());

            itemService.saveItem(request, image, null, null);

            assertNotNull(item.getItemImage1());
            assertTrue(Files.exists(tempDir.resolve(item.getItemImage1())));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findById
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns mapped DTO when item exists")
        void returnsDtoWhenFound() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
            when(mapper.toDto(testItem)).thenReturn(testDto);

            ItemResponseDto result = itemService.findById(itemId);

            assertNotNull(result);
            assertEquals(itemId, result.getId());
            verify(mapper).toDto(testItem);
        }

        @Test
        @DisplayName("prepends public base URL to image filenames")
        void prependsBaseUrlToImageFilenames() {
            testDto.setItemImage1("chair.png");
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
            when(mapper.toDto(testItem)).thenReturn(testDto);

            ItemResponseDto result = itemService.findById(itemId);

            assertEquals("http://localhost:8080/img/products/chair.png", result.getItemImage1());
        }

        @Test
        @DisplayName("does not double-prefix an image URL that already starts with http")
        void doesNotDoublePrefixAbsoluteUrl() {
            testDto.setItemImage1("http://cdn.example.com/chair.png");
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
            when(mapper.toDto(testItem)).thenReturn(testDto);

            ItemResponseDto result = itemService.findById(itemId);

            assertEquals("http://cdn.example.com/chair.png", result.getItemImage1());
        }

        @Test
        @DisplayName("throws ITEM_NOT_FOUND when item does not exist")
        void throwsWhenNotFound() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> itemService.findById(itemId));

            assertEquals(ErrorCodeEnum.ITEM_NOT_FOUND, ex.getErrorCode());
            verifyNoInteractions(mapper);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getItemEntityById
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getItemEntityById")
    class GetItemEntityById {

        @Test
        @DisplayName("returns raw entity when item exists")
        void returnsEntityWhenFound() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));

            Item result = itemService.getItemEntityById(itemId);

            assertNotNull(result);
            assertEquals(itemId, result.getID());
            assertEquals("Test Chair", result.getName());
        }

        @Test
        @DisplayName("throws ITEM_NOT_FOUND when item does not exist")
        void throwsWhenNotFound() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> itemService.getItemEntityById(itemId));

            assertEquals(ErrorCodeEnum.ITEM_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listOfItem
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listOfItem")
    class ListOfItem {

        @Test
        @DisplayName("returns mapped DTO list for all items")
        void returnsMappedList() {
            Item item2 = new Item();
            item2.setID(UUID.randomUUID());
            item2.setOwner(owner);

            ItemResponseDto dto2 = new ItemResponseDto();
            dto2.setId(item2.getID());

            when(itemRepository.findAll()).thenReturn(List.of(testItem, item2));
            when(mapper.toDto(testItem)).thenReturn(testDto);
            when(mapper.toDto(item2)).thenReturn(dto2);

            List<ItemResponseDto> result = itemService.listOfItem();

            assertEquals(2, result.size());
            verify(mapper, times(2)).toDto(any(Item.class));
        }

        @Test
        @DisplayName("returns empty list when no items exist")
        void returnsEmptyList() {
            when(itemRepository.findAll()).thenReturn(List.of());

            List<ItemResponseDto> result = itemService.listOfItem();

            assertTrue(result.isEmpty());
            verifyNoInteractions(mapper);
        }

        @Test
        @DisplayName("applies image URL prefix to every item in the list")
        void appliesImageUrlsToAllItems() {
            testDto.setItemImage1("img.png");
            when(itemRepository.findAll()).thenReturn(List.of(testItem));
            when(mapper.toDto(testItem)).thenReturn(testDto);

            List<ItemResponseDto> result = itemService.listOfItem();

            assertEquals("http://localhost:8080/img/products/img.png",
                    result.get(0).getItemImage1());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listOfItemByOwner
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listOfItemByOwner")
    class ListOfItemByOwner {

        @Test
        @DisplayName("returns only items belonging to the given owner")
        void returnsOwnerItems() {
            when(itemRepository.findByUser(ownerId)).thenReturn(List.of(testItem));
            when(mapper.toDto(testItem)).thenReturn(testDto);

            List<ItemResponseDto> result = itemService.listOfItemByOwner(ownerId);

            assertEquals(1, result.size());
            assertEquals(itemId, result.get(0).getId());
            verify(itemRepository).findByUser(ownerId);
        }

        @Test
        @DisplayName("returns empty list when owner has no items")
        void returnsEmptyWhenOwnerHasNoItems() {
            when(itemRepository.findByUser(ownerId)).thenReturn(List.of());

            List<ItemResponseDto> result = itemService.listOfItemByOwner(ownerId);

            assertTrue(result.isEmpty());
            verifyNoInteractions(mapper);
        }

        @Test
        @DisplayName("only queries with the supplied ownerId — does not call findAll")
        void queriesWithOwnerId() {
            when(itemRepository.findByUser(ownerId)).thenReturn(List.of());

            itemService.listOfItemByOwner(ownerId);

            verify(itemRepository).findByUser(ownerId);
            verify(itemRepository, never()).findAll();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateItem  (existing test kept + failure paths added)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateItem")
    class UpdateItem {

        @Test
        @DisplayName("updates existing entity in-place — does not create a new one")
        void updatesExistingEntityInsteadOfCreatingNewOne() {
            ItemRequestDto request = new ItemRequestDto();
            request.setName("Updated chair");
            request.setOwnerId(ownerId);

            ItemResponseDto response = new ItemResponseDto();
            response.setId(itemId);
            response.setName("Updated chair");

            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
            when(userService.getUserDetails(ownerId)).thenReturn(Optional.of(owner));
            doAnswer(inv -> { testItem.setName(((ItemRequestDto) inv.getArgument(0)).getName()); return null; })
                    .when(mapper).updateEntityFromRequestDto(eq(request), eq(testItem));
            when(itemRepository.save(testItem)).thenReturn(testItem);
            when(mapper.toDto(testItem)).thenReturn(response);

            ItemResponseDto result = itemService.updateItem(itemId, request, ownerId);

            assertEquals("Updated chair", testItem.getName());
            assertEquals(response, result);
            verify(itemRepository).save(testItem);
        }

        @Test
        @DisplayName("throws ITEM_NOT_FOUND when item does not exist")
        void throwsWhenItemNotFound() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> itemService.updateItem(itemId, new ItemRequestDto(), ownerId));

            assertEquals(ErrorCodeEnum.ITEM_NOT_FOUND, ex.getErrorCode());
            verify(itemRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UNAUTHORIZED when caller is not the item owner")
        void throwsWhenCallerIsNotOwner() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
            // testItem.owner.userID = ownerId; caller = otherUserId → mismatch

            ApiException ex = assertThrows(ApiException.class,
                    () -> itemService.updateItem(itemId, new ItemRequestDto(), otherUserId));

            assertEquals(ErrorCodeEnum.UNAUTHORIZED, ex.getErrorCode());
            verify(itemRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws FORBIDDEN_OWNER_CHANGE when ownerId in body differs from caller")
        void throwsWhenOwnerIdInBodyDiffersFromCaller() {
            UUID thirdPartyId = UUID.randomUUID();

            ItemRequestDto request = new ItemRequestDto();
            request.setOwnerId(thirdPartyId); // trying to reassign item to someone else

            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
            // First auth check passes (caller = ownerId = testItem.owner)
            doNothing().when(mapper).updateEntityFromRequestDto(any(), any());

            ApiException ex = assertThrows(ApiException.class,
                    () -> itemService.updateItem(itemId, request, ownerId));

            assertEquals(ErrorCodeEnum.FORBIDDEN_OWNER_CHANGE, ex.getErrorCode());
            verify(itemRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteItem
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteItem")
    class DeleteItem {

        @Test
        @DisplayName("deletes item when caller is the owner")
        void deletesWhenCallerIsOwner() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));

            assertDoesNotThrow(() -> itemService.deleteItem(itemId, ownerId));

            verify(itemRepository).deleteById(itemId);
        }

        @Test
        @DisplayName("throws ITEM_NOT_FOUND when item does not exist")
        void throwsWhenItemNotFound() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> itemService.deleteItem(itemId, ownerId));

            assertEquals(ErrorCodeEnum.ITEM_NOT_FOUND, ex.getErrorCode());
            verify(itemRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("throws UNAUTHORIZED when caller is not the item owner")
        void throwsWhenCallerIsNotOwner() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));

            ApiException ex = assertThrows(ApiException.class,
                    () -> itemService.deleteItem(itemId, otherUserId));

            assertEquals(ErrorCodeEnum.UNAUTHORIZED, ex.getErrorCode());
            verify(itemRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("does not call deleteById before verifying ownership")
        void verifiesOwnershipBeforeDeleting() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));

            assertThrows(ApiException.class, () -> itemService.deleteItem(itemId, otherUserId));

            verify(itemRepository, never()).deleteById(itemId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateItemAvailability
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateItemAvailability")
    class UpdateItemAvailability {

        @Test
        @DisplayName("sets the availability field and saves the item")
        void setsAvailabilityAndSaves() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
            when(itemRepository.save(testItem)).thenReturn(testItem);

            itemService.updateItemAvailability(itemId, "RENTED");

            assertEquals("RENTED", testItem.getAvailability());
            verify(itemRepository).save(testItem);
        }

        @Test
        @DisplayName("persists AVAILABLE status correctly")
        void setsAvailableStatus() {
            testItem.setAvailability("RENTED");
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
            when(itemRepository.save(testItem)).thenReturn(testItem);

            itemService.updateItemAvailability(itemId, "AVAILABLE");

            assertEquals("AVAILABLE", testItem.getAvailability());
            verify(itemRepository).save(testItem);
        }

        @Test
        @DisplayName("throws ITEM_NOT_FOUND when item does not exist")
        void throwsWhenItemNotFound() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> itemService.updateItemAvailability(itemId, "AVAILABLE"));

            assertEquals(ErrorCodeEnum.ITEM_NOT_FOUND, ex.getErrorCode());
            verify(itemRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getItemOwnerId
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getItemOwnerId")
    class GetItemOwnerId {

        @Test
        @DisplayName("returns the owner's UUID when item exists")
        void returnsOwnerIdWhenItemFound() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));

            UUID result = itemService.getItemOwnerId(itemId);

            assertEquals(ownerId, result);
        }

        @Test
        @DisplayName("throws ITEM_NOT_FOUND when item does not exist")
        void throwsWhenItemNotFound() {
            when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> itemService.getItemOwnerId(itemId));

            assertEquals(ErrorCodeEnum.ITEM_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("returns the correct owner even when multiple items exist")
        void returnsCorrectOwnerForCorrectItem() {
            User otherOwner = new User();
            otherOwner.setUserID(otherUserId);

            Item otherItem = new Item();
            otherItem.setID(UUID.randomUUID());
            otherItem.setOwner(otherOwner);

            when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));

            UUID result = itemService.getItemOwnerId(itemId);

            assertEquals(ownerId, result);
            assertNotEquals(otherUserId, result);
        }
    }
}