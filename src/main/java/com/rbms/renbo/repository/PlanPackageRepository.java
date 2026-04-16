/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rbms.renbo.repository;

import java.util.List;
import java.util.UUID;

import com.rbms.renbo.entity.PlanPackage;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Syafiqah Nabilah
 */
@Repository
public interface PlanPackageRepository extends CrudRepository<PlanPackage, UUID>{
    List<PlanPackage> findAll();

}
