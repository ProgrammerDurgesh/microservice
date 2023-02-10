package com.durgesh.feginClient;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@org.springframework.cloud.openfeign.FeignClient(name = "abc", url = "http://127.0.0.1:8001/")
public interface FeignClient {

    /*
    @Author Durgesh Yadav
    use netflix open-feign web client
    */



    @GetMapping("/address/{id}")
    String getAddressByEmployeeId(@PathVariable Long id);

}
