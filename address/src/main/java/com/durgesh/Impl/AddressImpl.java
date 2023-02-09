package com.durgesh.Impl;

import com.durgesh.dto.AddressDto;
import com.durgesh.entity.Address;
import com.durgesh.repo.AddressRepo;
import com.durgesh.service.AddressService;
import org.springframework.stereotype.Service;

@Service
public class AddressImpl implements AddressService {
    @Override
    public Address save(AddressDto addressDto) {
        return null;
    }

    @Override
    public Address getAll() {
        return null;
    }

    @Override
    public Address getById(Integer id) {
        return null;
    }

    @Override
    public Address removeAll() {
        return null;
    }

    @Override
    public Address removeById(Integer id) {
        return null;
    }
}
