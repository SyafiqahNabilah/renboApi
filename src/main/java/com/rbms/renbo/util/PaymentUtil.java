package com.rbms.renbo.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentUtil {

    /**
     * Generates a unique payment reference ID starting with "B-"
     * @return payment reference string like "B-12345678-1234-1234-1234-123456789012"
     */
    public String generatePaymentReference() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "B-" + uuid;
    }
}
