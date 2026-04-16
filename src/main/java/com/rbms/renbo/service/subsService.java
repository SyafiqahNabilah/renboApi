/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rbms.renbo.service;

import com.rbms.renbo.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 *
 * @author Syafiqah Nabilah
 */
@Slf4j
@Service
public class subsService {

    //private subspaymentRepo subspaymentRepo;
    private UserRepository ownerRepo;

//    public List<SubsProof> listAllSub() {
//        Iterable<SubsProof> iterable = subspaymentRepo.findAll();
//        List<SubsProof> list = new ArrayList<>();
//        iterable.forEach(list::add);
//        return list;
//    }
//
//    public List<String> getInfoListSubs() {
//        List<String> array = new ArrayList<String>();
//        Iterable<SubsProof> Sproof = subspaymentRepo.findAll();
//        Sproof.forEach((p) -> {
//
//            Owners owner = new Owners();
////            Optional<Owners> ownerList = ownerRepo.findById(p.getOwnerID());
//
//            owner.setOwnerID(0);
////            spsave.set
//        });
//
//
//        return array;
//
//    }
}
