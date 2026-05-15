package com.rbms.renbo.repository;

import com.rbms.renbo.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Syafiqah Nabilah
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    @Query(value = "select * from item where ownerID=?1", nativeQuery = true)
    List<Item> findByUser(UUID ownerID);

    @Query("SELECT i.owner.userID, COUNT(i) FROM Item i GROUP BY i.owner.userID")
    List<Object[]> countItemsByUser();

    @Query("SELECT i FROM Item i WHERE " +
           "(:keyword IS NULL OR LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:category IS NULL OR LOWER(i.category) = LOWER(:category)) AND " +
           "(:minPrice IS NULL OR i.rate >= :minPrice) AND " +
           "(:maxPrice IS NULL OR i.rate <= :maxPrice)")
    List<Item> searchItems(String keyword, String category, BigDecimal minPrice, BigDecimal maxPrice);

    @Query("SELECT DISTINCT i.category FROM Item i WHERE i.category IS NOT NULL")
    List<String> findDistinctCategories();
}
