package com.rbms.renbo.repository;

import com.rbms.renbo.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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

}
