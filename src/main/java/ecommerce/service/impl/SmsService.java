package ecommerce.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class SmsService {

    @Value("${bdbulksms.token}")
    private String token;

    @Value("${bdbulksms.api-url:https://api.bdbulksms.net/api.php}")
    private String apiUrl;

    // RestTemplate is built into Spring Web for making API calls
    private final RestTemplate restTemplate = new RestTemplate();

    public void sendOtpSms(String toPhoneNumber, String otp) {
        try {
            // 1. Prepare the message
            String messageText = "Your BeautyHaat verification code is: " + otp + ". Valid for 5 minutes.";

            // 2. Set Headers for a standard Form submission
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 3. Bind the required parameters mapping perfectly to BDBulkSMS docs
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("token", token);
            map.add("to", toPhoneNumber);
            map.add("message", messageText);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            // 4. Send the POST request to the Gateway
            String response = restTemplate.postForObject(apiUrl, request, String.class);

            // Useful for checking if the SMS actually sent during testing
            System.out.println("BDBulkSMS Response: " + response);

        } catch (Exception e) {
            System.err.println("Failed to send OTP SMS via BDBulkSMS: " + e.getMessage());
        }
    }
}