package com.durgesh.service;

import org.springframework.http.ResponseEntity;

public interface PaymentProcessor {
    String createPayload();
    void createRequest();
    void checkStatus();
    ResponseEntity<?> executeWebhook();
    //ArrayList<String> processRefundFile();
    //boolean updateRefund();
    void getTimeoutMsg();
    ResponseEntity<?> createResponseEntity();
    String getOrderId();
    ResponseEntity<?> postRedirectAfterCustVerification();
    //ApiError threeDSVerify();
    void threeDSVerify();

}
