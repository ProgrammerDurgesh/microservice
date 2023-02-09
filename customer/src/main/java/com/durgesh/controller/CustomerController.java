package com.durgesh.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerController {

    @GetMapping("/Customer")
    public String home()
    {
        return "this is employee";
    }
}
