/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rbms.renbo.model;

import com.rbms.renbo.constant.UserRoleEnum;
import com.rbms.renbo.entity.PlanPackage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class userRegistrationDto {
    @NotEmpty
    String firstName;
    @NotEmpty
    String lastName;
    @Email
    @NotEmpty
    String email;
    @NotEmpty
    @Size(min = 8)
    String password;
    String phoneNo;
    String address;
    String noAccount;
    @NotEmpty
    UserRoleEnum role;// "OWNER" or "RENTER" — admin created separately
    PlanPackage subsPlan;
}
