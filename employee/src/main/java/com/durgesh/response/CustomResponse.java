package com.durgesh.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CustomResponse extends Exception {

	private static final long serialVersionUID = 1L;

	public static ResponseEntity<Object> response(String message, HttpStatus httpStatus, Object object) {
		Map<String, Object> map = new HashMap<>();
		map.put("Message ", message);
		map.put("status", httpStatus);
		map.put("data", object);
		return new ResponseEntity<>(map, httpStatus);
	}
}
