package com.durgesh.service;

import com.durgesh.dto.AddressDto;
import com.durgesh.entity.Address;

public interface AddressService {


   Address save(AddressDto  addressDto);
   Address getAll();

   Address getById(Integer id);

   Address removeAll();

   Address removeById(Integer id);



}
