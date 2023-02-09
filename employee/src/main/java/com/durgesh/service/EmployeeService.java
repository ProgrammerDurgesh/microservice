package com.durgesh.service;

import com.durgesh.dto.EmployeeDto;
import com.durgesh.entity.Employee;

import java.util.List;

public interface EmployeeService {

	Employee save(EmployeeDto employeeDto);

	List<Employee> getAll();

	Employee getById(Long id);

	Employee removeAll();

	Employee removeById(Integer id);

	Employee  getByEmail(String email);

}
