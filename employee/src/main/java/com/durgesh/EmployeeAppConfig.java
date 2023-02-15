package com.durgesh;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EmployeeAppConfig {
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public ModelMapper mapper() {
		return new ModelMapper();
	}

	/*
	 * @Bean public WebClientAutoConfiguration autoConfiguration() { return new
	 * WebClientAutoConfiguration(); }
	 */

}
