package com.durgesh.service;

import com.durgesh.dto.CustomerDto;
import com.durgesh.entity.Customer;

public interface CustomerService {


    Customer save(CustomerDto customerDto);

    Customer getAll();

    Customer getById(Integer id);

    Customer removeAll();

    Customer removeById(Integer id);


}
