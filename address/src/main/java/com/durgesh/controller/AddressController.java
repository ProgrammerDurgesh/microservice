package com.durgesh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.durgesh.entity.Address;
import com.durgesh.repo.AddressRepo;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressRepo addressRepo;

    @GetMapping("/home")
    public String home() {
        return "this is address";
    }

    @GetMapping("/{id}")
    Address getAddressById(@PathVariable Long id) {
		return addressRepo.findById(id).orElse(null);
    }

}
