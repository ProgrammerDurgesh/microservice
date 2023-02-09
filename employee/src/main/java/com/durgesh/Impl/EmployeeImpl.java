package com.durgesh.Impl;

import com.durgesh.repo.EmployeeRepo;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.durgesh.dto.EmployeeDto;
import com.durgesh.entity.Employee;
import com.durgesh.service.EmployeeService;

import java.util.List;

@Service
public class EmployeeImpl implements EmployeeService {

	@Autowired
	private EmployeeRepo employeeRepo;



	@Autowired
	private ModelMapper mapper;

	public Employee dtoToEmployee(EmployeeDto dto) {
		return mapper.map(dto, Employee.class);
	}

	@Override
	public Employee save(EmployeeDto employeeDto) {
		return employeeRepo.save(dtoToEmployee(employeeDto));
	}

	@Override
	public List<Employee> getAll() {
		return employeeRepo.findAll();
	}

	@Override
	public Employee getById(Long id) {
		return employeeRepo.findById(id).orElse(null);
	}

	@Override
	public Employee removeAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Employee removeById(Integer id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Employee getByEmail(String email) {
		return employeeRepo.findByEmail( email);
	}
}
