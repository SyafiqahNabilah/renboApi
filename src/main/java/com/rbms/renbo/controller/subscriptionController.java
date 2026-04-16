/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.rbms.renbo.controller;

import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Syafiqah Nabilah
 */
@RestController
public class subscriptionController {

//    @Autowired
//    ownerRepository ownerRepository;
//
//    @RequestMapping("/subscribe")
//    public List<SubsProof> subscribe(Model model) {
//        model.addAttribute("subscribe", subserviceImp.listAllSub());
//        return subserviceImp.listAllSub();
//    }
//
//    @GetMapping("/subscribe-list")
//    public List<SubsProof> subscribelist(Model model){
//      return subspaymentRepo.findAll();
//    }
//
//    @RequestMapping("/subscribe-payment/{id}")
//    public String subscribePayment(@PathVariable("id") int id, Model model) {
//         model.addAttribute("subs",  subsRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid item Id:" + id)));
//        return "owner/subscribe-payment";
//    }
//
//    @PostMapping("/requestSubscribe")
//    public String requestSubs(SubsProof subp, BindingResult result, Model model,
//            @RequestParam("paymentProof") MultipartFile multipartFile1) throws IOException{
//        String img1 = StringUtils.cleanPath(multipartFile1.getOriginalFilename());
//        subp.setPaymentProof(img1);
//        subspaymentRepo.save(subp);
//        String uploadDir = "/Users/User/Documents/GitHub/Renbo-MS/src/main/resources/static/img/proof/";
//        FileUploadUtil.saveFile(uploadDir, img1, multipartFile1);
//        return "owner/subscribe";
//    }
    
}
