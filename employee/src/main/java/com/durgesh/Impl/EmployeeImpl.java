package com.durgesh.Impl;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.durgesh.dto.AddressResponse;
import com.durgesh.dto.EmployeeDto;
import com.durgesh.entity.Employee;
import com.durgesh.repo.EmployeeRepo;
import com.durgesh.service.EmployeeService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmployeeImpl implements EmployeeService {

	@Autowired
	private EmployeeRepo employeeRepo;
	/*
	 * @Autowired private WebClientAutoConfiguration autoConfiguration;
	 * 
	 * @Autowired private RestTemplate restTemplate;
	 * 
	 * @Autowired private DiscoveryClient client;
	 * 
	 */
	@Autowired private RestTemplate restTemplate;
	@Autowired
	private DiscoveryClient client2;

	/*
	 * @Autowired private FeignClient feignClient;
	 */

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
		log.info("DuGu");
		try {
			/*
			 * This is Use with feignClient String s =
			 * feignClient.getAddressByEmployeeId(id);
			 */			

			String url=getAddress(id);
			if (!url.isEmpty()) {
				System.out.println("Mission Done ");
			} else {
				System.out.println("  op's  ");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("hihhihi");
		}

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
		return employeeRepo.findByEmail(email);
	}


	public String getAddress(Long id) {
		List<ServiceInstance> instances = client2.getInstances("ADDRESS-SERVICE");
		System.out.println("Service instance Details :      " + instances);

		System.out.println("Service instance Details :      " + ServiceInstance.class);
		ServiceInstance instance = instances.get(0);
		String url = instance.getUri().toString();
		System.out.println("URI >>>>>>>>>>>>>>>>>>>>       " + url);
		
		Object forObject = restTemplate.getForObject(url+"/address/{id}",null);
		System.out.println("This Is URL For Address Server :              "+forObject);
		return "okay";
	}
}
