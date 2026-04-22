package com.rbms.renbo.entity;

import com.rbms.renbo.constant.UserRoleEnum;
import com.rbms.renbo.constant.UserStatusEnum;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.UUID;

//Entity for table name/entity in db
@Data
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID userID;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phoneNo;
    private String address;
    private String noAccount;// bank account for owners
    private Timestamp joinedDate;

    @Enumerated(EnumType.STRING)
    private UserRoleEnum role;        // OWNER, RENTER, ADMIN

    @Enumerated(EnumType.STRING)
    private UserStatusEnum status;    // ACTIVE, INACTIVE

}
