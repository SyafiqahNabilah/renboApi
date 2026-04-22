package com.rbms.renbo.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRoleEnum {
 RENTER("RENTER", "Renter"),
 OWNER("OWNER", "Owner"),
 ADMIN("ADMIN", "Admin");

 private final String code;
 private final String description;

    public static String findByDescription(String description) {
        for (UserRoleEnum role : UserRoleEnum.values()) {
         if (role.getDescription().equalsIgnoreCase(description)) {
          return role.getCode();
         }
        }
        throw new IllegalArgumentException("Invalid user role code: " + description);
    }
}
