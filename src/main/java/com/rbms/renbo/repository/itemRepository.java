/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
public interface itemRepository extends JpaRepository<Item, UUID> {
    @Query(value = "select * from item where availability='AVAILABLE'", nativeQuery = true)
    List<Item> findAll();

    @Query(value = "select * from item where ownerID=?1", nativeQuery = true)
    List<Item> findByUser(int ownerID);

//    Owner findByEmail(String email);
}
