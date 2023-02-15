package com.durgesh.controller;

import com.durgesh.dto.EmployeeDto;
import com.durgesh.entity.Employee;
import com.durgesh.response.CustomResponse;
import com.durgesh.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/employee")
public class EmployeeController extends CustomResponse {
    private static final long serialVersionUID = 1L;

	@Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/employee")
    public String home() {
        //String address = restTemplate.getForObject("http://127.0.0.1:8001/address", String.class);
        return "this is employee  " ;
    }

    @PostMapping("save")
    public ResponseEntity<?> save(@RequestBody EmployeeDto employeeDto) {
        try {
            Employee employee = employeeService.getByEmail(employeeDto.getEmail());
            if (employee == null) {
                Employee save = employeeService.save(employeeDto);
                return response("Record ", HttpStatus.CREATED, employeeDto);
            } else {
                return response("Email Already Exists ", HttpStatus.OK, employeeDto.getEmail());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response("Enter Valid Information ", HttpStatus.INTERNAL_SERVER_ERROR, employeeDto);
    }

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            Employee employee = employeeService.getById(id);
            if (employee != null) {
                return response("Record", HttpStatus.OK, employee);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response("Record Not Found", HttpStatus.NOT_FOUND, id);

    }

    @GetMapping("/all")
    ResponseEntity<?> getAll() {
        try {
            List<Employee> employees = employeeService.getAll();
            if (employees.size() > 0) {
                return response("Found Record  ", HttpStatus.OK, employees);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response("Record Not Found", HttpStatus.NOT_FOUND, "Empty");
    }
}