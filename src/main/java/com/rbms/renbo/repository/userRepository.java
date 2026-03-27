package com.rbms.renbo.repository;

import com.rbms.renbo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query(value = "select * from user where role=?1", nativeQuery = true)
    List<User> findByRole(String role);

    @Query(value = "update user set status=?1 where userid=?2", nativeQuery = true)
    void updateStatusUser(String status, UUID id);

    User findUserByEmail(String email);

}
